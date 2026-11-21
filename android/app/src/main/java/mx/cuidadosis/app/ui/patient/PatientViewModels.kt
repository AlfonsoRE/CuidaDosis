package mx.cuidadosis.app.ui.patient

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
import mx.cuidadosis.app.data.IntakeRepository
import mx.cuidadosis.app.data.LinkRepository
import mx.cuidadosis.app.data.remote.ApiResult
import mx.cuidadosis.app.data.remote.IntakeDto
import mx.cuidadosis.app.data.remote.MeResponse
import mx.cuidadosis.app.data.remote.TodayResponse

data class TodayUiState(
    val loading: Boolean = true,
    val today: TodayResponse? = null,
    val error: String? = null,
    // Tomas que se están confirmando (para desactivar su botón)
    val taking: Set<Int> = emptySet(),
    val message: String? = null,
)

// Tomas de hoy del paciente y botón "Tomado" (un toque, sin diálogo; RF-07)
class TodayViewModel(private val repo: IntakeRepository) : ViewModel() {
    private val _state = MutableStateFlow(TodayUiState())
    val state: StateFlow<TodayUiState> = _state.asStateFlow()

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val result = repo.today()
            _state.update {
                when (result) {
                    is ApiResult.Success -> it.copy(loading = false, today = result.data)
                    is ApiResult.Error -> it.copy(loading = false, error = result.message)
                }
            }
        }
    }

    fun messageShown() = _state.update { it.copy(message = null) }

    fun take(intake: IntakeDto) {
        if (intake.id in _state.value.taking) return
        _state.update { it.copy(taking = it.taking + intake.id) }
        viewModelScope.launch {
            val result = repo.take(intake)
            _state.update {
                it.copy(
                    taking = it.taking - intake.id,
                    message = when (result) {
                        is ApiResult.Success -> "Confirmada por el paciente: ${intake.name}, ${intake.time}."
                        is ApiResult.Error -> result.message
                    },
                )
            }
            // Se recarga siempre: si la toma ya estaba confirmada (409) se ve el estado real
            load()
        }
    }

    companion object {
        val Factory = viewModelFactory { initializer { TodayViewModel(container.intakeRepository) } }
    }
}

data class CaregiverLinkUiState(
    val loading: Boolean = true,
    val me: MeResponse? = null,
    val error: String? = null,
    val busy: Boolean = false,
    val confirmUnlink: Boolean = false,
    val message: String? = null,
)

// El paciente genera su código de invitación o desvincula a su cuidador
class CaregiverLinkViewModel(private val repo: LinkRepository) : ViewModel() {
    private val _state = MutableStateFlow(CaregiverLinkUiState())
    val state: StateFlow<CaregiverLinkUiState> = _state.asStateFlow()

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val result = repo.me()
            _state.update {
                when (result) {
                    is ApiResult.Success -> it.copy(loading = false, me = result.data)
                    is ApiResult.Error -> it.copy(loading = false, error = result.message)
                }
            }
        }
    }

    fun generateCode() {
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            val result = repo.invite()
            _state.update {
                it.copy(
                    busy = false,
                    message = when (result) {
                        is ApiResult.Success -> "Código generado: ${result.data.inviteCode}"
                        is ApiResult.Error -> result.message
                    },
                )
            }
            load()
        }
    }

    fun askUnlink() = _state.update { it.copy(confirmUnlink = true) }
    fun cancelUnlink() = _state.update { it.copy(confirmUnlink = false) }
    fun messageShown() = _state.update { it.copy(message = null) }

    fun confirmUnlink() {
        _state.update { it.copy(confirmUnlink = false, busy = true) }
        viewModelScope.launch {
            val result = repo.unlinkCaregiver()
            _state.update {
                it.copy(
                    busy = false,
                    message = when (result) {
                        is ApiResult.Success -> "Cuidador desvinculado."
                        is ApiResult.Error -> result.message
                    },
                )
            }
            load()
        }
    }

    companion object {
        val Factory = viewModelFactory { initializer { CaregiverLinkViewModel(container.linkRepository) } }
    }
}
