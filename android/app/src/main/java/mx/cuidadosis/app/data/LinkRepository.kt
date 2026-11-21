package mx.cuidadosis.app.data

import mx.cuidadosis.app.data.remote.AcceptRequest
import mx.cuidadosis.app.data.remote.AcceptResponse
import mx.cuidadosis.app.data.remote.ConfirmLinkResponse
import mx.cuidadosis.app.data.remote.ApiClient
import mx.cuidadosis.app.data.remote.ApiResult
import mx.cuidadosis.app.data.remote.InviteDto
import mx.cuidadosis.app.data.remote.MeResponse
import mx.cuidadosis.app.data.remote.apiCall

// Vinculación paciente y cuidador: código de 10 min, solicitud del cuidador y aceptación del paciente
class LinkRepository(private val api: ApiClient) {

    suspend fun me(): ApiResult<MeResponse> = apiCall { api.service().me() }

    suspend fun invite(): ApiResult<InviteDto> = apiCall { api.service().invite() }

    suspend fun accept(code: String): ApiResult<AcceptResponse> =
        when (val r = apiCall { api.service().accept(AcceptRequest(normalizeInviteCode(code))) }) {
            is ApiResult.Success -> r
            is ApiResult.Error -> r.copy(message = linkErrorMessage(r.code, r.message))
        }

    // El paciente acepta o rechaza la solicitud del cuidador (RF-08)
    suspend fun confirm(linkId: Int): ApiResult<ConfirmLinkResponse> = apiCall { api.service().confirmLink(linkId) }

    suspend fun reject(linkId: Int): ApiResult<Unit> = unit(apiCall { api.service().rejectLink(linkId) })

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

// Mensajes claros para el cuidador ante código vencido o demasiados intentos (G-01, G-05)
fun linkErrorMessage(code: Int?, serverMessage: String): String = when (code) {
    410 -> "El código venció (dura 10 minutos). Pide al paciente que genere uno nuevo."
    429 -> "Demasiados intentos con códigos incorrectos. Espera 10 minutos y vuelve a intentarlo."
    404 -> "El código no existe o ya se usó. Revisa que esté bien escrito."
    else -> serverMessage
}
