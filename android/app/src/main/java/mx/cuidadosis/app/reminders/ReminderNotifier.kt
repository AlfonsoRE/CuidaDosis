package mx.cuidadosis.app.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import mx.cuidadosis.app.MainActivity
import mx.cuidadosis.app.R
import java.time.LocalDateTime

// Notificación del aviso. Muestra nombre y dosis tal como los escribió el usuario, sin interpretarlos.
object ReminderNotifier {
    const val CHANNEL_ID = "intake_reminders"

    fun createChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL_ID, "Avisos de tomas", NotificationManager.IMPORTANCE_HIGH)
            .apply { description = "Aviso 5 minutos antes de cada toma registrada" }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun canNotify(context: Context): Boolean {
        val permission = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        return permission && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun show(context: Context, intakeAt: LocalDateTime, meds: List<ReminderMed>) {
        if (meds.isEmpty() || !canNotify(context)) return
        val time = "%02d:%02d".format(intakeAt.hour, intakeAt.minute)
        val lines = meds.map { "${it.name}: ${it.dose}" }
        val title = "Toma de las $time"
        val summary = "En 5 minutos"

        // Tocar el aviso abre la app; la pestaña inicial del paciente es "Hoy"
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val style = NotificationCompat.InboxStyle().setSummaryText(summary)
        lines.forEach { style.addLine(it) }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(lines.joinToString(", "))
            .setStyle(style)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            // Un id por hora del día: avisos de horas distintas no se reemplazan entre sí
            NotificationManagerCompat.from(context).notify(intakeAt.hour * 60 + intakeAt.minute, notification)
        } catch (_: SecurityException) {
            // El usuario quitó el permiso justo ahora: no hay nada más que hacer
        }
    }
}
