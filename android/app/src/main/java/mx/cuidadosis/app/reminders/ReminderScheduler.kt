package mx.cuidadosis.app.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import mx.cuidadosis.app.data.remote.MedicationDto
import java.time.Instant
import java.time.LocalDateTime

/**
 * Programa los avisos locales. Solo hay una alarma pendiente a la vez: la del próximo aviso.
 * Cuando suena, ReminderReceiver muestra el aviso y llama de nuevo a reschedule().
 */
class ReminderScheduler(private val context: Context) {
    private val store = ReminderStore(context)
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    // Guarda los medicamentos activos del paciente y reprograma
    @Synchronized
    fun sync(userId: Int, medications: List<MedicationDto>) {
        store.save(ReminderData(userId, medications.map { ReminderMed(it.id, it.name, it.dose, it.times) }))
        reschedule()
    }

    // Programa el próximo aviso con la copia local (no usa la red)
    @Synchronized
    fun reschedule() {
        val data = store.load()
        val next = data?.let { nextReminder(it.meds, Instant.now(), store.notified()) }
        if (next == null) {
            alarmManager.cancel(alarmIntent(null))
            return
        }
        val trigger = next.triggerAt.toEpochMilli()
        val intent = alarmIntent(next.intakeAt)
        if (canScheduleExact()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, intent)
        } else {
            // Respaldo si el sistema quitara el permiso de alarma exacta: puede llegar con retraso
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, intent)
        }
        Log.i(TAG, "Próximo aviso: toma de ${next.intakeAt} (hora de México), disparo ${next.triggerAt}")
    }

    // Medicamentos de una toma, según la copia local actual
    fun medsFor(intakeAt: LocalDateTime): List<ReminderMed> {
        val time = "%02d:%02d".format(intakeAt.hour, intakeAt.minute)
        return store.load()?.meds.orEmpty()
            .filter { med -> med.times.any { it.startsWith(time) } }
            .sortedBy { it.name.lowercase() }
    }

    @Synchronized
    fun markNotified(intakeAt: LocalDateTime) {
        store.addNotified(intakeAt, LocalDateTime.now(REMINDER_ZONE))
    }

    fun cachedUserId(): Int? = store.load()?.userId

    // Al cerrar sesión, expirar el token o entrar como cuidador: sin alarmas ni copia local
    @Synchronized
    fun cancelAll() {
        alarmManager.cancel(alarmIntent(null))
        store.clear()
        NotificationManagerCompat.from(context).cancelAll()
    }

    private fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    // Siempre el mismo PendingIntent (código 0): programar uno nuevo reemplaza al anterior
    private fun alarmIntent(intakeAt: LocalDateTime?): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).setAction(ReminderReceiver.ACTION)
        intakeAt?.let { intent.putExtra(ReminderReceiver.EXTRA_INTAKE_AT, it.toString()) }
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private companion object {
        const val TAG = "CuidaDosis"
    }
}
