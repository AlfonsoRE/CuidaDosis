package mx.cuidadosis.app.reminders

import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

// Las horas de los medicamentos son de México, sin importar la zona del teléfono (igual que la API)
val REMINDER_ZONE: ZoneId = ZoneId.of("America/Mexico_City")

// Minutos de anticipación del aviso
const val REMINDER_MINUTES_BEFORE = 5L

// Copia local mínima de un medicamento activo. Nombre y dosis se muestran tal como los escribió el usuario.
@Serializable
data class ReminderMed(
    val id: Int,
    val name: String,
    val dose: String,
    val times: List<String>,
)

// Próximo aviso: la toma (hora de México), cuándo disparar y qué medicamentos incluye
data class Reminder(
    val intakeAt: LocalDateTime,
    val triggerAt: Instant,
    val time: String,
    val meds: List<ReminderMed>,
)

/**
 * Calcula el próximo aviso. Considera cada hora distinta hoy y mañana (fecha de México) y toma
 * la primera posterior a [now] que no esté en [notified] (tomas ya avisadas, para no repetir).
 * Si faltan menos de 5 minutos para la toma, el aviso sale de inmediato.
 */
fun nextReminder(meds: List<ReminderMed>, now: Instant, notified: Set<LocalDateTime> = emptySet()): Reminder? {
    val byTime: Map<LocalTime, List<ReminderMed>> = meds
        .flatMap { med -> med.times.mapNotNull { parseTime(it) }.distinct().map { it to med } }
        .groupBy({ it.first }, { it.second })
    if (byTime.isEmpty()) return null

    val today = now.atZone(REMINDER_ZONE).toLocalDate()
    val candidates = listOf(today, today.plusDays(1))
        .flatMap { date -> byTime.keys.map { LocalDateTime.of(date, it) } }
        .sorted()

    val intakeAt = candidates.firstOrNull { candidate ->
        candidate.atZone(REMINDER_ZONE).toInstant().isAfter(now) && candidate !in notified
    } ?: return null

    val intakeInstant = intakeAt.atZone(REMINDER_ZONE).toInstant()
    val ideal = intakeInstant.minusSeconds(REMINDER_MINUTES_BEFORE * 60)
    return Reminder(
        intakeAt = intakeAt,
        triggerAt = if (ideal.isAfter(now)) ideal else now,
        time = "%02d:%02d".format(intakeAt.hour, intakeAt.minute),
        meds = byTime.getValue(intakeAt.toLocalTime()).sortedBy { it.name.lowercase() },
    )
}

// "HH:MM" o "HH:MM:SS"; se ignoran los segundos
private fun parseTime(value: String): LocalTime? =
    runCatching { LocalTime.parse(value).withSecond(0).withNano(0) }.getOrNull()
