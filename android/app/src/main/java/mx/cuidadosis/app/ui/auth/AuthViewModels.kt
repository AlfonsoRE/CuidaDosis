package mx.cuidadosis.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mx.cuidadosis.app.container
import mx.cuidadosis.app.data.AuthRepository
import mx.cuidadosis.app.data.local.Session
import mx.cuidadosis.app.data.remote.ApiResult

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val error: String? = null,
)

// Al iniciar sesión se guarda la sesión y la navegación cambia sola a la pantalla del rol
class LoginViewModel(private val auth: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onEmail(value: String) = _state.update { it.copy(email = value, error = null) }
    fun onPassword(value: String) = _state.update { it.copy(password = value, error = null) }

    fun login() {
        val s = _state.value
        if (s.email.isBlank() || s.password.isEmpty()) {
            _state.update { it.copy(error = "Escribe tu correo y tu contraseña.") }
            return
        }
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val result = auth.login(s.email, s.password)
            _state.update {
                when (result) {
                    is ApiResult.Success -> it.copy(loading = false, password = "")
                    is ApiResult.Error -> it.copy(loading = false, error = result.message)
                }
            }
        }
    }

    companion object {
        val Factory = viewModelFactory { initializer { LoginViewModel(container.authRepository) } }
    }
}

data class RegisterUiState(
    val fullName: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val role: String = Session.ROLE_PATIENT,
    val loading: Boolean = false,
    val fieldErrors: Map<String, String> = emptyMap(),
    val error: String? = null,
)

class RegisterViewModel(private val auth: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow(RegisterUiState())
    val state: StateFlow<RegisterUiState> = _state.asStateFlow()

    fun onFullName(v: String) = _state.update { it.copy(fullName = v, fieldErrors = it.fieldErrors - "full_name") }
    fun onEmail(v: String) = _state.update { it.copy(email = v, fieldErrors = it.fieldErrors - "email") }
    fun onPassword(v: String) = _state.update { it.copy(password = v, fieldErrors = it.fieldErrors - "password") }
    fun onConfirm(v: String) = _state.update { it.copy(confirmPassword = v, fieldErrors = it.fieldErrors - "confirm") }
    fun onRole(v: String) = _state.update { it.copy(role = v) }

    fun register() {
        val s = _state.value
        val errors = buildMap {
            if (s.fullName.isBlank()) put("full_name", "Escribe tu nombre.")
            if (s.email.isBlank()) put("email", "Escribe tu correo.")
            if (s.password.length < 8) put("password", "Debe tener al menos 8 caracteres.")
            if (s.password != s.confirmPassword) put("confirm", "Las contraseñas no coinciden.")
        }
        if (errors.isNotEmpty()) {
            _state.update { it.copy(fieldErrors = errors, error = null) }
            return
        }
        _state.update { it.copy(loading = true, error = null, fieldErrors = emptyMap()) }
        viewModelScope.launch {
            val result = auth.register(s.fullName, s.email, s.password, s.role)
            _state.update {
                when (result) {
                    is ApiResult.Success -> it.copy(loading = false)
                    is ApiResult.Error -> it.copy(
                        loading = false,
                        fieldErrors = result.fieldErrors,
                        error = if (result.fieldErrors.isEmpty()) result.message else null,
                    )
                }
            }
        }
    }

    companion object {
        val Factory = viewModelFactory { initializer { RegisterViewModel(container.authRepository) } }
    }
}
