package mx.cuidadosis.app.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import mx.cuidadosis.app.data.local.PatientStore
import mx.cuidadosis.app.data.local.SettingsStore
import mx.cuidadosis.app.data.nowMexico
import java.time.Instant
import java.time.LocalDateTime

/**
 * Avisos locales del paciente (ADR-002). Desde la v1.1 hay una alarma de reloj por cada aviso
 * pendiente (T - L, T y pospuestos), calculados con la copia local, así que funciona sin red.
 * Cuando una suena, ReminderReceiver muestra el aviso y llama de nuevo a reschedule().
 */
class ReminderScheduler(
    private val context: Context,
    private val patientStore: PatientStore,
    private val settings: SettingsStore,
) {
    private val store = ReminderStore(context)
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    /**
     * Programa una alarma por cada aviso pendiente (T - L, T y pospuestos) con la copia local, sin red.
     * v1.1: cada aviso tiene su propia alarma (`setAlarmClock`), así que un retraso del sistema en
     * una no arrastra a las demás. Las alarmas que ya no corresponden se cancelan.
     */
    @Synchronized
    fun reschedule() {
        val data = patientStore.current
        val events = data?.let {
            upcomingReminderEvents(
                meds = it.reminderMeds(),
                now = Instant.now(),
                leadMinutes = settings.reminderLead.value,
                fired = store.fired(),
                confirmed = it.confirmedSet(),
                snoozes = store.snoozes(),
            )
        }.orEmpty()
        val codes = events.map { alarmRequestCode(it) }.toSet()
        // Cancela las alarmas programadas antes que ya no están en la lista (y la única de la v1.0)
        (store.scheduledCodes() - codes).forEach { alarmManager.cancel(alarmIntent(it, null)) }
        alarmManager.cancel(alarmIntent(LEGACY_CODE, null))
        events.forEach { schedule(it) }
        store.setScheduledCodes(codes)
        events.firstOrNull()?.let {
            Log.i(TAG, "Avisos programados: ${events.size}; próximo ${it.kind}: toma de ${it.intakeAt} (hora de México), disparo ${it.triggerAt}")
        } ?: Log.i(TAG, "Avisos programados: 0")
    }

    private fun schedule(event: ReminderEvent) {
        val trigger = event.triggerAt.toEpochMilli()
        val intent = alarmIntent(alarmRequestCode(event), event)
        if (canScheduleExact()) {
            // Alarma de reloj: el sistema no la difiere por ahorro de energía ni por Doze
            alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(trigger, openAppIntent()), intent)
        } else {
            // Respaldo si el sistema quitara el permiso de alarma exacta: puede llegar con retraso
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, intent)
        }
    }

    // Lo que muestra el sistema al tocar el ícono de alarma: abre la app
    private fun openAppIntent(): PendingIntent = PendingIntent.getActivity(
        context,
        5,
        Intent(context, mx.cuidadosis.app.MainActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    // Medicamentos de una toma que siguen sin confirmar, según la copia local actual
    fun pendingMedsFor(intakeAt: LocalDateTime): List<ReminderMed> {
        val data = patientStore.current ?: return emptyList()
        return pendingMedsAt(data.reminderMeds(), intakeAt, data.confirmedSet())
    }

    @Synchronized
    fun markFired(key: String) = store.addFired(key, nowMexico())

    /**
     * Pospone el aviso de una toma [minutes] minutos (RF-16). No marca la toma.
     * Si el pospuesto cae a la hora de la toma o después, sustituye al segundo aviso (T).
     */
    @Synchronized
    fun snooze(intakeAt: LocalDateTime, minutes: Int) {
        val now = Instant.now()
        val at = now.plusSeconds(minutes * 60L)
        store.addSnooze(Snooze(intakeAt.toString(), at.epochSecond), now)
        if (!at.isBefore(intakeAt.atZone(REMINDER_ZONE).toInstant())) {
            store.addFired(eventKey(intakeAt, ReminderKind.DUE), nowMexico())
        }
        // Detiene el sonido de la alarma de esa toma; vuelve a sonar al terminar el plazo
        ReminderNotifier.stopAlarm(context, intakeAt)
        reschedule()
    }

    // Una toma confirmada detiene su alarma (si sonaba) y quita la notificación
    fun onConfirmed(intakeAt: LocalDateTime) {
        if (pendingMedsFor(intakeAt).isEmpty()) ReminderNotifier.stopAlarm(context, intakeAt)
        reschedule()
    }

    fun firedKeys(): Set<String> = store.fired()

    fun cachedUserId(): Int? = patientStore.current?.userId

    // Al cerrar sesión, expirar el token o entrar como cuidador: sin alarmas ni registros
    @Synchronized
    fun cancelAll() {
        store.scheduledCodes().forEach { alarmManager.cancel(alarmIntent(it, null)) }
        alarmManager.cancel(alarmIntent(LEGACY_CODE, null))
        store.clear()
        NotificationManagerCompat.from(context).cancelAll()
    }

    fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    // Un PendingIntent por aviso, identificado por su código (alarmRequestCode)
    private fun alarmIntent(code: Int, event: ReminderEvent?): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).setAction(ReminderReceiver.ACTION)
        event?.let {
            intent.putExtra(ReminderReceiver.EXTRA_INTAKE_AT, it.intakeAt.toString())
            intent.putExtra(ReminderReceiver.EXTRA_KIND, it.kind.name)
            intent.putExtra(ReminderReceiver.EXTRA_KEY, it.key)
        }
        return PendingIntent.getBroadcast(
            context,
            code,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private companion object {
        const val TAG = "CuidaDosis"
        // Código de la única alarma que usaba la v1.0; se cancela al actualizar
        const val LEGACY_CODE = 0
    }
}
