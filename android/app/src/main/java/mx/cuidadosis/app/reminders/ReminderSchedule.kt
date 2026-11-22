package mx.cuidadosis.app.reminders

import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

// Las horas de los medicamentos son de México, sin importar la zona del teléfono (igual que la API)
val REMINDER_ZONE: ZoneId = ZoneId.of("America/Mexico_City")

// Anticipación L del primer aviso (RF-05): 5 o 10 minutos, por defecto 5
const val REMINDER_MINUTES_BEFORE = 5L
val REMINDER_LEAD_OPTIONS = listOf(5, 10)

// Minutos para posponer un aviso (RF-16)
val SNOOZE_OPTIONS = listOf(5, 10)

// Un aviso que llega más tarde que esto (teléfono apagado, por ejemplo) ya no se muestra
const val LATE_LIMIT_MINUTES = 10L

// Alarma de la toma (RF-06, v1.1): suena hasta 2 minutos; después queda la notificación sin sonido
const val ALARM_RING_LIMIT_SECONDS = 120L

// Momento en que la alarma deja de sonar si nadie la atiende
fun alarmRingUntil(startedAt: Instant): Instant = startedAt.plusSeconds(ALARM_RING_LIMIT_SECONDS)

// Al cumplirse el límite: queda la notificación persistente solo si la toma sigue pendiente
fun shouldKeepSilentReminder(pendingMeds: Int): Boolean = pendingMeds > 0

// Cualquier valor guardado que no sea 5 o 10 se trata como 5
fun normalizeLead(value: Int?): Int = if (value != null && value in REMINDER_LEAD_OPTIONS) value else 5

// Copia local mínima de un medicamento activo. Nombre y dosis se muestran tal como los escribió el usuario.
// scheduleIds relaciona cada hora "HH:MM" con su horario en la API (para saber si esa toma ya se confirmó).
@Serializable
data class ReminderMed(
    val id: Int,
    val name: String,
    val dose: String,
    val times: List<String>,
    val scheduleIds: Map<String, Int> = emptyMap(),
) {
    fun scheduleIdAt(time: LocalTime): Int? = scheduleIds["%02d:%02d".format(time.hour, time.minute)]
}

// Tipos de aviso: antes de la toma (T - L), a la hora (T, RF-06) y pospuesto (RF-16)
enum class ReminderKind { BEFORE, DUE, SNOOZE }

// Aviso pospuesto por el paciente: la toma y cuándo volver a avisar
@Serializable
data class Snooze(val intakeAt: String, val atEpochSecond: Long) {
    val intake: LocalDateTime get() = LocalDateTime.parse(intakeAt)
    val at: Instant get() = Instant.ofEpochSecond(atEpochSecond)
}

data class ReminderEvent(
    val intakeAt: LocalDateTime,
    val kind: ReminderKind,
    val triggerAt: Instant,
    val meds: List<ReminderMed>,
    // Identifica el evento para no repetirlo
    val key: String,
) {
    val time: String get() = "%02d:%02d".format(intakeAt.hour, intakeAt.minute)
}

fun eventKey(intakeAt: LocalDateTime, kind: ReminderKind, snoozeAt: Instant? = null): String =
    if (snoozeAt == null) "$intakeAt|$kind" else "$intakeAt|$kind|${snoozeAt.epochSecond}"

/**
 * Medicamentos de una toma (fecha y hora de México) que siguen sin confirmar.
 * [confirmed] son pares (horario, hora programada) ya confirmados, en el servidor o en la cola local.
 */
fun pendingMedsAt(
    meds: List<ReminderMed>,
    intakeAt: LocalDateTime,
    confirmed: Set<Pair<Int, LocalDateTime>>,
): List<ReminderMed> {
    val time = intakeAt.toLocalTime()
    return meds
        .filter { med -> med.times.mapNotNull { parseTime(it) }.contains(time) }
        .filter { med ->
            val scheduleId = med.scheduleIdAt(time)
            scheduleId == null || (scheduleId to intakeAt) !in confirmed
        }
        .sortedBy { it.name.lowercase() }
}

/**
 * Calcula el próximo aviso entre todas las tomas de ayer, hoy y mañana (fecha de México):
 * - BEFORE a T - [leadMinutes]; si ya pasó pero aún no es T, sale de inmediato.
 * - DUE a la hora T si la toma sigue pendiente (hasta T + 10 min): desde la v1.1 es la alarma que se\n *   repite hasta Tomado o Posponer (el planificador la dispara una vez; la repetición es de la notificación).
 * - SNOOZE a la hora elegida al posponer (hasta 10 min después).
 * Se omiten las tomas ya confirmadas y los eventos de [fired] (ya mostrados).
 * [kinds] limita los tipos considerados.
 */
fun nextReminderEvent(
    meds: List<ReminderMed>,
    now: Instant,
    leadMinutes: Int = REMINDER_MINUTES_BEFORE.toInt(),
    fired: Set<String> = emptySet(),
    confirmed: Set<Pair<Int, LocalDateTime>> = emptySet(),
    snoozes: List<Snooze> = emptyList(),
    kinds: Set<ReminderKind> = ReminderKind.entries.toSet(),
): ReminderEvent? = upcomingReminderEvents(meds, now, leadMinutes, fired, confirmed, snoozes, kinds).firstOrNull()

/**
 * Todos los avisos pendientes de ayer, hoy y mañana (mismas reglas que [nextReminderEvent]),
 * ordenados por hora de disparo. Desde la v1.1 cada uno lleva su propia alarma, así que un aviso
 * atrasado por el sistema ya no arrastra a los siguientes.
 */
fun upcomingReminderEvents(
    meds: List<ReminderMed>,
    now: Instant,
    leadMinutes: Int = REMINDER_MINUTES_BEFORE.toInt(),
    fired: Set<String> = emptySet(),
    confirmed: Set<Pair<Int, LocalDateTime>> = emptySet(),
    snoozes: List<Snooze> = emptyList(),
    kinds: Set<ReminderKind> = ReminderKind.entries.toSet(),
): List<ReminderEvent> {
    val times = meds.flatMap { med -> med.times.mapNotNull { parseTime(it) } }.distinct()
    if (times.isEmpty()) return emptyList()

    val today = now.atZone(REMINDER_ZONE).toLocalDate()
    val slots = listOf(today.minusDays(1), today, today.plusDays(1))
        .flatMap { date -> times.map { LocalDateTime.of(date, it) } }
    val late = LATE_LIMIT_MINUTES * 60

    val candidates = mutableListOf<ReminderEvent>()
    for (slot in slots) {
        val pending = pendingMedsAt(meds, slot, confirmed)
        if (pending.isEmpty()) continue
        val t = slot.atZone(REMINDER_ZONE).toInstant()

        if (ReminderKind.BEFORE in kinds) {
            val key = eventKey(slot, ReminderKind.BEFORE)
            val ideal = t.minusSeconds(leadMinutes * 60L)
            if (now.isBefore(t) && key !in fired) {
                candidates += ReminderEvent(slot, ReminderKind.BEFORE, maxOf(ideal, now), pending, key)
            }
        }
        if (ReminderKind.DUE in kinds) {
            val key = eventKey(slot, ReminderKind.DUE)
            if (!now.isAfter(t.plusSeconds(late)) && key !in fired) {
                candidates += ReminderEvent(slot, ReminderKind.DUE, maxOf(t, now), pending, key)
            }
        }
    }
    if (ReminderKind.SNOOZE in kinds) {
        for (snooze in snoozes) {
            val slot = runCatching { snooze.intake }.getOrNull() ?: continue
            val pending = pendingMedsAt(meds, slot, confirmed)
            val key = eventKey(slot, ReminderKind.SNOOZE, snooze.at)
            if (pending.isNotEmpty() && key !in fired && !now.isAfter(snooze.at.plusSeconds(late))) {
                candidates += ReminderEvent(slot, ReminderKind.SNOOZE, maxOf(snooze.at, now), pending, key)
            }
        }
    }
    return candidates.sortedWith(compareBy({ it.triggerAt }, { it.intakeAt }, { it.kind.ordinal }))
}

/**
 * Código de PendingIntent único y estable por evento: día (ciclo de 400 días), minuto del día y tipo.
 * Hay un solo pospuesto por toma, así que tipo y hora bastan. Rango 10 000 000 a 12 304 000.
 */
fun alarmRequestCode(event: ReminderEvent): Int =
    10_000_000 + ((event.intakeAt.toLocalDate().toEpochDay() % 400).toInt() * 1440 + event.intakeAt.hour * 60 + event.intakeAt.minute) * 4 +
        event.kind.ordinal

// Próximo aviso previo (T - L) sin considerar confirmaciones ni pospuestos (forma de la Fase 5)
data class Reminder(
    val intakeAt: LocalDateTime,
    val triggerAt: Instant,
    val time: String,
    val meds: List<ReminderMed>,
)

/**
 * Próximo aviso a T - L posterior a [now] cuya toma no esté en [notified].
 * Si faltan menos de L minutos para la toma, el aviso sale de inmediato.
 */
fun nextReminder(
    meds: List<ReminderMed>,
    now: Instant,
    notified: Set<LocalDateTime> = emptySet(),
    leadMinutes: Int = REMINDER_MINUTES_BEFORE.toInt(),
): Reminder? {
    val fired = notified.map { eventKey(it, ReminderKind.BEFORE) }.toSet()
    val e = nextReminderEvent(meds, now, leadMinutes, fired, kinds = setOf(ReminderKind.BEFORE)) ?: return null
    return Reminder(e.intakeAt, e.triggerAt, e.time, e.meds)
}

// "HH:MM" o "HH:MM:SS"; se ignoran los segundos
private fun parseTime(value: String): LocalTime? =
    runCatching { LocalTime.parse(value).withSecond(0).withNano(0) }.getOrNull()
