package mx.cuidadosis.app.data.local

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

// URL base de la API elegida por el usuario (no es un dato secreto)
class SettingsStore(context: Context, val defaultBaseUrl: String) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _baseUrl = MutableStateFlow(prefs.getString(KEY_BASE_URL, null) ?: defaultBaseUrl)
    val baseUrl: StateFlow<String> = _baseUrl.asStateFlow()

    fun setBaseUrl(url: String) {
        prefs.edit { putString(KEY_BASE_URL, url) }
        _baseUrl.value = url
    }

    fun reset() {
        prefs.edit { remove(KEY_BASE_URL) }
        _baseUrl.value = defaultBaseUrl
    }

    private companion object {
        const val KEY_BASE_URL = "api_base_url"
    }
}

object BaseUrl {
    /**
     * Normaliza la URL escrita por el usuario: http o https, sin query, terminada en "/".
     * Devuelve null si no es válida.
     */
    fun normalize(input: String): String? {
        val trimmed = input.trim()
        if (!trimmed.startsWith("http://", ignoreCase = true) &&
            !trimmed.startsWith("https://", ignoreCase = true)
        ) {
            return null
        }
        val withSlash = if (trimmed.endsWith("/")) trimmed else "$trimmed/"
        val url = withSlash.toHttpUrlOrNull() ?: return null
        if (url.query != null || url.fragment != null) return null
        return url.toString()
    }
}
