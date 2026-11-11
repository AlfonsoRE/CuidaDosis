package mx.cuidadosis.app.ui.medications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import mx.cuidadosis.app.ui.common.DoseDisclaimer
import mx.cuidadosis.app.ui.common.ErrorText
import mx.cuidadosis.app.ui.common.FormField
import mx.cuidadosis.app.ui.common.LoadingBox

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MedicationFormScreen(
    onDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: MedicationFormViewModel = viewModel(factory = MedicationFormViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showPicker by remember { mutableStateOf(false) }

    LaunchedEffect(state.saved) { if (state.saved) onDone() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEdit) "Editar medicamento" else "Nuevo medicamento") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Atrás") } },
            )
        },
    ) { padding ->
        if (state.loading) {
            LoadingBox(Modifier.fillMaxSize().padding(padding))
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DoseDisclaimer()
            FormField(
                value = state.name,
                onValueChange = viewModel::onName,
                label = "Nombre del medicamento",
                error = state.fieldErrors["name"],
                enabled = !state.saving,
            )
            FormField(
                value = state.dose,
                onValueChange = viewModel::onDose,
                label = "Dosis (como te la indicó tu médico)",
                error = state.fieldErrors["dose"],
                enabled = !state.saving,
            )

            Text("Horas diarias", style = MaterialTheme.typography.titleSmall)
            if (state.times.isEmpty()) {
                Text("Todavía no hay horas.", style = MaterialTheme.typography.bodyMedium)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.times.forEach { time ->
                    InputChip(
                        selected = false,
                        onClick = { viewModel.removeTime(time) },
                        label = { Text("$time  ✕") },
                        enabled = !state.saving,
                    )
                }
            }
            Text("Toca una hora para quitarla.", style = MaterialTheme.typography.bodySmall)
            state.fieldErrors["times"]?.let { ErrorText(it) }
            OutlinedButton(onClick = { showPicker = true }, enabled = !state.saving) { Text("Agregar hora") }

            state.error?.let { ErrorText(it) }
            Button(
                onClick = viewModel::save,
                enabled = !state.saving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.saving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Text(if (state.isEdit) "Guardar cambios" else "Guardar medicamento")
                }
            }
        }
    }

    if (showPicker) {
        val pickerState = rememberTimePickerState(initialHour = 8, initialMinute = 0, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showPicker = false },
            title = { Text("Elige la hora") },
            text = { TimePicker(state = pickerState) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.addTime(pickerState.hour, pickerState.minute)
                    showPicker = false
                }) { Text("Agregar") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Cancelar") } },
        )
    }
}
