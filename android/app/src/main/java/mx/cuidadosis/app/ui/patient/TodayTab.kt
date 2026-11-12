package mx.cuidadosis.app.ui.patient

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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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

@Composable
fun TodayTab(
    snackbar: SnackbarHostState,
    viewModel: TodayViewModel = viewModel(factory = TodayViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.load() }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.messageShown()
        }
    }

    val today = state.today
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
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
                "Toca \"Tomado\" cuando tomes tu medicamento. Solo registra que lo marcaste.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        item { NotificationsOffCard() }
        when {
            state.loading && today == null -> item { LoadingBox(Modifier.fillMaxWidth().padding(32.dp)) }
            state.error != null && today == null -> item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ErrorText(state.error!!)
                    OutlinedButton(onClick = viewModel::load) { Text("Reintentar") }
                }
            }
            today != null && today.intakes.isEmpty() -> item {
                Text(
                    "No tienes tomas programadas hoy. Agrega medicamentos en la pestaña \"Medicamentos\".",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            }
            today != null -> {
                state.error?.let { item { ErrorText(it) } }
                items(today.intakes, key = { it.id }) { intake ->
                    IntakeCard(intake) {
                        if (!intake.isTaken) {
                            Button(
                                onClick = { viewModel.askTake(intake) },
                                enabled = !state.taking,
                            ) { Text("Tomado") }
                        }
                    }
                }
            }
        }
    }

    state.pendingTake?.let { intake ->
        AlertDialog(
            onDismissRequest = viewModel::cancelTake,
            title = { Text("¿Registrar la toma?") },
            text = {
                Text(
                    "${intake.name}, ${intake.dose}, de las ${intake.time}.\n" +
                        "Se registrará como tomada ahora. No se puede deshacer.",
                )
            },
            confirmButton = { TextButton(onClick = viewModel::confirmTake) { Text("Sí, la tomé") } },
            dismissButton = { TextButton(onClick = viewModel::cancelTake) { Text("Cancelar") } },
        )
    }
}
