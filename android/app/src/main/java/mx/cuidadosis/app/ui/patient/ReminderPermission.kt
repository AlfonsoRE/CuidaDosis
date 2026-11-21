package mx.cuidadosis.app.ui.patient

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import mx.cuidadosis.app.reminders.ReminderNotifier

// Android 13+: pide permiso de notificaciones una vez al entrar a la pantalla principal
@Composable
fun RequestNotificationPermission() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    var asked by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (!asked && !ReminderNotifier.canNotify(context)) {
            asked = true
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

// Lo que puede impedir que un aviso suene a tiempo (RF-14)
data class PermissionStatus(
    val notifications: Boolean,
    val exactAlarms: Boolean,
    val batteryUnrestricted: Boolean,
) {
    val allOk: Boolean get() = notifications && exactAlarms && batteryUnrestricted
}

fun permissionStatus(context: Context): PermissionStatus {
    val alarms = context.getSystemService(AlarmManager::class.java)
    val power = context.getSystemService(PowerManager::class.java)
    return PermissionStatus(
        notifications = ReminderNotifier.canNotify(context),
        exactAlarms = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms(),
        batteryUnrestricted = power.isIgnoringBatteryOptimizations(context.packageName),
    )
}

/**
 * Advertencia con el paso para corregir cada permiso que falte. Se revisa al volver a la app,
 * así que desaparece en cuanto el usuario concede el permiso. [showWhenOk] muestra la tarjeta
 * aunque todo esté bien (pantalla de ajustes).
 */
@Composable
fun PermissionsCard(showWhenOk: Boolean = false) {
    val context = LocalContext.current
    var status by remember { mutableStateOf(permissionStatus(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { status = permissionStatus(context) }
    if (status.allOk && !showWhenOk) return

    val warn = !status.allOk
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (warn) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val color = if (warn) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant
            Text(
                if (warn) "Los avisos pueden no sonar a tiempo" else "Permisos de avisos: todo en orden",
                style = MaterialTheme.typography.titleMedium,
                color = color,
            )
            if (!status.notifications) {
                Text("Las notificaciones están desactivadas.", style = MaterialTheme.typography.bodyLarge, color = color)
                FixButton("Activar notificaciones") {
                    context.startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                    )
                }
            }
            if (!status.exactAlarms && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Text("Las alarmas exactas no están permitidas.", style = MaterialTheme.typography.bodyLarge, color = color)
                FixButton("Permitir alarmas") {
                    context.startActivity(
                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")),
                    )
                }
            }
            if (!status.batteryUnrestricted) {
                Text(
                    "El ahorro de batería puede retrasar los avisos. Quita la restricción de batería para CuidaDosis.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = color,
                )
                FixButton("Ajustar batería") { openBatterySettings(context) }
            }
            if (warn) {
                Text(
                    "Revisa también que el modo No molestar permita los avisos de CuidaDosis.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = color,
                )
            }
        }
    }
}

@Composable
private fun FixButton(label: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.heightIn(min = 48.dp)) {
        Text(label, style = MaterialTheme.typography.titleSmall)
    }
}

// Abre la lista de optimización de batería (no se pide la exención directa para no requerir otro permiso)
@SuppressLint("BatteryLife")
private fun openBatterySettings(context: Context) {
    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
    runCatching { context.startActivity(intent) }.onFailure {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")),
        )
    }
}
