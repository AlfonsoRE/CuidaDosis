package mx.cuidadosis.app.data.local

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import mx.cuidadosis.app.data.remote.ApiJson
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Serializable
data class Session(
    val token: String,
    val expiresAt: String,
    val userId: Int,
    val fullName: String,
    val email: String,
    val role: String,
) {
    val isPatient: Boolean get() = role == ROLE_PATIENT

    // expires_at viene de la API en hora local de America/Mexico_City
    fun isExpired(): Boolean = runCatching {
        val expires = LocalDateTime.parse(expiresAt, API_DATE_FORMAT).atZone(API_ZONE)
        !expires.toInstant().isAfter(java.time.Instant.now())
    }.getOrDefault(true)

    companion object {
        const val ROLE_PATIENT = "patient"
        const val ROLE_CAREGIVER = "caregiver"
        private val API_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        private val API_ZONE: ZoneId = ZoneId.of("America/Mexico_City")
    }
}

/**
 * Guarda la sesión cifrada con AES-GCM (ver LocalCipher). En SharedPreferences solo queda
 * el texto cifrado (excluido de respaldos).
 */
class SessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("session", Context.MODE_PRIVATE)

    private val _session = MutableStateFlow(load())
    val session: StateFlow<Session?> = _session.asStateFlow()

    val current: Session? get() = _session.value

    fun save(session: Session) {
        val plain = ApiJson.encodeToString(Session.serializer(), session)
        prefs.edit { putString(KEY_DATA, LocalCipher.encrypt(plain)) }
        _session.value = session
    }

    fun clear() {
        prefs.edit { remove(KEY_DATA) }
        _session.value = null
    }

    private fun load(): Session? {
        val stored = prefs.getString(KEY_DATA, null) ?: return null
        val session = runCatching {
            ApiJson.decodeFromString(Session.serializer(), LocalCipher.decrypt(stored))
        }.getOrNull()
        if (session == null || session.isExpired()) {
            prefs.edit { remove(KEY_DATA) }
            return null
        }
        return session
    }

    private companion object {
        const val KEY_DATA = "data"
    }
}
