package mx.cuidadosis.app.data

import mx.cuidadosis.app.data.local.ConfirmedIntake
import mx.cuidadosis.app.data.local.LocalConfirmation
import mx.cuidadosis.app.data.local.PatientData
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

// Lógica pura del modo sin red (RF-13, ADR-003): cola de confirmaciones y lista de "Hoy" local.

/**
 * Agrega una confirmación a la cola si esa toma no estaba ya confirmada (en la cola o en el
 * servidor). Un doble toque no genera dos confirmaciones.
 */
fun enqueueConfirmation(data: PatientData, confirmation: LocalConfirmation): PatientData {
    val at = parseApiDateTime(confirmation.scheduledFor) ?: return data
    if ((confirmation.scheduleId to at) in data.confirmedSet()) return data
    return data.copy(queue = data.queue + confirmation)
}

// Resultados de POST /intakes/sync que sacan una confirmación de la cola (todas menos un error de red)
val SYNC_DONE_STATUSES = setOf("created", "duplicate", "already_confirmed", "rejected")

/**
 * Aplica la respuesta del servidor: quita de la cola las confirmaciones respondidas y pasa a
 * "confirmadas en el servidor" las aceptadas. Lo que no venga en [results] sigue en la cola.
 */
fun applySyncResults(data: PatientData, results: Map<String, String>, lastSyncAt: String?): PatientData {
    val done = data.queue.filter { results[it.clientUuid] in SYNC_DONE_STATUSES }
    val accepted = done.filter { results[it.clientUuid] != "rejected" }
        .map { ConfirmedIntake(it.scheduleId, it.scheduledFor, it.confirmedAt) }
        .filter { it !in data.serverConfirmed }
    return data.copy(
        queue = data.queue - done.toSet(),
        serverConfirmed = data.serverConfirmed + accepted,
        lastSyncAt = lastSyncAt ?: data.lastSyncAt,
    )
}

// Reintentar ante falta de red (sin código) o error del servidor; no ante 4xx
fun shouldRetrySync(httpCode: Int?): Boolean = httpCode == null || httpCode >= 500

// Una toma de hoy para la pantalla del paciente, armada sin red
data class TodayItem(
    val scheduleId: Int,
    val name: String,
    val dose: String,
    val time: String,
    val scheduledFor: LocalDateTime,
    val confirmedAt: String?,
    // Confirmada en el teléfono, todavía sin enviar al servidor
    val waitingSync: Boolean,
) {
    val confirmed: Boolean get() = confirmedAt != null
}

/**
 * Lista de hoy: las tomas que mandó el servidor (si son de hoy) más las de los medicamentos
 * guardados que falten, con las confirmaciones de la cola encima. Ordenada por hora.
 */
fun buildTodayList(data: PatientData, today: LocalDate): List<TodayItem> {
    val key = { scheduleId: Int, at: LocalDateTime -> scheduleId to at }
    val items = linkedMapOf<Pair<Int, LocalDateTime>, TodayItem>()

    if (data.todayDate == today.toString()) {
        data.todayIntakes.forEach { c ->
            val at = parseApiDateTime(c.scheduledFor) ?: return@forEach
            items[key(c.scheduleId, at)] = TodayItem(c.scheduleId, c.name, c.dose, c.time, at, c.confirmedAt, false)
        }
    }
    data.medications.forEach { med ->
        med.schedules.forEach { s ->
            val time = runCatching { LocalTime.parse(s.time) }.getOrNull() ?: return@forEach
            val at = LocalDateTime.of(today, time.withSecond(0).withNano(0))
            items.getOrPut(key(s.id, at)) { TodayItem(s.id, med.name, med.dose, s.time.take(5), at, null, false) }
        }
    }
    data.serverConfirmed.forEach { c ->
        val at = parseApiDateTime(c.scheduledFor) ?: return@forEach
        items[key(c.scheduleId, at)]?.let { if (it.confirmedAt == null) items[key(c.scheduleId, at)] = it.copy(confirmedAt = c.confirmedAt) }
    }
    data.queue.forEach { q ->
        val at = parseApiDateTime(q.scheduledFor) ?: return@forEach
        items[key(q.scheduleId, at)]?.let { items[key(q.scheduleId, at)] = it.copy(confirmedAt = q.confirmedAt, waitingSync = true) }
    }
    return items.values.sortedWith(compareBy({ it.scheduledFor }, { it.name.lowercase() }))
}
