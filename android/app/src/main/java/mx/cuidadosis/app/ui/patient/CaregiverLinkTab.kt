package mx.cuidadosis.app.ui.patient

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import mx.cuidadosis.app.ui.common.ErrorText
import mx.cuidadosis.app.ui.common.LoadingBox
import mx.cuidadosis.app.ui.common.formatApiDateTime

// Pestaña "Cuidador" del paciente: código de invitación o cuidador vinculado
@Composable
fun CaregiverLinkTab(
    snackbar: SnackbarHostState,
    viewModel: CaregiverLinkViewModel = viewModel(factory = CaregiverLinkViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.load() }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.messageShown()
        }
    }

    val me = state.me
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when {
            state.loading && me == null -> LoadingBox(Modifier.fillMaxWidth().padding(32.dp))
            me == null -> {
                ErrorText(state.error ?: "No se pudo cargar la información.")
                OutlinedButton(onClick = viewModel::load) { Text("Reintentar") }
            }
            me.link?.caregiver != null -> {
                val caregiver = me.link.caregiver
                Text("Tu cuidador", style = MaterialTheme.typography.titleLarge)
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(caregiver.fullName, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Vinculado desde ${formatApiDateTime(me.link.linkedAt)}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                Text(
                    "Tu cuidador puede ver tus medicamentos de hoy y si ya marcaste cada toma. " +
                        "Solo puedes tener un cuidador a la vez.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedButton(onClick = viewModel::askUnlink, enabled = !state.busy) {
                    Text("Desvincular cuidador")
                }
            }
            else -> {
                Text("No tienes cuidador vinculado", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Genera un código y compártelo con la persona que te cuida. Ella lo escribe en su " +
                        "app, en \"Vincular paciente\". El código dura 48 horas y sirve una sola vez.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                val invite = me.pendingInvite
                if (invite != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Tu código de invitación", style = MaterialTheme.typography.labelLarge)
                            Text(
                                invite.inviteCode,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 36.sp,
                                letterSpacing = 4.sp,
                            )
                            Text("Vence: ${formatApiDateTime(invite.expiresAt)}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    OutlinedButton(onClick = viewModel::generateCode, enabled = !state.busy) {
                        Text("Generar código nuevo")
                    }
                    Text(
                        "Si generas uno nuevo, el código anterior deja de funcionar.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else {
                    Button(onClick = viewModel::generateCode, enabled = !state.busy) {
                        Text("Generar código de invitación")
                    }
                }
            }
        }
    }

    if (state.confirmUnlink) {
        val name = state.me?.link?.caregiver?.fullName.orEmpty()
        AlertDialog(
            onDismissRequest = viewModel::cancelUnlink,
            title = { Text("¿Desvincular a $name?") },
            text = {
                Text("Dejará de ver tus tomas. Después podrás invitar a otro cuidador con un código nuevo.")
            },
            confirmButton = { TextButton(onClick = viewModel::confirmUnlink) { Text("Desvincular") } },
            dismissButton = { TextButton(onClick = viewModel::cancelUnlink) { Text("Cancelar") } },
        )
    }
}
