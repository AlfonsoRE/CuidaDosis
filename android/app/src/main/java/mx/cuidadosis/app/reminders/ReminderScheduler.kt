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
 * Avisos locales del paciente (ADR-002). Solo hay una alarma pendiente a la vez: la del próximo
 * evento (T - L, T o pospuesto), calculado con la copia local, así que funciona sin red.
 * Cuando suena, ReminderReceiver muestra el aviso y llama de nuevo a reschedule().
 */
class ReminderScheduler(
    private val context: Context,
    private val patientStore: PatientStore,
    private val settings: SettingsStore,
) {
    private val store = ReminderStore(context)
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    // Programa el próximo aviso con la copia local (no usa la red)
    @Synchronized
    fun reschedule() {
        val data = patientStore.current
        val next = data?.let {
            nextReminderEvent(
                meds = it.reminderMeds(),
                now = Instant.now(),
                leadMinutes = settings.reminderLead.value,
                fired = store.fired(),
                confirmed = it.confirmedSet(),
                snoozes = store.snoozes(),
            )
        }
        if (next == null) {
            alarmManager.cancel(alarmIntent(null))
            return
        }
        val trigger = next.triggerAt.toEpochMilli()
        val intent = alarmIntent(next)
        if (canScheduleExact()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, intent)
        } else {
            // Respaldo si el sistema quitara el permiso de alarma exacta: puede llegar con retraso
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, intent)
        }
        Log.i(TAG, "Próximo aviso ${next.kind}: toma de ${next.intakeAt} (hora de México), disparo ${next.triggerAt}")
    }

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
        alarmManager.cancel(alarmIntent(null))
        store.clear()
        NotificationManagerCompat.from(context).cancelAll()
    }

    fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    // Siempre el mismo PendingIntent (código 0): programar uno nuevo reemplaza al anterior
    private fun alarmIntent(event: ReminderEvent?): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).setAction(ReminderReceiver.ACTION)
        event?.let {
            intent.putExtra(ReminderReceiver.EXTRA_INTAKE_AT, it.intakeAt.toString())
            intent.putExtra(ReminderReceiver.EXTRA_KIND, it.kind.name)
            intent.putExtra(ReminderReceiver.EXTRA_KEY, it.key)
        }
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
