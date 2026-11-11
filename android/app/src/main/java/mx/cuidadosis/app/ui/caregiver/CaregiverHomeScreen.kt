package mx.cuidadosis.app.ui.caregiver

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Pantalla provisional del cuidador; la lista de pacientes llega en la Fase 4
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaregiverHomeScreen(userName: String, onLogout: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("CuidaDosis: cuidador") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Hola, $userName", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Aquí verás a tus pacientes vinculados y el estado de sus tomas de hoy. " +
                    "Esta vista llega en la siguiente fase del proyecto.",
                style = MaterialTheme.typography.bodyLarge,
            )
            OutlinedButton(onClick = onLogout) { Text("Cerrar sesión") }
        }
    }
}
