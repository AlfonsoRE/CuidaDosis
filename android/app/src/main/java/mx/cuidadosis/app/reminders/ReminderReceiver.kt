package mx.cuidadosis.app.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import mx.cuidadosis.app.CuidaDosisApp
import java.time.LocalDateTime
import java.time.ZonedDateTime

// Suena la alarma: muestra el aviso de esa toma y programa el siguiente
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION) return
        val scheduler = (context.applicationContext as CuidaDosisApp).container.reminderScheduler
        val intakeAt = intent.getStringExtra(EXTRA_INTAKE_AT)
            ?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() }

        if (intakeAt != null) {
            // Si la alarma llegó muy tarde (teléfono apagado, por ejemplo) ya no se avisa esa toma
            val now = ZonedDateTime.now(REMINDER_ZONE).toLocalDateTime()
            if (!now.isAfter(intakeAt.plusMinutes(LATE_LIMIT_MINUTES))) {
                ReminderNotifier.show(context, intakeAt, scheduler.medsFor(intakeAt))
            }
            scheduler.markNotified(intakeAt)
        }
        scheduler.reschedule()
    }

    companion object {
        const val ACTION = "mx.cuidadosis.app.action.INTAKE_REMINDER"
        const val EXTRA_INTAKE_AT = "intake_at"
        private const val LATE_LIMIT_MINUTES = 10L
    }
}
