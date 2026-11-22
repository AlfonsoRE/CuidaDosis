package mx.cuidadosis.app.reminders

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import mx.cuidadosis.app.CuidaDosisApp
import mx.cuidadosis.app.MainActivity
import mx.cuidadosis.app.R
import mx.cuidadosis.app.data.IntakeRepository
import mx.cuidadosis.app.data.local.LocalCipher
import mx.cuidadosis.app.data.remote.ApiJson
import mx.cuidadosis.app.data.remote.ApiResult
import mx.cuidadosis.app.ui.common.formatApiDateTime
import java.time.Instant

@Serializable
data class CaregiverAlarmData(
    val userId: Int,
    val watches: List<CaregiverWatch> = emptyList(),
    val fired: Set<String> = emptySet(),
    // Última sincronización conocida de cada paciente (para el aviso sin conexión)
    val lastSync: Map<Int, String> = emptyMap(),
)

/**
 * Alarma local del cuidador a T + 30 (RF-18, ADR-007). Las tomas a vigilar se obtienen de
 * GET /caregiver/patients/{id}/intakes/today cuando el cuidador abre la app; al sonar se vuelve a
 * consultar y solo se avisa si la toma sigue sin confirmar o si no se pudo consultar.
 * La lista se guarda cifrada (lleva nombres) y excluida de respaldos.
 */
class CaregiverAlarmScheduler(private val context: Context) {
    private val prefs = context.getSharedPreferences("caregiver_alarms", Context.MODE_PRIVATE)
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun load(): CaregiverAlarmData? = prefs.getString(KEY, null)?.let {
        runCatching { ApiJson.decodeFromString(CaregiverAlarmData.serializer(), LocalCipher.decrypt(it)) }.getOrNull()
    }

    @Synchronized
    private fun save(data: CaregiverAlarmData) =
        prefs.edit { putString(KEY, LocalCipher.encrypt(ApiJson.encodeToString(CaregiverAlarmData.serializer(), data))) }

    /** Descarga las tomas de hoy de todos los pacientes vinculados y reprograma. */
    suspend fun refresh(userId: Int, intakes: IntakeRepository) {
        val patients = intakes.caregiverPatients() as? ApiResult.Success ?: return
        val now = Instant.now()
        val watches = mutableListOf<CaregiverWatch>()
        val lastSync = mutableMapOf<Int, String>()
        for (p in patients.data.patients) {
            p.lastSyncAt?.let { lastSync[p.id] = it }
            val today = intakes.caregiverPatientToday(p.id) as? ApiResult.Success ?: continue
            watches += watchesFrom(p.id, p.fullName, today.data, now)
        }
        val previous = load()?.takeIf { it.userId == userId }
        save(CaregiverAlarmData(userId, watches, previous?.fired.orEmpty().filter { k -> watches.any { it.key == k } }.toSet(), lastSync))
        reschedule()
    }

    @Synchronized
    fun reschedule() {
        val data = load()
        val next = data?.let { nextCaregiverWatch(it.watches, Instant.now(), it.fired) }
        if (next == null) {
            alarmManager.cancel(pending(null))
            return
        }
        val at = maxOf(watchTriggerAt(next)!!, Instant.now()).toEpochMilli()
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        if (canExact) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending(next))
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending(next))
        }
        Log.i("CuidaDosis", "Alarma del cuidador: toma ${next.scheduledFor} del paciente ${next.patientId}")
    }

    @Synchronized
    fun cancelAll() {
        alarmManager.cancel(pending(null))
        prefs.edit { clear() }
    }

    /** Al sonar: consulta el estado y decide si avisar. */
    suspend fun onFire(key: String, intakes: IntakeRepository) {
        val data = load() ?: return
        val watch = data.watches.firstOrNull { it.key == key }
        if (watch != null) {
            val result = withTimeoutOrNull(15_000) { intakes.caregiverPatientToday(watch.patientId) }
            val today = (result as? ApiResult.Success)?.data
            val code = (result as? ApiResult.Error)?.code
            val lastSync = today?.patient?.lastSyncAt ?: data.lastSync[watch.patientId]
            when (caregiverAlertFor(watch, code, today)) {
                CaregiverAlert.UNCONFIRMED -> notify(
                    watch,
                    "Toma sin confirmar: ${watch.patientName}",
                    "${watch.medicationName} de las ${watch.scheduledFor.substring(11, 16)} sigue sin confirmar. " +
                        lastSyncLine(lastSync),
                )
                CaregiverAlert.CHECK_MANUALLY -> notify(
                    watch,
                    "Revisa a ${watch.patientName}",
                    "No se pudo consultar la toma de las ${watch.scheduledFor.substring(11, 16)} (sin conexión). " +
                        lastSyncLine(lastSync),
                )
                CaregiverAlert.LINK_REVOKED -> {
                    save(data.copy(watches = data.watches.filter { it.patientId != watch.patientId }))
                    reschedule()
                    return
                }
                CaregiverAlert.NONE -> Unit
            }
        }
        val fresh = load() ?: return
        save(fresh.copy(fired = fresh.fired + key))
        reschedule()
    }

    private fun lastSyncLine(lastSync: String?): String =
        lastSync?.let { "Última sincronización del paciente: ${formatApiDateTime(it)}." }
            ?: "El teléfono del paciente aún no ha sincronizado."

    private fun notify(watch: CaregiverWatch, title: String, text: String) {
        val allowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (!allowed) return
        val open = PendingIntent.getActivity(
            context,
            2,
            Intent(context, MainActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(10_000 + (watch.intakeId % 50_000), n)
        } catch (_: SecurityException) {
        }
    }

    private fun pending(watch: CaregiverWatch?): PendingIntent {
        val intent = Intent(context, CaregiverAlarmReceiver::class.java).setAction(CaregiverAlarmReceiver.ACTION)
        watch?.let { intent.putExtra(CaregiverAlarmReceiver.EXTRA_KEY, it.key) }
        return PendingIntent.getBroadcast(context, 3, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    companion object {
        // v1.1: canal nuevo con vibración (un canal creado no cambia sus ajustes)
        const val CHANNEL_ID = "caregiver_alerts_v2"
        private const val LEGACY_CHANNEL = "caregiver_alerts"
        private const val KEY = "data"

        fun createChannel(context: Context) {
            // Notificación normal (sonido y vibración breves, sin repetir), como el aviso previo del paciente
            val channel = NotificationChannel(CHANNEL_ID, "Tomas sin confirmar", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Aviso al cuidador 30 minutos después de una toma sin confirmar"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 400, 200, 400)
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
            manager.deleteNotificationChannel(LEGACY_CHANNEL)
        }
    }
}

// Suena la alarma del cuidador: consulta la API en segundo plano (goAsync) y decide si avisar
class CaregiverAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION) return
        val key = intent.getStringExtra(EXTRA_KEY) ?: return
        val container = (context.applicationContext as CuidaDosisApp).container
        val session = container.sessionStore.current
        if (session == null || session.isPatient) {
            container.caregiverAlarms.cancelAll()
            return
        }
        val done = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                container.caregiverAlarms.onFire(key, container.intakeRepository)
            } finally {
                done.finish()
            }
        }
    }

    companion object {
        const val ACTION = "mx.cuidadosis.app.action.CAREGIVER_CHECK"
        const val EXTRA_KEY = "key"
    }
}
