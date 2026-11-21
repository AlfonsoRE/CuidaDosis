package mx.cuidadosis.app.ui.caregiver

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import mx.cuidadosis.app.container
import mx.cuidadosis.app.ui.common.HistoryList
import mx.cuidadosis.app.ui.common.HistoryViewModel

// Historial de 7 días de un paciente vinculado (RF-15). Sin dosis salvo autorización (G-07).
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaregiverHistoryScreen(patientId: Int, patientName: String, onBack: () -> Unit) {
    val vm: HistoryViewModel = viewModel(
        key = "historial-$patientId",
        factory = viewModelFactory {
            initializer { HistoryViewModel { container.intakeRepository.caregiverPatientHistory(patientId) } }
        },
    )
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(patientName) },
                navigationIcon = { TextButton(onClick = onBack) { Text("Atrás") } },
            )
        },
    ) { padding ->
        HistoryList(vm, Modifier.padding(padding))
    }
}
