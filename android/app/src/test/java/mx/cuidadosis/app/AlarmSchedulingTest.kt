package mx.cuidadosis.app

import mx.cuidadosis.app.reminders.ALARM_RING_LIMIT_SECONDS
import mx.cuidadosis.app.reminders.REMINDER_ZONE
import mx.cuidadosis.app.reminders.ReminderKind
import mx.cuidadosis.app.reminders.ReminderMed
import mx.cuidadosis.app.reminders.Snooze
import mx.cuidadosis.app.reminders.alarmRequestCode
import mx.cuidadosis.app.reminders.alarmRingUntil
import mx.cuidadosis.app.reminders.eventKey
import mx.cuidadosis.app.reminders.nextReminderEvent
import mx.cuidadosis.app.reminders.shouldKeepSilentReminder
import mx.cuidadosis.app.reminders.upcomingReminderEvents
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime

// Planificador v1.1: una alarma por aviso, alarma a T con límite de 2 min (RF-05, RF-06, RF-16). Datos sintéticos.
class AlarmSchedulingTest {
    private val med = ReminderMed(1, "SINTETICO Med A", "1 tableta", listOf("08:00", "20:00"), mapOf("08:00" to 11, "20:00" to 12))
    private val t8 = LocalDateTime.parse("2026-11-22T08:00")
    private val t20 = LocalDateTime.parse("2026-11-22T20:00")

    private fun mx(v: String): Instant = LocalDateTime.parse(v).atZone(REMINDER_ZONE).toInstant()

    @Test
    fun cadaTomaTieneSuAlarmaATMenosLYOtraAT() {
        val events = upcomingReminderEvents(listOf(med), mx("2026-11-22T07:00"), leadMinutes = 10)
        val today = events.filter { it.intakeAt.toLocalDate() == t8.toLocalDate() }
        assertEquals(
            listOf(
                ReminderKind.BEFORE to mx("2026-11-22T07:50"),
                ReminderKind.DUE to mx("2026-11-22T08:00"),
                ReminderKind.BEFORE to mx("2026-11-22T19:50"),
                ReminderKind.DUE to mx("2026-11-22T20:00"),
            ),
            today.map { it.kind to it.triggerAt },
        )
        // También quedan programadas las de mañana: 8 en total, ordenadas por hora
        assertEquals(8, events.size)
        assertEquals(events.sortedBy { it.triggerAt }, events)
    }

    @Test
    fun elProximoEventoEsElPrimeroDeLaLista() {
        val now = mx("2026-11-22T07:00")
        assertEquals(upcomingReminderEvents(listOf(med), now).first(), nextReminderEvent(listOf(med), now))
    }

    @Test
    fun cadaAvisoTieneUnCodigoDeAlarmaDistintoYEstable() {
        val events = upcomingReminderEvents(listOf(med), mx("2026-11-22T07:00"))
        val codes = events.map { alarmRequestCode(it) }
        assertEquals(codes.size, codes.toSet().size)
        // Recalcular (por ejemplo, tras reiniciar) da los mismos códigos: se reemplazan, no se duplican
        assertEquals(codes, upcomingReminderEvents(listOf(med), mx("2026-11-22T07:00")).map { alarmRequestCode(it) })
    }

    @Test
    fun confirmarCancelaLasAlarmasDeEsaToma() {
        val events = upcomingReminderEvents(listOf(med), mx("2026-11-22T07:00"), confirmed = setOf(11 to t8))
        assertTrue(events.none { it.intakeAt == t8 })
        assertTrue(events.any { it.intakeAt == t20 })
    }

    @Test
    fun posponerSustituyeALaAlarmaDeLaHoraT() {
        // El paciente pospone 10 min a las 08:00: el evento T ya sonó y queda el pospuesto a las 08:10
        val fired = setOf(eventKey(t8, ReminderKind.BEFORE), eventKey(t8, ReminderKind.DUE))
        val snooze = Snooze(t8.toString(), mx("2026-11-22T08:10").epochSecond)
        val events = upcomingReminderEvents(listOf(med), mx("2026-11-22T08:00:30"), 5, fired, snoozes = listOf(snooze))
        val first = events.first()
        assertEquals(ReminderKind.SNOOZE, first.kind)
        assertEquals(mx("2026-11-22T08:10"), first.triggerAt)
        assertTrue(events.none { it.intakeAt == t8 && it.kind == ReminderKind.DUE })
    }

    @Test
    fun trasReiniciarSeReprogramanLosQueFaltan() {
        // Antes del reinicio ya sonaron los de las 08:00; al reprogramar a las 09:00 quedan los de las 20:00 y mañana
        val fired = setOf(eventKey(t8, ReminderKind.BEFORE), eventKey(t8, ReminderKind.DUE))
        val events = upcomingReminderEvents(listOf(med), mx("2026-11-22T09:00"), 5, fired)
        assertEquals(t20, events.first().intakeAt)
        assertEquals(ReminderKind.BEFORE, events.first().kind)
        assertTrue(events.none { it.intakeAt == t8 })
    }

    @Test
    fun laAlarmaSuenaComoMaximoDosMinutos() {
        val start = mx("2026-11-22T08:00")
        assertEquals(120L, ALARM_RING_LIMIT_SECONDS)
        assertEquals(mx("2026-11-22T08:02"), alarmRingUntil(start))
    }

    @Test
    fun trasElLimiteQuedaLaNotificacionSoloSiSiguePendiente() {
        assertTrue(shouldKeepSilentReminder(1))
        assertTrue(shouldKeepSilentReminder(3))
        assertFalse(shouldKeepSilentReminder(0))
    }
}
