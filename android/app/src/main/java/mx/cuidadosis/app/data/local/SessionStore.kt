package mx.cuidadosis.app.data.local

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import mx.cuidadosis.app.data.remote.ApiJson
import java.security.KeyStore
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

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
 * Guarda la sesión cifrada con AES-GCM. La clave vive en Android Keystore y no sale del
 * dispositivo; en SharedPreferences solo queda el texto cifrado (excluido de respaldos).
 */
class SessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("session", Context.MODE_PRIVATE)

    private val _session = MutableStateFlow(load())
    val session: StateFlow<Session?> = _session.asStateFlow()

    val current: Session? get() = _session.value

    fun save(session: Session) {
        val plain = ApiJson.encodeToString(Session.serializer(), session).toByteArray(Charsets.UTF_8)
        prefs.edit { putString(KEY_DATA, encrypt(plain)) }
        _session.value = session
    }

    fun clear() {
        prefs.edit { remove(KEY_DATA) }
        _session.value = null
    }

    private fun load(): Session? {
        val stored = prefs.getString(KEY_DATA, null) ?: return null
        val session = runCatching {
            ApiJson.decodeFromString(Session.serializer(), String(decrypt(stored), Charsets.UTF_8))
        }.getOrNull()
        if (session == null || session.isExpired()) {
            prefs.edit { remove(KEY_DATA) }
            return null
        }
        return session
    }

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    // Formato guardado: Base64(IV de 12 bytes + texto cifrado con etiqueta GCM)
    private fun encrypt(plain: ByteArray): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val output = cipher.iv + cipher.doFinal(plain)
        return Base64.encodeToString(output, Base64.NO_WRAP)
    }

    private fun decrypt(stored: String): ByteArray {
        val bytes = Base64.decode(stored, Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            key(),
            GCMParameterSpec(128, bytes, 0, IV_SIZE),
        )
        return cipher.doFinal(bytes, IV_SIZE, bytes.size - IV_SIZE)
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "cuidadosis_session"
        const val KEY_DATA = "data"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_SIZE = 12
    }
}
