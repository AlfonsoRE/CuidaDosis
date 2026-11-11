package mx.cuidadosis.app.data.remote

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.Response
import java.io.IOException

// Resultado de una llamada a la API con mensajes listos para mostrar en español
sealed interface ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>

    data class Error(
        val message: String,
        val fieldErrors: Map<String, String> = emptyMap(),
        val code: Int? = null,
    ) : ApiResult<Nothing>
}

const val NETWORK_ERROR_MESSAGE =
    "No se pudo conectar con el servidor. Revisa la dirección del servidor, que Apache esté encendido " +
        "y que la app tenga permiso para acceder a dispositivos de la red local."

val ApiJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

suspend fun <T> apiCall(block: suspend () -> Response<T>): ApiResult<T> =
    try {
        val response = block()
        val body = response.body()
        if (response.isSuccessful && body != null) {
            ApiResult.Success(body)
        } else {
            parseApiError(response.code(), response.errorBody()?.string())
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        Log.w("CuidaDosis", "Error de red al llamar a la API", e)
        ApiResult.Error(NETWORK_ERROR_MESSAGE)
    } catch (e: SerializationException) {
        ApiResult.Error("El servidor respondió algo inesperado. Revisa la dirección del servidor.")
    } catch (e: IllegalArgumentException) {
        ApiResult.Error("El servidor respondió algo inesperado. Revisa la dirección del servidor.")
    }

// Convierte {"error": "...", "details": {...}} de la API en un ApiResult.Error
fun parseApiError(code: Int, body: String?): ApiResult.Error {
    val parsed = body
        ?.takeIf { it.isNotBlank() }
        ?.let { runCatching { ApiJson.decodeFromString<ErrorResponse>(it) }.getOrNull() }
    return if (parsed != null) {
        ApiResult.Error(parsed.error, parsed.details.orEmpty(), code)
    } else {
        ApiResult.Error("Error del servidor (código $code).", emptyMap(), code)
    }
}
