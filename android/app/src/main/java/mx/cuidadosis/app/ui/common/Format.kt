package mx.cuidadosis.app.ui.common

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// La API manda fechas en hora local de America/Mexico_City: "yyyy-MM-dd HH:mm:ss" y "yyyy-MM-dd"
private val API_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
private val SHOW_DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
private val SHOW_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private val SHOW_TIME = DateTimeFormatter.ofPattern("HH:mm")

fun formatApiDateTime(value: String?): String =
    value?.let { runCatching { LocalDateTime.parse(it, API_DATE_TIME).format(SHOW_DATE_TIME) }.getOrNull() } ?: ""

fun formatApiTime(value: String?): String =
    value?.let { runCatching { LocalDateTime.parse(it, API_DATE_TIME).format(SHOW_TIME) }.getOrNull() } ?: ""

fun formatApiDate(value: String?): String =
    value?.let { runCatching { LocalDate.parse(it).format(SHOW_DATE) }.getOrNull() } ?: ""

// Las horas de la API están en hora de México, sin importar la zona del teléfono
private val API_ZONE: ZoneId = ZoneId.of("America/Mexico_City")

// Una toma pendiente cuya hora ("HH:MM") ya pasó hoy
fun isTimePast(time: String, now: LocalTime = LocalTime.now(API_ZONE)): Boolean =
    runCatching { LocalTime.parse(time).isBefore(now) }.getOrDefault(false)

fun summaryText(taken: Int, total: Int): String = when (total) {
    0 -> "Sin tomas programadas hoy"
    1 -> "$taken de 1 toma registrada"
    else -> "$taken de $total tomas registradas"
}
