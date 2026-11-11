package mx.cuidadosis.app.data

import mx.cuidadosis.app.data.local.Session
import mx.cuidadosis.app.data.local.SessionStore
import mx.cuidadosis.app.data.remote.ApiClient
import mx.cuidadosis.app.data.remote.ApiResult
import mx.cuidadosis.app.data.remote.AuthResponse
import mx.cuidadosis.app.data.remote.HealthResponse
import mx.cuidadosis.app.data.remote.LoginRequest
import mx.cuidadosis.app.data.remote.RegisterRequest
import mx.cuidadosis.app.data.remote.apiCall

class AuthRepository(
    private val api: ApiClient,
    private val sessionStore: SessionStore,
) {
    suspend fun login(email: String, password: String): ApiResult<Session> =
        saveSession(apiCall { api.service().login(LoginRequest(email.trim(), password)) })

    suspend fun register(fullName: String, email: String, password: String, role: String): ApiResult<Session> =
        saveSession(
            apiCall {
                api.service().register(RegisterRequest(fullName.trim(), email.trim(), password, role))
            },
        )

    // Cierra la sesión en el servidor (si se puede) y siempre la borra del teléfono
    suspend fun logout() {
        if (sessionStore.current != null) {
            apiCall { api.service().logout() }
        }
        sessionStore.clear()
    }

    suspend fun checkServer(baseUrl: String): ApiResult<HealthResponse> =
        apiCall { api.service().health(baseUrl) }

    private fun saveSession(result: ApiResult<AuthResponse>): ApiResult<Session> = when (result) {
        is ApiResult.Error -> result
        is ApiResult.Success -> {
            val r = result.data
            val session = Session(
                token = r.token,
                expiresAt = r.expiresAt,
                userId = r.user.id,
                fullName = r.user.fullName,
                email = r.user.email,
                role = r.user.role,
            )
            sessionStore.save(session)
            ApiResult.Success(session)
        }
    }
}
