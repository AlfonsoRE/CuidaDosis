package mx.cuidadosis.app.data

import mx.cuidadosis.app.data.local.SessionStore
import mx.cuidadosis.app.data.remote.ApiClient
import mx.cuidadosis.app.data.remote.ApiResult
import mx.cuidadosis.app.data.remote.MedicationDto
import mx.cuidadosis.app.data.remote.MedicationRequest
import mx.cuidadosis.app.data.remote.apiCall
import mx.cuidadosis.app.reminders.ReminderScheduler

// Medicamentos del paciente. Nombre y dosis son texto libre: la app no los interpreta.
// Cada lista obtenida actualiza los avisos locales (Fase 5).
class MedicationRepository(
    private val api: ApiClient,
    private val sessionStore: SessionStore,
    private val reminders: ReminderScheduler,
) {

    suspend fun list(): ApiResult<List<MedicationDto>> =
        when (val r = apiCall { api.service().medications() }) {
            is ApiResult.Success -> {
                val session = sessionStore.current
                if (session != null && session.isPatient) {
                    reminders.sync(session.userId, r.data.medications)
                }
                ApiResult.Success(r.data.medications)
            }
            is ApiResult.Error -> r
        }

    suspend fun get(id: Int): ApiResult<MedicationDto> =
        when (val r = apiCall { api.service().medication(id) }) {
            is ApiResult.Success -> ApiResult.Success(r.data.medication)
            is ApiResult.Error -> r
        }

    suspend fun save(id: Int?, name: String, dose: String, times: List<String>): ApiResult<MedicationDto> {
        val body = MedicationRequest(name.trim(), dose.trim(), times)
        val r = apiCall {
            if (id == null) api.service().createMedication(body) else api.service().updateMedication(id, body)
        }
        return when (r) {
            is ApiResult.Success -> {
                list() // reprograma los avisos con los horarios nuevos
                ApiResult.Success(r.data.medication)
            }
            is ApiResult.Error -> r
        }
    }

    suspend fun deactivate(id: Int): ApiResult<Unit> =
        when (val r = apiCall { api.service().deactivateMedication(id) }) {
            is ApiResult.Success -> {
                list() // deja de avisar de este medicamento
                ApiResult.Success(Unit)
            }
            is ApiResult.Error -> r
        }
}
