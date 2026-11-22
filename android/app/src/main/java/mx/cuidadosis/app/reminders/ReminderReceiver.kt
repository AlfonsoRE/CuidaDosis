package mx.cuidadosis.app.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import mx.cuidadosis.app.CuidaDosisApp
import mx.cuidadosis.app.data.formatApiDateTimeValue
import java.time.LocalDateTime

// Suena la alarma de un evento: muestra el aviso si la toma sigue pendiente
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION) return
        val scheduler = (context.applicationContext as CuidaDosisApp).container.reminderScheduler
        val intakeAt = intent.getStringExtra(EXTRA_INTAKE_AT)
            ?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() }
        val kind = intent.getStringExtra(EXTRA_KIND)
            ?.let { runCatching { ReminderKind.valueOf(it) }.getOrNull() }
        val key = intent.getStringExtra(EXTRA_KEY)

        if (intakeAt != null && kind != null && key != null && key !in scheduler.firedKeys()) {
            // Se revisa al sonar: la toma pudo confirmarse después de programar la alarma
            val pending = scheduler.pendingMedsFor(intakeAt)
            if (pending.isNotEmpty()) ReminderNotifier.show(context, intakeAt, kind, pending)
            scheduler.markFired(key)
        }
        scheduler.reschedule()
    }

    companion object {
        const val ACTION = "mx.cuidadosis.app.action.INTAKE_REMINDER"
        const val EXTRA_INTAKE_AT = "intake_at"
        const val EXTRA_KIND = "kind"
        const val EXTRA_KEY = "key"
    }
}

/**
 * Acciones de la alarma del paciente (RF-06, RF-07, RF-16), compartidas por la notificación y por la
 * pantalla de alarma. Las dos detienen el sonido y quitan el corte de 2 minutos.
 */
object AlarmActions {
    // "Tomado": confirma todos los medicamentos pendientes de esa hora (cola local; se envía si hay red)
    fun take(context: Context, intakeAt: LocalDateTime, onDone: () -> Unit = {}) {
        val container = (context.applicationContext as CuidaDosisApp).container
        val scheduler = container.reminderScheduler
        val time = intakeAt.toLocalTime()
        scheduler.pendingMedsFor(intakeAt).forEach { med ->
            med.scheduleIdAt(time)?.let { container.intakeRepository.confirm(it, formatApiDateTimeValue(intakeAt)) }
        }
        ReminderNotifier.stopAlarm(context, intakeAt)
        scheduler.reschedule()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                withTimeoutOrNull(15_000) { container.intakeRepository.syncQueue() }
            } finally {
                onDone()
            }
        }
    }

    fun snooze(context: Context, intakeAt: LocalDateTime, minutes: Int) {
        val scheduler = (context.applicationContext as CuidaDosisApp).container.reminderScheduler
        scheduler.snooze(intakeAt, minutes)
    }
}

// Botón "Tomado" de la notificación de alarma
class TakeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION) return
        val intakeAt = intent.getStringExtra(EXTRA_INTAKE_AT)
            ?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() } ?: return
        val done = goAsync()
        AlarmActions.take(context, intakeAt) { done.finish() }
    }

    companion object {
        const val ACTION = "mx.cuidadosis.app.action.TAKE"
        const val EXTRA_INTAKE_AT = "intake_at"
    }
}

// Botones "Posponer" de la notificación (RF-16): detienen la alarma y vuelve a sonar al plazo
class SnoozeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION) return
        val intakeAt = intent.getStringExtra(EXTRA_INTAKE_AT)
            ?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() } ?: return
        val minutes = intent.getIntExtra(EXTRA_MINUTES, 0).takeIf { it in SNOOZE_OPTIONS } ?: return
        AlarmActions.snooze(context, intakeAt, minutes)
    }

    companion object {
        const val ACTION = "mx.cuidadosis.app.action.SNOOZE"
        const val EXTRA_INTAKE_AT = "intake_at"
        const val EXTRA_MINUTES = "minutes"
    }
}

// A los 2 minutos de sonar: si la toma sigue pendiente, queda la notificación persistente sin sonido
class RingLimitReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION) return
        val intakeAt = intent.getStringExtra(EXTRA_INTAKE_AT)
            ?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() } ?: return
        val kind = intent.getStringExtra(EXTRA_KIND)
            ?.let { runCatching { ReminderKind.valueOf(it) }.getOrNull() } ?: ReminderKind.DUE
        val scheduler = (context.applicationContext as CuidaDosisApp).container.reminderScheduler
        val pending = scheduler.pendingMedsFor(intakeAt)
        if (shouldKeepSilentReminder(pending.size)) {
            ReminderNotifier.showAlarm(context, intakeAt, kind, pending, ringing = false)
        } else {
            ReminderNotifier.cancel(context, intakeAt)
        }
    }

    companion object {
        const val ACTION = "mx.cuidadosis.app.action.RING_LIMIT"
        const val EXTRA_INTAKE_AT = "intake_at"
        const val EXTRA_KIND = "kind"
    }
}
