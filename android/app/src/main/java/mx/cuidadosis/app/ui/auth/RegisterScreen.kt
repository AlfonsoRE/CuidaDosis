package mx.cuidadosis.app.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import mx.cuidadosis.app.data.local.Session
import mx.cuidadosis.app.ui.common.ErrorText
import mx.cuidadosis.app.ui.common.FormField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onBack: () -> Unit,
    viewModel: RegisterViewModel = viewModel(factory = RegisterViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val roles = listOf(Session.ROLE_PATIENT to "Paciente", Session.ROLE_CAREGIVER to "Cuidador")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Crear cuenta") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Atrás") } },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("¿Cómo usarás la app?", style = MaterialTheme.typography.titleSmall)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                roles.forEachIndexed { index, (value, label) ->
                    SegmentedButton(
                        selected = state.role == value,
                        onClick = { viewModel.onRole(value) },
                        shape = SegmentedButtonDefaults.itemShape(index, roles.size),
                        enabled = !state.loading,
                    ) { Text(label) }
                }
            }
            Text(
                if (state.role == Session.ROLE_PATIENT) {
                    "Registras tus medicamentos y marcas tus tomas."
                } else {
                    "Consultas las tomas de los pacientes que te vinculen."
                },
                style = MaterialTheme.typography.bodySmall,
            )

            FormField(
                value = state.fullName,
                onValueChange = viewModel::onFullName,
                label = "Nombre completo",
                error = state.fieldErrors["full_name"],
                enabled = !state.loading,
            )
            FormField(
                value = state.email,
                onValueChange = viewModel::onEmail,
                label = "Correo",
                keyboardType = KeyboardType.Email,
                error = state.fieldErrors["email"],
                enabled = !state.loading,
            )
            FormField(
                value = state.password,
                onValueChange = viewModel::onPassword,
                label = "Contraseña",
                keyboardType = KeyboardType.Password,
                isPassword = true,
                error = state.fieldErrors["password"],
                supportingText = "Mínimo 8 caracteres.",
                enabled = !state.loading,
            )
            FormField(
                value = state.confirmPassword,
                onValueChange = viewModel::onConfirm,
                label = "Repite la contraseña",
                keyboardType = KeyboardType.Password,
                isPassword = true,
                error = state.fieldErrors["confirm"],
                enabled = !state.loading,
            )
            state.fieldErrors["role"]?.let { ErrorText(it) }
            state.error?.let { ErrorText(it) }

            Button(
                onClick = viewModel::register,
                enabled = !state.loading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.loading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Text("Crear cuenta")
                }
            }
        }
    }
}
