package mx.cuidadosis.app.ui.caregiver

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import mx.cuidadosis.app.ui.common.ErrorText
import mx.cuidadosis.app.ui.common.IntakeCard
import mx.cuidadosis.app.ui.common.LoadingBox
import mx.cuidadosis.app.ui.common.formatApiDate
import mx.cuidadosis.app.ui.common.summaryText

// Detalle de un paciente para el cuidador: tomas de hoy en solo lectura
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaregiverPatientScreen(
    patientName: String,
    onBack: () -> Unit,
    viewModel: CaregiverPatientViewModel = viewModel(factory = CaregiverPatientViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.load() }
    LaunchedEffect(state.unlinked) { if (state.unlinked) onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(patientName) },
                navigationIcon = { TextButton(onClick = onBack) { Text("Atrás") } },
            )
        },
    ) { padding ->
        val today = state.today
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (today != null) "Hoy, ${formatApiDate(today.date)}" else "Hoy",
                            style = MaterialTheme.typography.titleLarge,
                        )
                        today?.let {
                            Text(summaryText(it.summary.taken, it.summary.total), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    TextButton(onClick = viewModel::load, enabled = !state.loading) { Text("Actualizar") }
                }
                Text(
                    "Solo lectura: el paciente marca sus tomas desde su app. \"Tomada\" indica que el " +
                        "paciente tocó el botón, no confirma la ingesta.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            when {
                state.notLinked -> item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ErrorText("Ya no estás vinculado con este paciente.")
                        OutlinedButton(onClick = onBack) { Text("Volver a mis pacientes") }
                    }
                }
                state.loading && today == null -> item { LoadingBox(Modifier.fillMaxWidth().padding(32.dp)) }
                today == null -> item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ErrorText(state.error ?: "No se pudo cargar.")
                        OutlinedButton(onClick = viewModel::load) { Text("Reintentar") }
                    }
                }
                else -> {
                    state.error?.let { item { ErrorText(it) } }
                    if (today.intakes.isEmpty()) {
                        item { Text("No tiene tomas programadas hoy.", style = MaterialTheme.typography.bodyLarge) }
                    }
                    items(today.intakes, key = { it.id }) { intake -> IntakeCard(intake) }
                    item {
                        OutlinedButton(
                            onClick = viewModel::askUnlink,
                            enabled = !state.busy,
                            modifier = Modifier.padding(top = 16.dp),
                        ) { Text("Dejar de seguir a este paciente") }
                    }
                }
            }
        }
    }

    if (state.confirmUnlink) {
        AlertDialog(
            onDismissRequest = viewModel::cancelUnlink,
            title = { Text("¿Dejar de seguir a $patientName?") },
            text = { Text("Ya no verás sus tomas. Para volver a seguirlo necesitarás un código nuevo de su parte.") },
            confirmButton = { TextButton(onClick = viewModel::confirmUnlink) { Text("Dejar de seguir") } },
            dismissButton = { TextButton(onClick = viewModel::cancelUnlink) { Text("Cancelar") } },
        )
    }
}
