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
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime

// Notificación del aviso. Muestra nombre y dosis tal como los escribió el usuario, sin interpretarlos.
object ReminderNotifier {
    const val CHANNEL_ID = "intake_reminders"

    fun createChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL_ID, "Avisos de tomas", NotificationManager.IMPORTANCE_HIGH)
            .apply { description = "Aviso antes de cada toma registrada y a la hora de la toma" }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun canNotify(context: Context): Boolean {
        val permission = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        return permission && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun show(context: Context, intakeAt: LocalDateTime, kind: ReminderKind, meds: List<ReminderMed>) {
        if (meds.isEmpty() || !canNotify(context)) return
        val time = "%02d:%02d".format(intakeAt.hour, intakeAt.minute)
        val lines = meds.map { "${it.name}: ${it.dose}" }
        val title = when (kind) {
            ReminderKind.BEFORE -> "Toma de las $time"
            ReminderKind.DUE -> "Es la hora de tu toma de las $time"
            ReminderKind.SNOOZE -> "Recordatorio: toma de las $time"
        }
        val summary = when (kind) {
            ReminderKind.BEFORE -> {
                val minutes = Duration.between(Instant.now(), intakeAt.atZone(REMINDER_ZONE).toInstant())
                    .toMinutes().coerceAtLeast(1)
                if (minutes == 1L) "En 1 minuto" else "En $minutes minutos"
            }
            ReminderKind.DUE -> "Sigue pendiente"
            ReminderKind.SNOOZE -> "Aviso pospuesto"
        }

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

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText("$summary: ${lines.joinToString(", ")}")
            .setStyle(style)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(open)
            .setAutoCancel(true)
        // Posponer no marca la toma (RF-16)
        SNOOZE_OPTIONS.forEach { minutes ->
            builder.addAction(0, "Posponer $minutes min", snoozeIntent(context, intakeAt, minutes))
        }
        try {
            NotificationManagerCompat.from(context).notify(notificationId(intakeAt), builder.build())
        } catch (_: SecurityException) {
            // El usuario quitó el permiso justo ahora: no hay nada más que hacer
        }
    }

    fun cancel(context: Context, intakeAt: LocalDateTime) =
        NotificationManagerCompat.from(context).cancel(notificationId(intakeAt))

    // Un id por hora del día: avisos de horas distintas no se reemplazan entre sí
    private fun notificationId(intakeAt: LocalDateTime) = intakeAt.hour * 60 + intakeAt.minute

    private fun snoozeIntent(context: Context, intakeAt: LocalDateTime, minutes: Int): PendingIntent {
        val intent = Intent(context, SnoozeReceiver::class.java)
            .setAction(SnoozeReceiver.ACTION)
            .putExtra(SnoozeReceiver.EXTRA_INTAKE_AT, intakeAt.toString())
            .putExtra(SnoozeReceiver.EXTRA_MINUTES, minutes)
        // Código distinto por toma y por opción, para que cada botón lleve sus datos
        return PendingIntent.getBroadcast(
            context,
            1 + notificationId(intakeAt) * 100 + minutes,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
