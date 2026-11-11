package mx.cuidadosis.app.data

import mx.cuidadosis.app.data.remote.AcceptRequest
import mx.cuidadosis.app.data.remote.AcceptResponse
import mx.cuidadosis.app.data.remote.ApiClient
import mx.cuidadosis.app.data.remote.ApiResult
import mx.cuidadosis.app.data.remote.InviteDto
import mx.cuidadosis.app.data.remote.MeResponse
import mx.cuidadosis.app.data.remote.apiCall

// Vinculación paciente y cuidador por código de invitación
class LinkRepository(private val api: ApiClient) {

    suspend fun me(): ApiResult<MeResponse> = apiCall { api.service().me() }

    suspend fun invite(): ApiResult<InviteDto> = apiCall { api.service().invite() }

    suspend fun accept(code: String): ApiResult<AcceptResponse> =
        apiCall { api.service().accept(AcceptRequest(normalizeInviteCode(code))) }

    suspend fun unlinkCaregiver(): ApiResult<Unit> = unit(apiCall { api.service().unlinkCaregiver() })

    suspend fun unlinkPatient(patientId: Int): ApiResult<Unit> =
        unit(apiCall { api.service().unlinkPatient(patientId) })

    private fun <T> unit(r: ApiResult<T>): ApiResult<Unit> = when (r) {
        is ApiResult.Success -> ApiResult.Success(Unit)
        is ApiResult.Error -> r
    }
}

// El código se dicta o se copia: se ignoran espacios y guiones, y se pasa a mayúsculas
fun normalizeInviteCode(input: String): String =
    input.filter { it.isLetterOrDigit() }.uppercase()
