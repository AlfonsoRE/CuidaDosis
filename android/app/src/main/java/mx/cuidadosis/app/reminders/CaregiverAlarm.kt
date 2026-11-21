package mx.cuidadosis.app.reminders

import kotlinx.serialization.Serializable
import mx.cuidadosis.app.data.UNCONFIRMED_AFTER_MINUTES
import mx.cuidadosis.app.data.parseApiDateTime
import mx.cuidadosis.app.data.remote.TodayResponse
import java.time.Instant
import java.time.LocalDateTime

// Alarma local del cuidador a T + 30 por cada toma pendiente de sus pacientes (RF-18, ADR-007)

// Una toma pendiente que el cuidador vigila
@Serializable
data class CaregiverWatch(
    val patientId: Int,
    val patientName: String,
    val intakeId: Int,
    val medicationName: String,
    val scheduledFor: String,
) {
    val intakeAt: LocalDateTime? get() = parseApiDateTime(scheduledFor)
    val key: String get() = "$patientId|$intakeId|$scheduledFor"
}

fun watchTriggerAt(watch: CaregiverWatch): Instant? =
    watch.intakeAt?.plusMinutes(UNCONFIRMED_AFTER_MINUTES)?.atZone(REMINDER_ZONE)?.toInstant()

/**
 * Tomas pendientes del día de un paciente que todavía no llegan a T + 30 (las que ya pasaron se
 * verán como "Sin confirmar" al abrir la app; no se programa alarma para el pasado).
 */
fun watchesFrom(patientId: Int, patientName: String, today: TodayResponse, now: Instant): List<CaregiverWatch> =
    today.intakes
        .filter { !it.isTaken }
        .map { CaregiverWatch(patientId, patientName, it.id, it.name, it.scheduledFor) }
        .filter { w -> watchTriggerAt(w)?.isAfter(now) == true }

// Próxima alarma: la toma vigilada con T + 30 más cercano que no haya sonado
fun nextCaregiverWatch(watches: List<CaregiverWatch>, now: Instant, fired: Set<String>): CaregiverWatch? =
    watches
        .filter { it.key !in fired }
        .filter { w -> watchTriggerAt(w)?.let { !it.plusSeconds(LATE_LIMIT_MINUTES * 60).isBefore(now) } == true }
        .minByOrNull { watchTriggerAt(it)!! }

enum class CaregiverAlert { UNCONFIRMED, CHECK_MANUALLY, NONE, LINK_REVOKED }

/**
 * Qué hacer al sonar la alarma, según la consulta al servidor:
 * - Sin vínculo (404): no avisar y dejar de vigilar a ese paciente.
 * - Sin respuesta (sin red o error): avisar que revise, con la última sincronización conocida.
 * - Toma confirmada: no avisar. Sigue pendiente: avisar "Sin confirmar".
 */
fun caregiverAlertFor(watch: CaregiverWatch, httpCode: Int?, today: TodayResponse?): CaregiverAlert {
    if (httpCode == 404) return CaregiverAlert.LINK_REVOKED
    if (today == null) return CaregiverAlert.CHECK_MANUALLY
    val intake = today.intakes.firstOrNull { it.id == watch.intakeId } ?: return CaregiverAlert.NONE
    return if (intake.isTaken) CaregiverAlert.NONE else CaregiverAlert.UNCONFIRMED
}
