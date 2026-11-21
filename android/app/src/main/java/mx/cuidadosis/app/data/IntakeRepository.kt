package mx.cuidadosis.app.data

import mx.cuidadosis.app.data.local.ConfirmedIntake
import mx.cuidadosis.app.data.local.PatientStore
import mx.cuidadosis.app.data.local.SessionStore
import mx.cuidadosis.app.data.remote.ApiClient
import mx.cuidadosis.app.data.remote.ApiResult
import mx.cuidadosis.app.data.remote.CaregiverPatientsResponse
import mx.cuidadosis.app.data.remote.IntakeDto
import mx.cuidadosis.app.data.remote.TodayResponse
import mx.cuidadosis.app.data.remote.apiCall
import mx.cuidadosis.app.reminders.ReminderScheduler

// Tomas del día. "Tomado" solo registra el toque del usuario (Confirmada por el paciente), no la ingesta.
class IntakeRepository(
    private val api: ApiClient,
    private val sessionStore: SessionStore,
    private val patientStore: PatientStore,
    private val reminders: ReminderScheduler,
) {

    suspend fun today(): ApiResult<TodayResponse> {
        val r = apiCall { api.service().todayIntakes() }
        if (r is ApiResult.Success) rememberConfirmed(r.data.date, r.data.intakes)
        return r
    }

    suspend fun take(intake: IntakeDto): ApiResult<IntakeDto> =
        when (val r = apiCall { api.service().takeIntake(intake.id) }) {
            is ApiResult.Success -> {
                rememberConfirmed(null, listOf(r.data.intake))
                ApiResult.Success(r.data.intake)
            }
            is ApiResult.Error -> r
        }

    suspend fun caregiverPatients(): ApiResult<CaregiverPatientsResponse> =
        apiCall { api.service().caregiverPatients() }

    suspend fun caregiverPatientToday(patientId: Int): ApiResult<TodayResponse> =
        apiCall { api.service().caregiverPatientToday(patientId) }

    /**
     * Guarda en la copia local qué tomas confirmó el servidor, para no repetir avisos de tomas
     * ya confirmadas. Con [date] reemplaza las de ese día; sin él solo agrega.
     */
    private fun rememberConfirmed(date: String?, intakes: List<IntakeDto>) {
        val session = sessionStore.current ?: return
        if (!session.isPatient) return
        val confirmed = intakes.filter { it.isTaken && it.scheduleId != null }.map {
            ConfirmedIntake(it.scheduleId!!, it.scheduledFor, it.confirmedAt.orEmpty())
        }
        if (date != null) {
            patientStore.saveServerConfirmed(session.userId, setOf(date), confirmed)
        } else {
            patientStore.update(session.userId) { d ->
                d.copy(serverConfirmed = d.serverConfirmed + confirmed.filter { it !in d.serverConfirmed })
            }
        }
        intakes.mapNotNull { parseApiDateTime(it.scheduledFor) }.distinct().forEach { reminders.onConfirmed(it) }
        if (intakes.isEmpty()) reminders.reschedule()
    }
}
