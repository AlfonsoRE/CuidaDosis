package mx.cuidadosis.app.ui.caregiver

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mx.cuidadosis.app.container
import mx.cuidadosis.app.data.IntakeRepository
import mx.cuidadosis.app.data.LinkRepository
import mx.cuidadosis.app.data.normalizeInviteCode
import mx.cuidadosis.app.data.remote.ApiResult
import mx.cuidadosis.app.data.remote.CaregiverPatientsResponse
import mx.cuidadosis.app.data.remote.TodayResponse
import mx.cuidadosis.app.ui.CaregiverPatientRoute

data class CaregiverHomeUiState(
    val loading: Boolean = true,
    val data: CaregiverPatientsResponse? = null,
    val error: String? = null,
    val showLinkDialog: Boolean = false,
    val code: String = "",
    val codeError: String? = null,
    val linking: Boolean = false,
    val message: String? = null,
)

// Lista de pacientes del cuidador y vinculación con código
class CaregiverHomeViewModel(
    private val intakes: IntakeRepository,
    private val links: LinkRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(CaregiverHomeUiState())
    val state: StateFlow<CaregiverHomeUiState> = _state.asStateFlow()

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val result = intakes.caregiverPatients()
            _state.update {
                when (result) {
                    is ApiResult.Success -> it.copy(loading = false, data = result.data)
                    is ApiResult.Error -> it.copy(loading = false, error = result.message)
                }
            }
        }
    }

    fun openLinkDialog() = _state.update { it.copy(showLinkDialog = true, code = "", codeError = null) }
    fun closeLinkDialog() = _state.update { it.copy(showLinkDialog = false) }
    fun onCode(v: String) = _state.update { it.copy(code = v.take(12), codeError = null) }
    fun messageShown() = _state.update { it.copy(message = null) }

    fun link() {
        val code = normalizeInviteCode(_state.value.code)
        if (code.length != 8) {
            _state.update { it.copy(codeError = "El código tiene 8 letras o números.") }
            return
        }
        _state.update { it.copy(linking = true, codeError = null) }
        viewModelScope.launch {
            when (val result = links.accept(code)) {
                is ApiResult.Success -> {
                    _state.update {
                        it.copy(
                            linking = false,
                            showLinkDialog = false,
                            message = "Ahora sigues a ${result.data.patient.fullName}.",
                        )
                    }
                    load()
                }
                is ApiResult.Error -> _state.update {
                    it.copy(linking = false, codeError = result.fieldErrors["invite_code"] ?: result.message)
                }
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { CaregiverHomeViewModel(container.intakeRepository, container.linkRepository) }
        }
    }
}

data class CaregiverPatientUiState(
    val loading: Boolean = true,
    val today: TodayResponse? = null,
    val error: String? = null,
    val notLinked: Boolean = false,
    val confirmUnlink: Boolean = false,
    val busy: Boolean = false,
    val unlinked: Boolean = false,
)

// Tomas de hoy de un paciente (solo lectura) y opción de dejar de seguirlo
class CaregiverPatientViewModel(
    savedStateHandle: SavedStateHandle,
    private val intakes: IntakeRepository,
    private val links: LinkRepository,
) : ViewModel() {
    private val patientId = savedStateHandle.toRoute<CaregiverPatientRoute>().id

    private val _state = MutableStateFlow(CaregiverPatientUiState())
    val state: StateFlow<CaregiverPatientUiState> = _state.asStateFlow()

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val result = intakes.caregiverPatientToday(patientId)
            _state.update {
                when (result) {
                    is ApiResult.Success -> it.copy(loading = false, today = result.data, notLinked = false)
                    // 404: el paciente lo desvinculó mientras tanto
                    is ApiResult.Error -> it.copy(
                        loading = false,
                        error = result.message,
                        notLinked = result.code == 404,
                        today = if (result.code == 404) null else it.today,
                    )
                }
            }
        }
    }

    fun askUnlink() = _state.update { it.copy(confirmUnlink = true) }
    fun cancelUnlink() = _state.update { it.copy(confirmUnlink = false) }

    fun confirmUnlink() {
        _state.update { it.copy(confirmUnlink = false, busy = true) }
        viewModelScope.launch {
            val result = links.unlinkPatient(patientId)
            _state.update {
                when (result) {
                    is ApiResult.Success -> it.copy(busy = false, unlinked = true)
                    is ApiResult.Error -> it.copy(busy = false, error = result.message)
                }
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                CaregiverPatientViewModel(
                    createSavedStateHandle(),
                    container.intakeRepository,
                    container.linkRepository,
                )
            }
        }
    }
}
