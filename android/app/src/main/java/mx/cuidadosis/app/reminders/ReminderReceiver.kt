package mx.cuidadosis.app.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import mx.cuidadosis.app.CuidaDosisApp
import java.time.LocalDateTime

// Suena la alarma: muestra el aviso si la toma sigue pendiente y programa el siguiente
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION) return
        val scheduler = (context.applicationContext as CuidaDosisApp).container.reminderScheduler
        val intakeAt = intent.getStringExtra(EXTRA_INTAKE_AT)
            ?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() }
        val kind = intent.getStringExtra(EXTRA_KIND)
            ?.let { runCatching { ReminderKind.valueOf(it) }.getOrNull() }
        val key = intent.getStringExtra(EXTRA_KEY)

        if (intakeAt != null && kind != null && key != null) {
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

// Botón "Posponer" de la notificación (RF-16)
class SnoozeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION) return
        val intakeAt = intent.getStringExtra(EXTRA_INTAKE_AT)
            ?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() } ?: return
        val minutes = intent.getIntExtra(EXTRA_MINUTES, 0).takeIf { it in SNOOZE_OPTIONS } ?: return
        (context.applicationContext as CuidaDosisApp).container.reminderScheduler.snooze(intakeAt, minutes)
    }

    companion object {
        const val ACTION = "mx.cuidadosis.app.action.SNOOZE"
        const val EXTRA_INTAKE_AT = "intake_at"
        const val EXTRA_MINUTES = "minutes"
    }
}
