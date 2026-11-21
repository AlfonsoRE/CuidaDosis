package mx.cuidadosis.app.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mx.cuidadosis.app.data.local.CachedIntake
import mx.cuidadosis.app.data.local.ConfirmedIntake
import mx.cuidadosis.app.data.local.LocalConfirmation
import mx.cuidadosis.app.data.local.PatientStore
import mx.cuidadosis.app.data.local.SessionStore
import mx.cuidadosis.app.data.remote.ApiClient
import mx.cuidadosis.app.data.remote.ApiResult
import mx.cuidadosis.app.data.remote.CaregiverPatientsResponse
import mx.cuidadosis.app.data.remote.SyncConfirmationDto
import mx.cuidadosis.app.data.remote.SyncRequest
import mx.cuidadosis.app.data.remote.TodayResponse
import mx.cuidadosis.app.data.remote.apiCall
import mx.cuidadosis.app.reminders.ReminderScheduler
import java.util.UUID

/**
 * Tomas del paciente con modo sin red (RF-13, ADR-003). "Tomado" se guarda primero en la cola
 * local cifrada con un UUID y luego se envía con POST /intakes/sync; si no hay red se reintenta.
 * "Tomado" solo registra el toque (Confirmada por el paciente), no la ingesta.
 */
class IntakeRepository(
    private val api: ApiClient,
    private val sessionStore: SessionStore,
    private val patientStore: PatientStore,
    private val reminders: ReminderScheduler,
) {
    private val syncMutex = Mutex()
    private var retryJob: Job? = null

    // Confirma con un toque, sin red. Devuelve false si no hay sesión de paciente.
    fun confirm(scheduleId: Int, scheduledFor: String): Boolean {
        val session = sessionStore.current?.takeIf { it.isPatient } ?: return false
        val confirmation = LocalConfirmation(
            clientUuid = UUID.randomUUID().toString(),
            scheduleId = scheduleId,
            scheduledFor = scheduledFor,
            confirmedAt = formatApiDateTimeValue(nowMexico()),
        )
        patientStore.update(session.userId) { enqueueConfirmation(it, confirmation) }
        parseApiDateTime(scheduledFor)?.let { reminders.onConfirmed(it) }
        return true
    }

    /**
     * Envía la cola (aunque esté vacía: así el servidor registra la última sincronización, RF-12).
     * Error con código null = sin red; con 5xx también conviene reintentar.
     */
    suspend fun syncQueue(): ApiResult<Unit> = syncMutex.withLock {
        val session = sessionStore.current?.takeIf { it.isPatient } ?: return ApiResult.Success(Unit)
        val queue = patientStore.current?.takeIf { it.userId == session.userId }?.queue.orEmpty().take(MAX_BATCH)
        val body = SyncRequest(queue.map { SyncConfirmationDto(it.clientUuid, it.scheduleId, it.scheduledFor, it.confirmedAt) })
        when (val r = apiCall { api.service().syncIntakes(body) }) {
            is ApiResult.Success -> {
                val results = r.data.results.associate { it.clientUuid to it.status }
                r.data.results.filter { it.status == "rejected" }
                    .forEach { Log.w(TAG, "Confirmación rechazada por el servidor: ${it.error}") }
                patientStore.update(session.userId) { applySyncResults(it, results, r.data.lastSyncAt) }
                reminders.reschedule()
                ApiResult.Success(Unit)
            }
            is ApiResult.Error -> r
        }
    }

    // Sincroniza y descarga la lista de hoy del servidor para la copia local
    suspend fun refreshToday(): ApiResult<TodayResponse> {
        val sync = syncQueue()
        if (sync is ApiResult.Error && shouldRetrySync(sync.code)) return sync
        val r = apiCall { api.service().todayIntakes() }
        if (r is ApiResult.Success) saveToday(r.data)
        return r
    }

    fun hasPendingSync(): Boolean = patientStore.current?.queue?.isNotEmpty() == true

    /**
     * Reintentos automáticos mientras la app está viva: al recuperar la red y cada cierto tiempo
     * (30 s, 1, 2 y hasta 5 min) mientras haya confirmaciones sin enviar.
     */
    fun startAutoSync(context: Context, scope: CoroutineScope) {
        val connectivity = context.getSystemService(ConnectivityManager::class.java)
        connectivity.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                scope.launch { if (hasPendingSync()) syncQueue() }
            }
        })
        scope.launch {
            patientStore.data.collect { data ->
                if (data?.queue?.isNotEmpty() == true && retryJob?.isActive != true) {
                    retryJob = scope.launch { retryLoop() }
                }
            }
        }
    }

    private suspend fun retryLoop() {
        var wait = 30_000L
        while (hasPendingSync()) {
            val r = syncQueue()
            if (r is ApiResult.Success && !hasPendingSync()) return
            if (r is ApiResult.Error && !shouldRetrySync(r.code)) return
            delay(wait)
            wait = (wait * 2).coerceAtMost(300_000L)
        }
    }

    suspend fun caregiverPatients(): ApiResult<CaregiverPatientsResponse> =
        apiCall { api.service().caregiverPatients() }

    suspend fun caregiverPatientToday(patientId: Int): ApiResult<TodayResponse> =
        apiCall { api.service().caregiverPatientToday(patientId) }

    private fun saveToday(today: TodayResponse) {
        val session = sessionStore.current?.takeIf { it.isPatient } ?: return
        val cached = today.intakes.filter { it.scheduleId != null }.map {
            CachedIntake(it.scheduleId!!, it.medicationId, it.name, it.dose.orEmpty(), it.time, it.scheduledFor, it.confirmedAt)
        }
        val confirmed = cached.filter { it.confirmedAt != null }
            .map { ConfirmedIntake(it.scheduleId, it.scheduledFor, it.confirmedAt!!) }
        patientStore.saveServerConfirmed(session.userId, setOf(today.date), confirmed)
        patientStore.update(session.userId) { it.copy(todayDate = today.date, todayIntakes = cached) }
        cached.mapNotNull { parseApiDateTime(it.scheduledFor) }.distinct().forEach { reminders.onConfirmed(it) }
        reminders.reschedule()
    }

    private companion object {
        const val TAG = "CuidaDosis"
        const val MAX_BATCH = 100
    }
}
