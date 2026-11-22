package mx.cuidadosis.app.reminders

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import mx.cuidadosis.app.MainActivity
import mx.cuidadosis.app.R
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime

/**
 * Notificaciones del paciente (v1.1). Muestra nombre y dosis tal como los escribió el usuario.
 * - Aviso previo (T - L, RF-05): notificación normal con sonido y vibración.
 * - Alarma a la hora T y al terminar un pospuesto (RF-06): sonido de alarma y vibración que se
 *   repiten (FLAG_INSISTENT) hasta que el paciente toque "Tomado" o "Posponer", con un límite de
 *   2 minutos; después queda una notificación persistente sin sonido.
 */
object ReminderNotifier {
    // Canales v1.1 (IDs nuevos: un canal ya creado en un teléfono no cambia su sonido ni vibración)
    const val CHANNEL_BEFORE = "intake_before_v2"
    const val CHANNEL_ALARM = "intake_alarm_v2"
    const val CHANNEL_ALARM_SILENT = "intake_alarm_silent_v2"

    // Canal de la versión 1.0 (sin vibración), se retira al iniciar la app
    private const val LEGACY_CHANNEL = "intake_reminders"

    private val BEFORE_VIBRATION = longArrayOf(0, 400, 200, 400)
    private val ALARM_VIBRATION = longArrayOf(0, 800, 400, 800, 400, 800)

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val before = NotificationChannel(CHANNEL_BEFORE, "Aviso antes de la toma", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Aviso 5 o 10 minutos antes de cada toma, con sonido y vibración"
            enableVibration(true)
            vibrationPattern = BEFORE_VIBRATION
            setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
        }
        val alarm = NotificationChannel(CHANNEL_ALARM, "Alarma de la toma", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Alarma a la hora de la toma: suena y vibra hasta que toques Tomado o Posponer (máximo 2 minutos)"
            enableVibration(true)
            vibrationPattern = ALARM_VIBRATION
            // Sonido de alarma por el flujo de alarma: no lo silencia el modo vibración
            setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
        }
        val silent = NotificationChannel(
            CHANNEL_ALARM_SILENT,
            "Toma pendiente (sin sonido)",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Queda visible cuando la alarma dejó de sonar y la toma sigue pendiente"
            setSound(null, null)
            enableVibration(false)
        }
        manager.createNotificationChannels(listOf(before, alarm, silent))
        manager.deleteNotificationChannel(LEGACY_CHANNEL)
    }

    fun canNotify(context: Context): Boolean {
        val permission = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        return permission && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun show(context: Context, intakeAt: LocalDateTime, kind: ReminderKind, meds: List<ReminderMed>) {
        if (meds.isEmpty() || !canNotify(context)) return
        when (kind) {
            ReminderKind.BEFORE -> showBefore(context, intakeAt, meds)
            ReminderKind.DUE, ReminderKind.SNOOZE -> showAlarm(context, intakeAt, kind, meds, ringing = true)
        }
    }

    private fun showBefore(context: Context, intakeAt: LocalDateTime, meds: List<ReminderMed>) {
        val seconds = Duration.between(Instant.now(), intakeAt.atZone(REMINDER_ZONE).toInstant()).seconds
        val minutes = ((seconds + 30) / 60).coerceAtLeast(1)
        val summary = if (minutes == 1L) "En 1 minuto" else "En $minutes minutos"
        val lines = meds.map { "${it.name}: ${it.dose}" }
        val style = NotificationCompat.InboxStyle().setSummaryText(summary)
        lines.forEach { style.addLine(it) }
        val builder = NotificationCompat.Builder(context, CHANNEL_BEFORE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Toma de las ${hhmm(intakeAt)}")
            .setContentText("$summary: ${lines.joinToString(", ")}")
            .setStyle(style)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVibrate(BEFORE_VIBRATION)
            .setContentIntent(openApp(context))
            .setAutoCancel(true)
        SNOOZE_OPTIONS.forEach { builder.addAction(0, "Posponer $it min", snoozeIntent(context, intakeAt, it)) }
        notify(context, intakeAt, builder.build())
    }

    /**
     * Alarma de la toma. Con [ringing] suena y vibra en bucle (FLAG_INSISTENT) y se programa el
     * corte a los 2 minutos; sin [ringing] es la versión persistente y silenciosa.
     */
    fun showAlarm(context: Context, intakeAt: LocalDateTime, kind: ReminderKind, meds: List<ReminderMed>, ringing: Boolean) {
        if (meds.isEmpty() || !canNotify(context)) return
        val lines = meds.map { "${it.name}: ${it.dose}" }
        val title = when (kind) {
            ReminderKind.SNOOZE -> "Recordatorio: toma de las ${hhmm(intakeAt)}"
            else -> "Es la hora de tu toma de las ${hhmm(intakeAt)}"
        }
        val summary = if (ringing) "Toca Tomado o Posponer" else "Sigue pendiente"
        val style = NotificationCompat.InboxStyle().setSummaryText(summary)
        lines.forEach { style.addLine(it) }
        val builder = NotificationCompat.Builder(context, if (ringing) CHANNEL_ALARM else CHANNEL_ALARM_SILENT)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText("$summary: ${lines.joinToString(", ")}")
            .setStyle(style)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(if (ringing) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_DEFAULT)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(alarmScreen(context, intakeAt, kind))
            .addAction(0, "Tomado", takeIntent(context, intakeAt))
        SNOOZE_OPTIONS.forEach { builder.addAction(0, "Posponer $it min", snoozeIntent(context, intakeAt, it)) }
        if (ringing) {
            builder.setVibrate(ALARM_VIBRATION)
            builder.setFullScreenIntent(alarmScreen(context, intakeAt, kind), true)
        } else {
            builder.setSilent(true).setOnlyAlertOnce(true)
        }
        val notification = builder.build()
        if (ringing) notification.flags = notification.flags or Notification.FLAG_INSISTENT
        notify(context, intakeAt, notification)
        if (ringing) scheduleRingLimit(context, intakeAt, kind)
    }

    // Detiene el sonido: quita la notificación de esa toma y el corte de 2 minutos pendiente
    fun stopAlarm(context: Context, intakeAt: LocalDateTime) {
        cancel(context, intakeAt)
        context.getSystemService(AlarmManager::class.java).cancel(ringLimitIntent(context, intakeAt, ReminderKind.DUE))
    }

    fun cancel(context: Context, intakeAt: LocalDateTime) =
        NotificationManagerCompat.from(context).cancel(notificationId(intakeAt))

    private fun notify(context: Context, intakeAt: LocalDateTime, notification: Notification) {
        try {
            NotificationManagerCompat.from(context).notify(notificationId(intakeAt), notification)
        } catch (_: SecurityException) {
            // El usuario quitó el permiso justo ahora: no hay nada más que hacer
        }
    }

    private fun scheduleRingLimit(context: Context, intakeAt: LocalDateTime, kind: ReminderKind) {
        val at = alarmRingUntil(Instant.now()).toEpochMilli()
        val alarms = context.getSystemService(AlarmManager::class.java)
        val pi = ringLimitIntent(context, intakeAt, kind)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()) {
            // v1.1.1: alarma de reloj, como los avisos. En el teléfono, HyperOS agrupaba el corte hecho con
            // setExactAndAllowWhileIdle y la alarma sonaba hasta 6 minutos con la pantalla apagada.
            alarms.setAlarmClock(AlarmManager.AlarmClockInfo(at, alarmScreen(context, intakeAt, kind)), pi)
        } else {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    // Un id por hora del día: avisos de horas distintas no se reemplazan entre sí
    fun notificationId(intakeAt: LocalDateTime) = intakeAt.hour * 60 + intakeAt.minute

    private fun hhmm(t: LocalDateTime) = "%02d:%02d".format(t.hour, t.minute)

    private fun openApp(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun alarmScreen(context: Context, intakeAt: LocalDateTime, kind: ReminderKind): PendingIntent =
        PendingIntent.getActivity(
            context,
            2_000_000 + notificationId(intakeAt),
            Intent(context, AlarmActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(AlarmActivity.EXTRA_INTAKE_AT, intakeAt.toString())
                .putExtra(AlarmActivity.EXTRA_KIND, kind.name),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun takeIntent(context: Context, intakeAt: LocalDateTime): PendingIntent = PendingIntent.getBroadcast(
        context,
        3_000_000 + notificationId(intakeAt),
        Intent(context, TakeReceiver::class.java)
            .setAction(TakeReceiver.ACTION)
            .putExtra(TakeReceiver.EXTRA_INTAKE_AT, intakeAt.toString()),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

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

    private fun ringLimitIntent(context: Context, intakeAt: LocalDateTime, kind: ReminderKind): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            4_000_000 + notificationId(intakeAt),
            Intent(context, RingLimitReceiver::class.java)
                .setAction(RingLimitReceiver.ACTION)
                .putExtra(RingLimitReceiver.EXTRA_INTAKE_AT, intakeAt.toString())
                .putExtra(RingLimitReceiver.EXTRA_KIND, kind.name),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
}
