package mx.cuidadosis.app.data.local

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import mx.cuidadosis.app.data.nowMexico
import mx.cuidadosis.app.data.parseApiDateTime
import mx.cuidadosis.app.data.remote.ApiJson
import mx.cuidadosis.app.reminders.ReminderMed
import java.time.LocalDateTime

@Serializable
data class LocalSchedule(val id: Int, val time: String)

// Nombre y dosis tal como los escribió el usuario; la app no los interpreta
@Serializable
data class LocalMedication(
    val id: Int,
    val name: String,
    val dose: String,
    val schedules: List<LocalSchedule>,
)

// Toma confirmada según el servidor (hora programada y hora del toque, formato de la API)
@Serializable
data class ConfirmedIntake(val scheduleId: Int, val scheduledFor: String, val confirmedAt: String)

// Confirmación hecha en el teléfono que espera envío (RF-13, ADR-003). El UUID la hace idempotente.
@Serializable
data class LocalConfirmation(
    val clientUuid: String,
    val scheduleId: Int,
    val scheduledFor: String,
    val confirmedAt: String,
)

// Copia de una toma de hoy tal como la mandó el servidor, para mostrar "Hoy" sin red
@Serializable
data class CachedIntake(
    val scheduleId: Int,
    val medicationId: Int,
    val name: String,
    val dose: String,
    val time: String,
    val scheduledFor: String,
    val confirmedAt: String? = null,
)

@Serializable
data class PatientData(
    val userId: Int,
    val medications: List<LocalMedication> = emptyList(),
    val serverConfirmed: List<ConfirmedIntake> = emptyList(),
    val queue: List<LocalConfirmation> = emptyList(),
    // Última lista de "Hoy" recibida del servidor y su fecha (yyyy-MM-dd)
    val todayDate: String? = null,
    val todayIntakes: List<CachedIntake> = emptyList(),
    // Hora del servidor en la última sincronización exitosa
    val lastSyncAt: String? = null,
) {
    fun reminderMeds(): List<ReminderMed> = medications.map { med ->
        ReminderMed(
            id = med.id,
            name = med.name,
            dose = med.dose,
            times = med.schedules.map { it.time },
            scheduleIds = med.schedules.associate { it.time to it.id },
        )
    }

    // Pares (horario, hora programada) ya confirmados, en el servidor o en la cola local
    fun confirmedSet(): Set<Pair<Int, LocalDateTime>> =
        (serverConfirmed.map { it.scheduleId to it.scheduledFor } + queue.map { it.scheduleId to it.scheduledFor })
            .mapNotNull { (id, at) -> parseApiDateTime(at)?.let { id to it } }
            .toSet()
}

/**
 * Copia local de los datos del paciente para que los avisos y la pantalla "Hoy" funcionen sin red
 * (ADR-003). Un solo archivo de preferencias con el JSON cifrado (AES-GCM, clave en Android
 * Keystore) y excluido de respaldos. Se borra al cerrar sesión o al cambiar de usuario.
 */
class PatientStore(context: Context) {
    private val prefs = context.getSharedPreferences("patient_local", Context.MODE_PRIVATE)
    private val _data = MutableStateFlow(read())
    val data: StateFlow<PatientData?> = _data.asStateFlow()

    val current: PatientData? get() = _data.value

    // Aplica un cambio a los datos del usuario; si el teléfono tenía datos de otro usuario, empieza vacío
    @Synchronized
    fun update(userId: Int, change: (PatientData) -> PatientData): PatientData {
        val base = _data.value?.takeIf { it.userId == userId } ?: PatientData(userId)
        val next = change(base)
        prefs.edit { putString(KEY, LocalCipher.encrypt(ApiJson.encodeToString(PatientData.serializer(), next))) }
        _data.value = next
        return next
    }

    fun saveMedications(userId: Int, medications: List<LocalMedication>) =
        update(userId) { it.copy(medications = medications) }

    /**
     * Reemplaza las confirmaciones conocidas de los días [dates] ("yyyy-MM-dd") por [confirmed]
     * y descarta las de más de 8 días.
     */
    fun saveServerConfirmed(userId: Int, dates: Set<String>, confirmed: List<ConfirmedIntake>) =
        update(userId) { data ->
            val oldest = nowMexico().minusDays(8)
            val kept = data.serverConfirmed.filter { c ->
                c.scheduledFor.take(10) !in dates &&
                    (parseApiDateTime(c.scheduledFor)?.isAfter(oldest) ?: false)
            }
            data.copy(serverConfirmed = kept + confirmed)
        }

    @Synchronized
    fun clear() {
        prefs.edit { clear() }
        _data.value = null
    }

    private fun read(): PatientData? {
        val stored = prefs.getString(KEY, null) ?: return null
        return runCatching {
            ApiJson.decodeFromString(PatientData.serializer(), LocalCipher.decrypt(stored))
        }.getOrNull()
    }

    private companion object {
        const val KEY = "data"
    }
}
