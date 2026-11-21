package mx.cuidadosis.app.data

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// Hora de México, igual que la API (ADR-008)
val MX_ZONE: ZoneId = ZoneId.of("America/Mexico_City")

// Formato de fechas de la API: "yyyy-MM-dd HH:mm:ss"
val API_DATE_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

fun parseApiDateTime(value: String?): LocalDateTime? =
    value?.let { runCatching { LocalDateTime.parse(it, API_DATE_TIME) }.getOrNull() }

fun formatApiDateTimeValue(value: LocalDateTime): String = value.withNano(0).format(API_DATE_TIME)

fun nowMexico(): LocalDateTime = LocalDateTime.now(MX_ZONE).withNano(0)

// Minutos tras la hora programada para que una toma pendiente pase a "Sin confirmar" (RF-11)
const val UNCONFIRMED_AFTER_MINUTES = 30L

/**
 * Estado visible de una toma (ADR-005). Solo se guarda pendiente o confirmada;
 * "Sin confirmar" se calcula. La confirmación no verifica la ingesta (ADR-006).
 */
enum class IntakeState(val label: String) {
    PENDING("Pendiente"),
    CONFIRMED("Confirmada por el paciente"),
    UNCONFIRMED("Sin confirmar"),
}

fun intakeState(scheduledFor: LocalDateTime, confirmed: Boolean, now: LocalDateTime): IntakeState = when {
    confirmed -> IntakeState.CONFIRMED
    now.isAfter(scheduledFor.plusMinutes(UNCONFIRMED_AFTER_MINUTES)) -> IntakeState.UNCONFIRMED
    else -> IntakeState.PENDING
}

// Leyenda obligatoria junto a las etiquetas (RF-10)
const val CONFIRMATION_DISCLAIMER =
    "\"Confirmada por el paciente\" solo indica que tocó \"Tomado\"; no verifica que haya tomado el medicamento."
