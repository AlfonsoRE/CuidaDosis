package mx.cuidadosis.app.ui.patient

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import mx.cuidadosis.app.ui.medications.MedicationListTab

enum class PatientTab(val label: String) {
    TODAY("Hoy"),
    MEDICATIONS("Medicamentos"),
    CAREGIVER("Cuidador"),
}

// Pantalla principal del paciente: pestañas Hoy, Medicamentos y Cuidador
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientHomeScreen(
    userName: String,
    onAddMedication: () -> Unit,
    onEditMedication: (Int) -> Unit,
    onLogout: () -> Unit,
) {
    var tab by rememberSaveable { mutableStateOf(PatientTab.TODAY) }
    val snackbar = remember { SnackbarHostState() }
    RequestNotificationPermission()

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text("CuidaDosis")
                            Text(userName, style = MaterialTheme.typography.bodySmall)
                        }
                    },
                    actions = { TextButton(onClick = onLogout) { Text("Cerrar sesión") } },
                )
                PrimaryTabRow(selectedTabIndex = tab.ordinal) {
                    PatientTab.entries.forEach { t ->
                        Tab(selected = tab == t, onClick = { tab = t }, text = { Text(t.label) })
                    }
                }
            }
        },
        floatingActionButton = {
            if (tab == PatientTab.MEDICATIONS) {
                ExtendedFloatingActionButton(onClick = onAddMedication) { Text("+ Agregar") }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                PatientTab.TODAY -> TodayTab(snackbar)
                PatientTab.MEDICATIONS -> MedicationListTab(snackbar, onEdit = onEditMedication)
                PatientTab.CAREGIVER -> CaregiverLinkTab(snackbar)
            }
        }
    }
}
