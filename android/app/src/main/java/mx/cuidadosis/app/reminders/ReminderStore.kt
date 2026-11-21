package mx.cuidadosis.app.reminders

import android.content.Context
import androidx.core.content.edit
import kotlinx.serialization.builtins.ListSerializer
import mx.cuidadosis.app.data.remote.ApiJson
import java.time.Instant
import java.time.LocalDateTime

/**
 * Registro de los avisos ya mostrados y de los pospuestos. No guarda nombres ni dosis
 * (esos están cifrados en PatientStore), solo fechas y horas.
 */
class ReminderStore(context: Context) {
    private val prefs = context.getSharedPreferences("reminders", Context.MODE_PRIVATE)

    init {
        // La Fase 5 guardaba aquí la copia de los medicamentos; ahora vive en PatientStore
        if (prefs.contains(LEGACY_DATA) || prefs.contains(LEGACY_NOTIFIED)) {
            prefs.edit { remove(LEGACY_DATA).remove(LEGACY_NOTIFIED) }
        }
    }

    fun fired(): Set<String> = prefs.getStringSet(KEY_FIRED, emptySet()).orEmpty()

    // Agrega el evento y descarta los de tomas de hace más de 2 días
    fun addFired(key: String, nowMexico: LocalDateTime) {
        val limit = nowMexico.minusDays(2)
        val kept = (fired() + key).filter { k ->
            runCatching { LocalDateTime.parse(k.substringBefore('|')) }.getOrNull()?.isAfter(limit) ?: false
        }
        prefs.edit { putStringSet(KEY_FIRED, kept.toSet()) }
    }

    fun snoozes(): List<Snooze> {
        val raw = prefs.getString(KEY_SNOOZES, null) ?: return emptyList()
        return runCatching { ApiJson.decodeFromString(ListSerializer(Snooze.serializer()), raw) }.getOrDefault(emptyList())
    }

    // Un pospuesto por toma: posponer otra vez reemplaza al anterior. Se descartan los vencidos.
    fun addSnooze(snooze: Snooze, now: Instant) {
        val kept = snoozes()
            .filter { it.intakeAt != snooze.intakeAt }
            .filter { it.at.plusSeconds(LATE_LIMIT_MINUTES * 60).isAfter(now) }
        prefs.edit {
            putString(KEY_SNOOZES, ApiJson.encodeToString(ListSerializer(Snooze.serializer()), kept + snooze))
        }
    }

    fun clear() = prefs.edit { clear() }

    private companion object {
        const val KEY_FIRED = "fired"
        const val KEY_SNOOZES = "snoozes"
        const val LEGACY_DATA = "data"
        const val LEGACY_NOTIFIED = "notified"
    }
}
