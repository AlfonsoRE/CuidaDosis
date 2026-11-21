package mx.cuidadosis.app

import mx.cuidadosis.app.data.applySyncResults
import mx.cuidadosis.app.data.buildTodayList
import mx.cuidadosis.app.data.enqueueConfirmation
import mx.cuidadosis.app.data.shouldRetrySync
import mx.cuidadosis.app.data.local.CachedIntake
import mx.cuidadosis.app.data.local.ConfirmedIntake
import mx.cuidadosis.app.data.local.LocalConfirmation
import mx.cuidadosis.app.data.local.LocalMedication
import mx.cuidadosis.app.data.local.LocalSchedule
import mx.cuidadosis.app.data.local.PatientData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

// Cola de confirmaciones sin red (RF-13, ADR-003). Datos sintéticos.
class OfflineQueueTest {
    private val med = LocalMedication(1, "SINTETICO Med A", "1 tableta", listOf(LocalSchedule(11, "08:00"), LocalSchedule(12, "20:00")))
    private val base = PatientData(userId = 7, medications = listOf(med))
    private val today = LocalDate.parse("2026-11-22")
    private fun conf(uuid: String, scheduleId: Int = 11, at: String = "2026-11-22 08:00:00") =
        LocalConfirmation(uuid, scheduleId, at, "2026-11-22 08:02:00")

    @Test
    fun dobleToqueNoDuplicaLaConfirmacion() {
        val once = enqueueConfirmation(base, conf("u-1"))
        val twice = enqueueConfirmation(once, conf("u-2"))
        assertEquals(1, twice.queue.size)
        assertEquals("u-1", twice.queue.single().clientUuid)
    }

    @Test
    fun noEncolaUnaTomaYaConfirmadaEnElServidor() {
        val data = base.copy(serverConfirmed = listOf(ConfirmedIntake(11, "2026-11-22 08:00:00", "2026-11-22 08:01:00")))
        assertTrue(enqueueConfirmation(data, conf("u-1")).queue.isEmpty())
    }

    @Test
    fun respuestaRepetidaEsIdempotente() {
        val queued = enqueueConfirmation(base, conf("u-1"))
        val first = applySyncResults(queued, mapOf("u-1" to "created"), "2026-11-22 09:00:00")
        // El servidor responde duplicate a un reenvío: el estado local no cambia
        val again = applySyncResults(first.copy(queue = queued.queue), mapOf("u-1" to "duplicate"), "2026-11-22 09:05:00")
        assertTrue(again.queue.isEmpty())
        assertEquals(1, again.serverConfirmed.size)
        assertEquals("2026-11-22 09:05:00", again.lastSyncAt)
    }

    @Test
    fun sinRespuestaLaConfirmacionSigueEnLaCola() {
        val queued = enqueueConfirmation(base, conf("u-1"))
        val after = applySyncResults(queued, emptyMap(), null)
        assertEquals(queued.queue, after.queue)
    }

    @Test
    fun rechazadaSaleDeLaColaPeroNoCuentaComoConfirmada() {
        val queued = enqueueConfirmation(base, conf("u-1"))
        val after = applySyncResults(queued, mapOf("u-1" to "rejected"), "2026-11-22 09:00:00")
        assertTrue(after.queue.isEmpty())
        assertTrue(after.serverConfirmed.isEmpty())
    }

    @Test
    fun tresConfirmacionesSinRedSeEnvianSinPerderNinguna() {
        val med2 = med.copy(id = 2, schedules = listOf(LocalSchedule(13, "14:00")))
        var data = base.copy(medications = listOf(med, med2))
        data = enqueueConfirmation(data, conf("a", 11, "2026-11-22 08:00:00"))
        data = enqueueConfirmation(data, conf("b", 13, "2026-11-22 14:00:00"))
        data = enqueueConfirmation(data, conf("c", 12, "2026-11-22 20:00:00"))
        assertEquals(3, data.queue.size)
        val after = applySyncResults(data, mapOf("a" to "created", "b" to "created", "c" to "already_confirmed"), "2026-11-22 21:00:00")
        assertTrue(after.queue.isEmpty())
        assertEquals(3, after.serverConfirmed.size)
    }

    @Test
    fun soloReintentaSinRedOConErrorDelServidor() {
        assertTrue(shouldRetrySync(null))
        assertTrue(shouldRetrySync(500))
        assertTrue(shouldRetrySync(503))
        assertFalse(shouldRetrySync(422))
        assertFalse(shouldRetrySync(403))
    }

    @Test
    fun listaDeHoySinRedUsaMedicamentosYCola() {
        val data = enqueueConfirmation(base, conf("u-1"))
        val list = buildTodayList(data, today)
        assertEquals(listOf("08:00", "20:00"), list.map { it.time })
        assertTrue(list[0].confirmed && list[0].waitingSync)
        assertFalse(list[1].confirmed)
    }

    @Test
    fun listaDeHoyIncluyeLoQueMandoElServidor() {
        // Una toma de un medicamento ya desactivado sigue apareciendo si el servidor la mandó
        val data = base.copy(
            todayDate = "2026-11-22",
            todayIntakes = listOf(CachedIntake(99, 5, "SINTETICO Med X", "2 gotas", "06:00", "2026-11-22 06:00:00", "2026-11-22 06:05:00")),
        )
        val list = buildTodayList(data, today)
        assertEquals(listOf("06:00", "08:00", "20:00"), list.map { it.time })
        assertTrue(list[0].confirmed && !list[0].waitingSync)
        // La copia de otro día no se usa
        assertEquals(2, buildTodayList(data, today.plusDays(1)).size)
    }
}
