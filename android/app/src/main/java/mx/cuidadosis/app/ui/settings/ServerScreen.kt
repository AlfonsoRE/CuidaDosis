package mx.cuidadosis.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mx.cuidadosis.app.container
import mx.cuidadosis.app.data.AuthRepository
import mx.cuidadosis.app.data.local.BaseUrl
import mx.cuidadosis.app.data.local.SettingsStore
import mx.cuidadosis.app.data.remote.ApiResult
import mx.cuidadosis.app.ui.common.ErrorText
import mx.cuidadosis.app.ui.common.FormField

data class ServerUiState(
    val url: String,
    val busy: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)

// Permite cambiar la URL de la API sin recompilar (por ejemplo, para un teléfono físico)
class ServerViewModel(
    private val settings: SettingsStore,
    private val auth: AuthRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ServerUiState(url = settings.baseUrl.value))
    val state: StateFlow<ServerUiState> = _state.asStateFlow()

    fun onUrl(v: String) = _state.update { it.copy(url = v, message = null, error = null) }

    fun test() {
        val url = BaseUrl.normalize(_state.value.url)
            ?: return _state.update { it.copy(error = INVALID_URL, message = null) }
        _state.update { it.copy(url = url, busy = true, message = null, error = null) }
        viewModelScope.launch {
            val result = auth.checkServer(url)
            _state.update {
                when (result) {
                    is ApiResult.Success -> if (result.data.status == "ok") {
                        it.copy(busy = false, message = "Conexión correcta con la API.")
                    } else {
                        it.copy(busy = false, error = "El servidor respondió, pero no parece la API de CuidaDosis.")
                    }
                    is ApiResult.Error -> it.copy(busy = false, error = result.message)
                }
            }
        }
    }

    fun save(): Boolean {
        val url = BaseUrl.normalize(_state.value.url)
        if (url == null) {
            _state.update { it.copy(error = INVALID_URL, message = null) }
            return false
        }
        settings.setBaseUrl(url)
        return true
    }

    fun reset() {
        settings.reset()
        _state.update { ServerUiState(url = settings.baseUrl.value, message = "Se restableció la dirección por defecto.") }
    }

    companion object {
        private const val INVALID_URL = "Escribe una dirección que empiece con http:// o https://"
        val Factory = viewModelFactory {
            initializer { ServerViewModel(container.settingsStore, container.authRepository) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerScreen(
    onBack: () -> Unit,
    viewModel: ServerViewModel = viewModel(factory = ServerViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Servidor de la API") },
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FormField(
                value = state.url,
                onValueChange = viewModel::onUrl,
                label = "URL base",
                keyboardType = KeyboardType.Uri,
                enabled = !state.busy,
            )
            Text(
                "Emulador: http://10.0.2.2/CuidaDosis/api/public/\n" +
                    "Teléfono en la misma red: http://<IP de la PC>/CuidaDosis/api/public/",
                style = MaterialTheme.typography.bodySmall,
            )
            state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            state.error?.let { ErrorText(it) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = viewModel::test, enabled = !state.busy) { Text("Probar conexión") }
                Button(onClick = { if (viewModel.save()) onBack() }, enabled = !state.busy) { Text("Guardar") }
            }
            TextButton(onClick = viewModel::reset, enabled = !state.busy) { Text("Restablecer dirección por defecto") }
        }
    }
}
