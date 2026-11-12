package mx.cuidadosis.app.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import mx.cuidadosis.app.CuidaDosisApp

/**
 * Android borra las alarmas al reiniciar o al actualizar la app, y un cambio de hora o de zona
 * puede dejarlas mal. En esos casos se reprograma con la copia local, sin red.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in ACTIONS) return
        val container = (context.applicationContext as CuidaDosisApp).container
        val session = container.sessionStore.current
        if (session != null && session.isPatient && container.reminderScheduler.cachedUserId() == session.userId) {
            container.reminderScheduler.reschedule()
        } else {
            container.reminderScheduler.cancelAll()
        }
    }

    private companion object {
        val ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
        )
    }
}
