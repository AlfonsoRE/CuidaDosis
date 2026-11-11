package mx.cuidadosis.app.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import mx.cuidadosis.app.ui.common.ErrorText
import mx.cuidadosis.app.ui.common.FormField

@Composable
fun LoginScreen(
    serverUrl: String,
    onRegister: () -> Unit,
    onServer: () -> Unit,
    viewModel: LoginViewModel = viewModel(factory = LoginViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(32.dp))
        Text("CuidaDosis", style = MaterialTheme.typography.headlineLarge)
        Text(
            "Recordatorio y registro de tomas de medicamentos",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(16.dp))

        FormField(
            value = state.email,
            onValueChange = viewModel::onEmail,
            label = "Correo",
            keyboardType = KeyboardType.Email,
            enabled = !state.loading,
        )
        FormField(
            value = state.password,
            onValueChange = viewModel::onPassword,
            label = "Contraseña",
            keyboardType = KeyboardType.Password,
            isPassword = true,
            enabled = !state.loading,
        )
        state.error?.let { ErrorText(it, Modifier.fillMaxWidth()) }

        Button(
            onClick = viewModel::login,
            enabled = !state.loading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.loading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
            } else {
                Text("Iniciar sesión")
            }
        }
        TextButton(onClick = onRegister, enabled = !state.loading) {
            Text("¿No tienes cuenta? Regístrate")
        }

        Spacer(Modifier.height(24.dp))
        TextButton(onClick = onServer, enabled = !state.loading) {
            Text("Servidor: $serverUrl", style = MaterialTheme.typography.bodySmall)
        }
    }
}
