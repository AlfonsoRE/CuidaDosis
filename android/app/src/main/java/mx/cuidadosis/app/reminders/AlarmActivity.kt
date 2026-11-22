package mx.cuidadosis.app.reminders

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import mx.cuidadosis.app.CuidaDosisApp
import mx.cuidadosis.app.data.CONFIRMATION_DISCLAIMER
import mx.cuidadosis.app.ui.theme.CuidaDosisTheme
import java.time.LocalDateTime

/**
 * Pantalla de la alarma de la toma (RF-06): se muestra sobre la pantalla de bloqueo. "Tomado"
 * confirma con un toque (RF-07) y "Posponer" vuelve a avisar al plazo (RF-16); ambos detienen el
 * sonido. Nombre y dosis se muestran tal como los escribió el usuario.
 */
class AlarmActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)

        val intakeAt = intent.getStringExtra(EXTRA_INTAKE_AT)
            ?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() }
        if (intakeAt == null) {
            finish()
            return
        }
        val scheduler = (application as CuidaDosisApp).container.reminderScheduler
        val meds = scheduler.pendingMedsFor(intakeAt)
        if (meds.isEmpty()) {
            // Ya confirmada: no hay nada que atender
            ReminderNotifier.stopAlarm(this, intakeAt)
            finish()
            return
        }
        val time = "%02d:%02d".format(intakeAt.hour, intakeAt.minute)

        setContent {
            CuidaDosisTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text("Es la hora de tu toma", style = MaterialTheme.typography.titleLarge)
                        Text(time, style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Bold)
                        meds.forEach { med ->
                            Column {
                                Text(med.name, style = MaterialTheme.typography.titleMedium)
                                Text(med.dose, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                        Button(
                            onClick = {
                                AlarmActions.take(this@AlarmActivity, intakeAt)
                                finish()
                            },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
                        ) { Text("Tomado", style = MaterialTheme.typography.titleLarge) }
                        SNOOZE_OPTIONS.forEach { minutes ->
                            OutlinedButton(
                                onClick = {
                                    AlarmActions.snooze(this@AlarmActivity, intakeAt, minutes)
                                    finish()
                                },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                            ) { Text("Posponer $minutes min", style = MaterialTheme.typography.titleMedium) }
                        }
                        Text(CONFIRMATION_DISCLAIMER, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_INTAKE_AT = "intake_at"
        const val EXTRA_KIND = "kind"
    }
}
