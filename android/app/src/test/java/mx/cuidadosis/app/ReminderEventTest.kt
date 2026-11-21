package mx.cuidadosis.app

import mx.cuidadosis.app.reminders.REMINDER_ZONE
import mx.cuidadosis.app.reminders.ReminderKind
import mx.cuidadosis.app.reminders.ReminderMed
import mx.cuidadosis.app.reminders.Snooze
import mx.cuidadosis.app.reminders.eventKey
import mx.cuidadosis.app.reminders.nextReminderEvent
import mx.cuidadosis.app.reminders.normalizeLead
import mx.cuidadosis.app.reminders.pendingMedsAt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime

// Avisos T - L, T y pospuestos (RF-05, RF-06, RF-16). Datos sintéticos.
class ReminderEventTest {
    private val medA = ReminderMed(1, "SINTETICO Med A", "1 tableta", listOf("08:00"), mapOf("08:00" to 11))
    private val medB = ReminderMed(2, "SINTETICO Med B", "5 ml", listOf("08:00"), mapOf("08:00" to 12))
    private val t8 = LocalDateTime.parse("2026-11-22T08:00")

    private fun mx(value: String): Instant = LocalDateTime.parse(value).atZone(REMINDER_ZONE).toInstant()

    @Test
    fun conLDiezElPrimerAvisoEsDiezMinutosAntes() {
        val e = nextReminderEvent(listOf(medA), mx("2026-11-22T07:00"), leadMinutes = 10)!!
        assertEquals(ReminderKind.BEFORE, e.kind)
        assertEquals(mx("2026-11-22T07:50"), e.triggerAt)
    }

    @Test
    fun conLCincoElPrimerAvisoEsCincoMinutosAntes() {
        val e = nextReminderEvent(listOf(medA), mx("2026-11-22T07:00"), leadMinutes = 5)!!
        assertEquals(mx("2026-11-22T07:55"), e.triggerAt)
    }

    @Test
    fun despuesDelPrimerAvisoVieneElDeLaHoraT() {
        val fired = setOf(eventKey(t8, ReminderKind.BEFORE))
        val e = nextReminderEvent(listOf(medA), mx("2026-11-22T07:56"), 5, fired)!!
        assertEquals(ReminderKind.DUE, e.kind)
        assertEquals(mx("2026-11-22T08:00"), e.triggerAt)
    }

    @Test
    fun elAvisoDeLaHoraTSuenaUnaSolaVez() {
        val fired = setOf(eventKey(t8, ReminderKind.BEFORE), eventKey(t8, ReminderKind.DUE))
        val e = nextReminderEvent(listOf(medA), mx("2026-11-22T08:01"), 5, fired)!!
        assertEquals(LocalDateTime.parse("2026-11-23T08:00"), e.intakeAt)
        assertEquals(ReminderKind.BEFORE, e.kind)
    }

    @Test
    fun unaTomaConfirmadaNoGeneraAvisos() {
        val confirmed = setOf(11 to t8)
        val e = nextReminderEvent(listOf(medA), mx("2026-11-22T07:00"), 5, confirmed = confirmed)!!
        assertEquals(LocalDateTime.parse("2026-11-23T08:00"), e.intakeAt)
    }

    @Test
    fun soloAvisaLosMedicamentosSinConfirmarDeEsaHora() {
        val pending = pendingMedsAt(listOf(medA, medB), t8, setOf(11 to t8))
        assertEquals(listOf(medB), pending)
        val e = nextReminderEvent(listOf(medA, medB), mx("2026-11-22T07:00"), 5, confirmed = setOf(11 to t8))!!
        assertEquals(listOf(medB), e.meds)
    }

    @Test
    fun posponerVuelveAAvisarEnElTiempoElegido() {
        val fired = setOf(eventKey(t8, ReminderKind.BEFORE), eventKey(t8, ReminderKind.DUE))
        val at = mx("2026-11-22T08:10")
        val e = nextReminderEvent(listOf(medA), mx("2026-11-22T08:00"), 5, fired, snoozes = listOf(Snooze(t8.toString(), at.epochSecond)))!!
        assertEquals(ReminderKind.SNOOZE, e.kind)
        assertEquals(at, e.triggerAt)
    }

    @Test
    fun unPospuestoDeUnaTomaConfirmadaNoSuena() {
        val fired = setOf(eventKey(t8, ReminderKind.BEFORE), eventKey(t8, ReminderKind.DUE))
        val snooze = Snooze(t8.toString(), mx("2026-11-22T08:10").epochSecond)
        val e = nextReminderEvent(listOf(medA), mx("2026-11-22T08:05"), 5, fired, setOf(11 to t8), listOf(snooze))!!
        assertEquals(LocalDateTime.parse("2026-11-23T08:00"), e.intakeAt)
    }

    @Test
    fun avisoDeLaHoraTMuyAtrasadoYaNoSale() {
        // Teléfono apagado de 07:00 a 08:30: el de las 08:00 ya no se muestra
        val e = nextReminderEvent(listOf(medA), mx("2026-11-22T08:30"), 5)!!
        assertEquals(LocalDateTime.parse("2026-11-23T08:00"), e.intakeAt)
    }

    @Test
    fun avisoDeLaHoraTCercaDeMedianocheDelDiaAnterior() {
        val med = ReminderMed(3, "SINTETICO Med C", "1 gota", listOf("23:58"), mapOf("23:58" to 13))
        val slot = LocalDateTime.parse("2026-11-22T23:58")
        val e = nextReminderEvent(listOf(med), mx("2026-11-23T00:02"), 5, setOf(eventKey(slot, ReminderKind.BEFORE)))!!
        assertEquals(slot, e.intakeAt)
        assertEquals(ReminderKind.DUE, e.kind)
        assertEquals(mx("2026-11-23T00:02"), e.triggerAt)
    }

    @Test
    fun reglasDeL() {
        assertEquals(5, normalizeLead(null))
        assertEquals(5, normalizeLead(5))
        assertEquals(10, normalizeLead(10))
        assertEquals(5, normalizeLead(7))
        assertEquals(5, normalizeLead(0))
    }

    @Test
    fun sinMedicamentosNoHayEventos() {
        assertNull(nextReminderEvent(emptyList(), mx("2026-11-22T07:00")))
    }
}
