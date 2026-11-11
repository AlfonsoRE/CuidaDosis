package mx.cuidadosis.app.ui.caregiver

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import mx.cuidadosis.app.data.remote.CaregiverPatientDto
import mx.cuidadosis.app.ui.common.ErrorText
import mx.cuidadosis.app.ui.common.LoadingBox
import mx.cuidadosis.app.ui.common.formatApiDate
import mx.cuidadosis.app.ui.common.summaryText

// Pantalla principal del cuidador: sus pacientes con el resumen de hoy
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaregiverHomeScreen(
    userName: String,
    onOpenPatient: (id: Int, name: String) -> Unit,
    onLogout: () -> Unit,
    viewModel: CaregiverHomeViewModel = viewModel(factory = CaregiverHomeViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.load() }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.messageShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Mis pacientes")
                        Text(userName, style = MaterialTheme.typography.bodySmall)
                    }
                },
                actions = { TextButton(onClick = onLogout) { Text("Cerrar sesión") } },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = viewModel::openLinkDialog) { Text("+ Vincular paciente") }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        val data = state.data
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        if (data != null) "Hoy, ${formatApiDate(data.date)}" else "Hoy",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = viewModel::load, enabled = !state.loading) { Text("Actualizar") }
                }
            }
            when {
                state.loading && data == null -> item { LoadingBox(Modifier.fillMaxWidth().padding(32.dp)) }
                data == null -> item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ErrorText(state.error ?: "No se pudo cargar la lista.")
                        OutlinedButton(onClick = viewModel::load) { Text("Reintentar") }
                    }
                }
                data.patients.isEmpty() -> item {
                    Text(
                        "Aún no sigues a ningún paciente. Pide a tu paciente su código de invitación " +
                            "y toca \"Vincular paciente\".",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(vertical = 24.dp),
                    )
                }
                else -> {
                    state.error?.let { item { ErrorText(it) } }
                    items(data.patients, key = { it.id }) { patient ->
                        PatientCard(patient, onClick = { onOpenPatient(patient.id, patient.fullName) })
                    }
                }
            }
        }
    }

    if (state.showLinkDialog) {
        AlertDialog(
            onDismissRequest = viewModel::closeLinkDialog,
            title = { Text("Vincular paciente") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Escribe el código de 8 caracteres que generó tu paciente en su app.")
                    OutlinedTextField(
                        value = state.code,
                        onValueChange = viewModel::onCode,
                        label = { Text("Código de invitación") },
                        singleLine = true,
                        isError = state.codeError != null,
                        supportingText = state.codeError?.let { { Text(it) } },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                        enabled = !state.linking,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::link, enabled = !state.linking) { Text("Vincular") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::closeLinkDialog, enabled = !state.linking) { Text("Cancelar") }
            },
        )
    }
}

@Composable
private fun PatientCard(patient: CaregiverPatientDto, onClick: () -> Unit) {
    val s = patient.summary
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(patient.fullName, style = MaterialTheme.typography.titleMedium)
            Text(summaryText(s.taken, s.total), style = MaterialTheme.typography.bodyMedium)
            if (s.pending > 0) {
                Text("Pendientes: ${s.pending}", style = MaterialTheme.typography.bodySmall)
            }
            Text("Toca para ver el detalle", style = MaterialTheme.typography.labelSmall)
        }
    }
}
