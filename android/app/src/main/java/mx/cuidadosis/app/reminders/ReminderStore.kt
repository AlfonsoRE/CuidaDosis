package mx.cuidadosis.app.reminders

import android.content.Context
import androidx.core.content.edit
import kotlinx.serialization.Serializable
import mx.cuidadosis.app.data.local.LocalCipher
import mx.cuidadosis.app.data.remote.ApiJson
import java.time.LocalDateTime

@Serializable
data class ReminderData(val userId: Int, val meds: List<ReminderMed>)

/**
 * Copia local de los medicamentos activos del paciente para reprogramar avisos sin red
 * (por ejemplo, al reiniciar el teléfono). Va cifrada y excluida de respaldos.
 */
class ReminderStore(context: Context) {
    private val prefs = context.getSharedPreferences("reminders", Context.MODE_PRIVATE)

    fun load(): ReminderData? {
        val stored = prefs.getString(KEY_DATA, null) ?: return null
        return runCatching {
            ApiJson.decodeFromString(ReminderData.serializer(), LocalCipher.decrypt(stored))
        }.getOrNull()
    }

    fun save(data: ReminderData) {
        val previous = load()
        prefs.edit {
            putString(KEY_DATA, LocalCipher.encrypt(ApiJson.encodeToString(ReminderData.serializer(), data)))
            // Otro usuario en el mismo teléfono empieza sin historial de avisos
            if (previous != null && previous.userId != data.userId) remove(KEY_NOTIFIED)
        }
    }

    // Tomas ya avisadas (hora de México); evita repetir un aviso al reprogramar
    fun notified(): Set<LocalDateTime> =
        prefs.getStringSet(KEY_NOTIFIED, emptySet()).orEmpty()
            .mapNotNull { runCatching { LocalDateTime.parse(it) }.getOrNull() }
            .toSet()

    // Agrega la toma y descarta las que ya pasaron (nunca vuelven a ser candidatas)
    fun addNotified(intakeAt: LocalDateTime, nowMexico: LocalDateTime) {
        val kept = (notified() + intakeAt).filter { it.isAfter(nowMexico) || it == intakeAt }
        prefs.edit { putStringSet(KEY_NOTIFIED, kept.map { it.toString() }.toSet()) }
    }

    fun clear() = prefs.edit { clear() }

    private companion object {
        const val KEY_DATA = "data"
        const val KEY_NOTIFIED = "notified"
    }
}
