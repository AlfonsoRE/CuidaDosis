package mx.cuidadosis.app

import mx.cuidadosis.app.reminders.REMINDER_ZONE
import mx.cuidadosis.app.reminders.ReminderMed
import mx.cuidadosis.app.reminders.nextReminder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime

class ReminderScheduleTest {
    // Datos sintéticos de prueba
    private val med1 = ReminderMed(1, "SINTETICO Med A", "1 tableta", listOf("08:00", "20:00"))
    private val med2 = ReminderMed(2, "SINTETICO Med B", "5 ml", listOf("20:00"))

    // Instante a partir de una hora de México
    private fun mx(value: String): Instant = LocalDateTime.parse(value).atZone(REMINDER_ZONE).toInstant()

    @Test
    fun avisaCincoMinutosAntesDeLaSiguienteToma() {
        val r = nextReminder(listOf(med1), mx("2026-11-12T07:00"))!!
        assertEquals(LocalDateTime.parse("2026-11-12T08:00"), r.intakeAt)
        assertEquals(mx("2026-11-12T07:55"), r.triggerAt)
        assertEquals("08:00", r.time)
    }

    @Test
    fun pasaAlDiaSiguienteDespuesDeLaUltimaToma() {
        val r = nextReminder(listOf(med1), mx("2026-11-12T21:00"))!!
        assertEquals(LocalDateTime.parse("2026-11-13T08:00"), r.intakeAt)
        assertEquals(mx("2026-11-13T07:55"), r.triggerAt)
    }

    @Test
    fun avisaDeInmediatoSiFaltanMenosDeCincoMinutos() {
        val now = mx("2026-11-12T07:58")
        val r = nextReminder(listOf(med1), now)!!
        assertEquals(LocalDateTime.parse("2026-11-12T08:00"), r.intakeAt)
        assertEquals(now, r.triggerAt)
    }

    @Test
    fun laTomaExactamenteAhoraYaNoSeAvisa() {
        val r = nextReminder(listOf(med1), mx("2026-11-12T08:00"))!!
        assertEquals(LocalDateTime.parse("2026-11-12T20:00"), r.intakeAt)
    }

    @Test
    fun agrupaMedicamentosDeLaMismaHora() {
        val r = nextReminder(listOf(med2, med1), mx("2026-11-12T12:00"))!!
        assertEquals(LocalDateTime.parse("2026-11-12T20:00"), r.intakeAt)
        assertEquals(listOf(med1, med2), r.meds)
    }

    @Test
    fun noRepiteUnaTomaYaAvisada() {
        // Ya se avisó la de las 08:00 (a las 07:55); al reprogramar a las 07:56 sigue la de las 20:00
        val r = nextReminder(listOf(med1), mx("2026-11-12T07:56"), setOf(LocalDateTime.parse("2026-11-12T08:00")))!!
        assertEquals(LocalDateTime.parse("2026-11-12T20:00"), r.intakeAt)
    }

    @Test
    fun avisaUnaHoraNuevaAnteriorAUnaYaAvisada() {
        // Se avisó la de las 08:00 y luego se agregó un medicamento a las 07:58: también se avisa
        val nuevo = ReminderMed(6, "SINTETICO Med E", "1 tableta", listOf("07:58"))
        val now = mx("2026-11-12T07:56")
        val r = nextReminder(listOf(med1, nuevo), now, setOf(LocalDateTime.parse("2026-11-12T08:00")))!!
        assertEquals(LocalDateTime.parse("2026-11-12T07:58"), r.intakeAt)
        assertEquals(now, r.triggerAt)
        // Después de avisar la de 07:58 no se repite la de 08:00
        val notified = setOf(LocalDateTime.parse("2026-11-12T08:00"), LocalDateTime.parse("2026-11-12T07:58"))
        assertEquals(LocalDateTime.parse("2026-11-12T20:00"), nextReminder(listOf(med1, nuevo), now, notified)!!.intakeAt)
    }

    @Test
    fun usaHoraDeMexicoAunqueElTelefonoEsteEnUtc() {
        // 2026-11-12 13:00 UTC son las 07:00 en México (UTC-6, sin horario de verano)
        val r = nextReminder(listOf(med1), Instant.parse("2026-11-12T13:00:00Z"))!!
        assertEquals(LocalDateTime.parse("2026-11-12T08:00"), r.intakeAt)
        assertEquals(Instant.parse("2026-11-12T13:55:00Z"), r.triggerAt)
    }

    @Test
    fun fechaDeMexicoCercaDeMedianoche() {
        // 2026-11-13 05:00 UTC todavía es 2026-11-12 23:00 en México
        val med = ReminderMed(3, "SINTETICO Med C", "1 gota", listOf("23:30"))
        val r = nextReminder(listOf(med), Instant.parse("2026-11-13T05:00:00Z"))!!
        assertEquals(LocalDateTime.parse("2026-11-12T23:30"), r.intakeAt)
    }

    @Test
    fun aceptaHorasConSegundos() {
        val med = ReminderMed(4, "SINTETICO Med D", "1 tableta", listOf("09:15:00"))
        val r = nextReminder(listOf(med), mx("2026-11-12T09:00"))!!
        assertEquals(LocalDateTime.parse("2026-11-12T09:15"), r.intakeAt)
        assertEquals("09:15", r.time)
    }

    @Test
    fun sinMedicamentosNoHayAviso() {
        assertNull(nextReminder(emptyList(), mx("2026-11-12T07:00")))
        assertNull(nextReminder(listOf(ReminderMed(5, "SINTETICO", "x", emptyList())), mx("2026-11-12T07:00")))
    }
}
