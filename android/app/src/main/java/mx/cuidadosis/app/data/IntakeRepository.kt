package mx.cuidadosis.app.data

import mx.cuidadosis.app.data.remote.ApiClient
import mx.cuidadosis.app.data.remote.ApiResult
import mx.cuidadosis.app.data.remote.CaregiverPatientsResponse
import mx.cuidadosis.app.data.remote.IntakeDto
import mx.cuidadosis.app.data.remote.TodayResponse
import mx.cuidadosis.app.data.remote.apiCall

// Tomas del día. "Tomado" solo registra el toque del usuario, no la ingesta real.
class IntakeRepository(private val api: ApiClient) {

    suspend fun today(): ApiResult<TodayResponse> = apiCall { api.service().todayIntakes() }

    suspend fun take(intakeId: Int): ApiResult<IntakeDto> =
        when (val r = apiCall { api.service().takeIntake(intakeId) }) {
            is ApiResult.Success -> ApiResult.Success(r.data.intake)
            is ApiResult.Error -> r
        }

    suspend fun caregiverPatients(): ApiResult<CaregiverPatientsResponse> =
        apiCall { api.service().caregiverPatients() }

    suspend fun caregiverPatientToday(patientId: Int): ApiResult<TodayResponse> =
        apiCall { api.service().caregiverPatientToday(patientId) }
}
