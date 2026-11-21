package mx.cuidadosis.app.ui.patient

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mx.cuidadosis.app.data.TodayItem
import mx.cuidadosis.app.data.buildTodayList
import mx.cuidadosis.app.data.formatApiDateTimeValue
import mx.cuidadosis.app.data.nowMexico
import mx.cuidadosis.app.data.shouldRetrySync
import mx.cuidadosis.app.data.local.PatientData
import mx.cuidadosis.app.data.local.PatientStore
import java.time.LocalDate
import java.time.LocalDateTime
import mx.cuidadosis.app.container
import mx.cuidadosis.app.data.IntakeRepository
import mx.cuidadosis.app.data.LinkRepository
import mx.cuidadosis.app.data.remote.ApiResult
import mx.cuidadosis.app.data.remote.MeResponse

data class TodayUiState(
    val loading: Boolean = false,
    val items: List<TodayItem> = emptyList(),
    val date: LocalDate = nowMexico().toLocalDate(),
    val now: LocalDateTime = nowMexico(),
    // Sin conexión con el servidor en el último intento (la pantalla sigue funcionando)
    val offline: Boolean = false,
    val error: String? = null,
    val lastSyncAt: String? = null,
    val waitingSync: Int = 0,
    val message: String? = null,
)

/**
 * Tomas de hoy del paciente desde la copia local (funciona sin red, RF-13) y botón "Tomado"
 * (un toque, sin diálogo; RF-07). Al actualizar se envía la cola y se descarga la lista del servidor.
 */
class TodayViewModel(
    private val repo: IntakeRepository,
    private val store: PatientStore,
) : ViewModel() {
    private val _state = MutableStateFlow(TodayUiState())
    val state: StateFlow<TodayUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch { store.data.collect { rebuild(it) } }
        // Cada minuto: una toma pendiente pasa a "Sin confirmar" sin tener que recargar
        viewModelScope.launch {
            while (true) {
                delay(60_000)
                rebuild(store.current)
            }
        }
    }

    private fun rebuild(data: PatientData?) {
        val now = nowMexico()
        _state.update {
            it.copy(
                items = data?.let { d -> buildTodayList(d, now.toLocalDate()) }.orEmpty(),
                date = now.toLocalDate(),
                now = now,
                lastSyncAt = data?.lastSyncAt,
                waitingSync = data?.queue?.size ?: 0,
            )
        }
    }

    fun load() {
        _state.update { it.copy(loading = true) }
        viewModelScope.launch {
            val result = repo.refreshToday()
            _state.update {
                when (result) {
                    is ApiResult.Success -> it.copy(loading = false, offline = false, error = null)
                    is ApiResult.Error -> if (shouldRetrySync(result.code)) {
                        it.copy(loading = false, offline = true, error = null)
                    } else {
                        it.copy(loading = false, offline = false, error = result.message)
                    }
                }
            }
        }
    }

    fun messageShown() = _state.update { it.copy(message = null) }

    fun take(item: TodayItem) {
        if (item.confirmed) return
        repo.confirm(item.scheduleId, formatApiDateTimeValue(item.scheduledFor))
        _state.update { it.copy(message = "Confirmada por el paciente: ${item.name}, ${item.time}.") }
        // Se intenta enviar de inmediato; si no hay red queda en la cola y se reintenta
        viewModelScope.launch {
            val r = repo.syncQueue()
            _state.update { it.copy(offline = r is ApiResult.Error && shouldRetrySync(r.code)) }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { TodayViewModel(container.intakeRepository, container.patientStore) }
        }
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

    // Aceptar o rechazar la solicitud del cuidador (RF-08; ADR-004)
    fun acceptRequest(linkId: Int) = respond("Ahora tu cuidador puede ver tus tomas.") { repo.confirm(linkId) }
    fun rejectRequest(linkId: Int) = respond("Solicitud rechazada.") { repo.reject(linkId) }

    private fun respond(ok: String, call: suspend () -> ApiResult<*>) {
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            val result = call()
            _state.update {
                it.copy(
                    busy = false,
                    message = when (result) {
                        is ApiResult.Success -> ok
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
