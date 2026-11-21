package mx.cuidadosis.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mx.cuidadosis.app.AppContainer
import mx.cuidadosis.app.reminders.REMINDER_LEAD_OPTIONS
import mx.cuidadosis.app.ui.patient.PermissionsCard

// Ajustes del paciente: anticipación L del aviso (solo en este teléfono, RF-05) y estado de permisos (RF-14)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderSettingsScreen(container: AppContainer, onBack: () -> Unit) {
    val lead by container.settingsStore.reminderLead.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Volver") } },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Anticipación del aviso", style = MaterialTheme.typography.titleLarge)
            Text(
                "¿Cuántos minutos antes de cada toma quieres el primer aviso? Si a la hora de la toma " +
                    "sigue pendiente, el teléfono avisa una vez más.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Column(Modifier.selectableGroup()) {
                REMINDER_LEAD_OPTIONS.forEach { minutes ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .selectable(
                                selected = lead == minutes,
                                role = Role.RadioButton,
                                onClick = {
                                    container.settingsStore.setReminderLead(minutes)
                                    container.reminderScheduler.reschedule()
                                },
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = lead == minutes, onClick = null)
                        Text(
                            "$minutes minutos antes" + if (minutes == 5) " (por defecto)" else "",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 12.dp),
                        )
                    }
                }
            }
            Text(
                "Este ajuste solo se guarda en este teléfono.",
                style = MaterialTheme.typography.bodyMedium,
            )
            PermissionsCard(showWhenOk = true)
        }
    }
}
