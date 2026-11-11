package mx.cuidadosis.app.ui.medications

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
import mx.cuidadosis.app.data.MedicationRepository
import mx.cuidadosis.app.data.remote.ApiResult
import mx.cuidadosis.app.data.remote.MedicationDto
import mx.cuidadosis.app.ui.MedicationFormRoute

data class MedicationListUiState(
    val loading: Boolean = true,
    val medications: List<MedicationDto> = emptyList(),
    val error: String? = null,
    val pendingDeactivate: MedicationDto? = null,
    val message: String? = null,
)

class MedicationListViewModel(private val repo: MedicationRepository) : ViewModel() {
    private val _state = MutableStateFlow(MedicationListUiState())
    val state: StateFlow<MedicationListUiState> = _state.asStateFlow()

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val result = repo.list()
            _state.update {
                when (result) {
                    is ApiResult.Success -> it.copy(loading = false, medications = result.data)
                    is ApiResult.Error -> it.copy(loading = false, error = result.message)
                }
            }
        }
    }

    fun askDeactivate(med: MedicationDto) = _state.update { it.copy(pendingDeactivate = med) }
    fun cancelDeactivate() = _state.update { it.copy(pendingDeactivate = null) }
    fun messageShown() = _state.update { it.copy(message = null) }

    fun confirmDeactivate() {
        val med = _state.value.pendingDeactivate ?: return
        _state.update { it.copy(pendingDeactivate = null, loading = true) }
        viewModelScope.launch {
            when (val result = repo.deactivate(med.id)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(message = "Se desactivó ${med.name}.") }
                    load()
                }
                is ApiResult.Error -> _state.update { it.copy(loading = false, message = result.message) }
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { MedicationListViewModel(container.medicationRepository) }
        }
    }
}

data class MedicationFormUiState(
    val isEdit: Boolean,
    val loading: Boolean = false,
    val saving: Boolean = false,
    val name: String = "",
    val dose: String = "",
    val times: List<String> = emptyList(),
    val fieldErrors: Map<String, String> = emptyMap(),
    val error: String? = null,
    val saved: Boolean = false,
)

// Alta (id null) o edición de un medicamento. PUT envía la lista completa de horas.
class MedicationFormViewModel(
    savedStateHandle: SavedStateHandle,
    private val repo: MedicationRepository,
) : ViewModel() {
    private val medicationId: Int? = savedStateHandle.toRoute<MedicationFormRoute>().id

    private val _state = MutableStateFlow(MedicationFormUiState(isEdit = medicationId != null))
    val state: StateFlow<MedicationFormUiState> = _state.asStateFlow()

    init {
        if (medicationId != null) load(medicationId)
    }

    private fun load(id: Int) {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val result = repo.get(id)
            _state.update {
                when (result) {
                    is ApiResult.Success -> it.copy(
                        loading = false,
                        name = result.data.name,
                        dose = result.data.dose,
                        times = result.data.times.sorted(),
                    )
                    is ApiResult.Error -> it.copy(loading = false, error = result.message)
                }
            }
        }
    }

    fun onName(v: String) = _state.update { it.copy(name = v, fieldErrors = it.fieldErrors - "name") }
    fun onDose(v: String) = _state.update { it.copy(dose = v, fieldErrors = it.fieldErrors - "dose") }

    fun addTime(hour: Int, minute: Int) {
        val time = formatTime(hour, minute)
        _state.update {
            when {
                time in it.times -> it.copy(fieldErrors = it.fieldErrors + ("times" to "La hora $time ya está en la lista."))
                it.times.size >= MAX_TIMES -> it.copy(fieldErrors = it.fieldErrors + ("times" to "Máximo $MAX_TIMES horas."))
                else -> it.copy(times = (it.times + time).sorted(), fieldErrors = it.fieldErrors - "times")
            }
        }
    }

    fun removeTime(time: String) = _state.update { it.copy(times = it.times - time) }

    fun save() {
        val s = _state.value
        val errors = validateMedication(s.name, s.dose, s.times)
        if (errors.isNotEmpty()) {
            _state.update { it.copy(fieldErrors = errors, error = null) }
            return
        }
        _state.update { it.copy(saving = true, error = null, fieldErrors = emptyMap()) }
        viewModelScope.launch {
            val result = repo.save(medicationId, s.name, s.dose, s.times)
            _state.update {
                when (result) {
                    is ApiResult.Success -> it.copy(saving = false, saved = true)
                    is ApiResult.Error -> it.copy(
                        saving = false,
                        fieldErrors = result.fieldErrors,
                        error = if (result.fieldErrors.isEmpty()) result.message else null,
                    )
                }
            }
        }
    }

    companion object {
        const val MAX_TIMES = 10
        val Factory = viewModelFactory {
            initializer { MedicationFormViewModel(createSavedStateHandle(), container.medicationRepository) }
        }
    }
}

fun formatTime(hour: Int, minute: Int): String = "%02d:%02d".format(hour, minute)

// Mismas reglas que la API: solo longitud y cantidad de horas, sin interpretar el contenido
fun validateMedication(name: String, dose: String, times: List<String>): Map<String, String> = buildMap {
    if (name.trim().length !in 1..100) put("name", "Escribe el nombre (máximo 100 caracteres).")
    if (dose.trim().length !in 1..100) put("dose", "Escribe la dosis indicada (máximo 100 caracteres).")
    if (times.isEmpty()) put("times", "Agrega al menos una hora.")
    if (times.size > MedicationFormViewModel.MAX_TIMES) put("times", "Máximo ${MedicationFormViewModel.MAX_TIMES} horas.")
}
