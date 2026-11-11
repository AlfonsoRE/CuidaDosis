package mx.cuidadosis.app.ui.medications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import mx.cuidadosis.app.data.remote.MedicationDto
import mx.cuidadosis.app.ui.common.DoseDisclaimer
import mx.cuidadosis.app.ui.common.ErrorText
import mx.cuidadosis.app.ui.common.LoadingBox

// Pestaña "Medicamentos" del paciente (el botón "Agregar" vive en PatientHomeScreen)
@Composable
fun MedicationListTab(
    snackbar: SnackbarHostState,
    onEdit: (Int) -> Unit,
    viewModel: MedicationListViewModel = viewModel(factory = MedicationListViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Recarga al mostrar la pestaña (también al volver del formulario)
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.load() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.messageShown()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { DoseDisclaimer() }
        when {
            state.loading && state.medications.isEmpty() -> item {
                LoadingBox(Modifier.fillMaxWidth().padding(32.dp))
            }
            state.error != null -> item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ErrorText(state.error!!)
                    OutlinedButton(onClick = viewModel::load) { Text("Reintentar") }
                }
            }
            state.medications.isEmpty() -> item {
                Text(
                    "Aún no tienes medicamentos. Toca \"Agregar\" para registrar el primero.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            }
            else -> items(state.medications, key = { it.id }) { med ->
                MedicationCard(
                    med = med,
                    onEdit = { onEdit(med.id) },
                    onDeactivate = { viewModel.askDeactivate(med) },
                )
            }
        }
    }

    state.pendingDeactivate?.let { med ->
        AlertDialog(
            onDismissRequest = viewModel::cancelDeactivate,
            title = { Text("¿Desactivar ${med.name}?") },
            text = { Text("Dejará de aparecer en tu lista y en tus tomas. El historial de tomas se conserva.") },
            confirmButton = { TextButton(onClick = viewModel::confirmDeactivate) { Text("Desactivar") } },
            dismissButton = { TextButton(onClick = viewModel::cancelDeactivate) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun MedicationCard(med: MedicationDto, onEdit: () -> Unit, onDeactivate: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(med.name, style = MaterialTheme.typography.titleMedium)
            Text(med.dose, style = MaterialTheme.typography.bodyMedium)
            Text("Horario: ${med.times.joinToString(", ")}", style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onEdit) { Text("Editar") }
                TextButton(onClick = onDeactivate) { Text("Desactivar") }
            }
        }
    }
}
