package mx.cuidadosis.app

import mx.cuidadosis.app.data.remote.IntakeDto
import mx.cuidadosis.app.data.remote.IntakeSummary
import mx.cuidadosis.app.data.remote.TodayResponse
import mx.cuidadosis.app.reminders.CaregiverAlert
import mx.cuidadosis.app.reminders.CaregiverWatch
import mx.cuidadosis.app.reminders.REMINDER_ZONE
import mx.cuidadosis.app.reminders.caregiverAlertFor
import mx.cuidadosis.app.reminders.nextCaregiverWatch
import mx.cuidadosis.app.reminders.watchTriggerAt
import mx.cuidadosis.app.reminders.watchesFrom
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime

// Alarma del cuidador a T + 30 (RF-18, ADR-007). Datos sintéticos.
class CaregiverAlarmTest {
    private fun mx(v: String): Instant = LocalDateTime.parse(v).atZone(REMINDER_ZONE).toInstant()
    private fun intake(id: Int, time: String, taken: Boolean) = IntakeDto(
        id = id, medicationId = 1, scheduleId = id, name = "SINTETICO Med", time = time,
        scheduledFor = "2026-11-22 $time:00", status = if (taken) "taken" else "pending",
        confirmedAt = if (taken) "2026-11-22 $time:00" else null,
    )
    private fun today(vararg i: IntakeDto) = TodayResponse("2026-11-22", intakes = i.toList(), summary = IntakeSummary(0, 0, 0))
    private val w8 = CaregiverWatch(5, "Paciente SINTETICO", 1, "SINTETICO Med", "2026-11-22 08:00:00")

    @Test
    fun laAlarmaEsATMasTreinta() {
        assertEquals(mx("2026-11-22T08:30"), watchTriggerAt(w8))
    }

    @Test
    fun soloVigilaPendientesQueNoLleganATMasTreinta() {
        val t = today(intake(1, "08:00", false), intake(2, "12:00", true), intake(3, "13:00", false))
        val watches = watchesFrom(5, "Paciente SINTETICO", t, mx("2026-11-22T10:00"))
        assertEquals(listOf(3), watches.map { it.intakeId })
    }

    @Test
    fun eligeLaMasCercanaQueNoHayaSonado() {
        val w13 = w8.copy(intakeId = 3, scheduledFor = "2026-11-22 13:00:00")
        assertEquals(w8, nextCaregiverWatch(listOf(w13, w8), mx("2026-11-22T07:00"), emptySet()))
        assertEquals(w13, nextCaregiverWatch(listOf(w13, w8), mx("2026-11-22T07:00"), setOf(w8.key)))
        assertNull(nextCaregiverWatch(listOf(w8), mx("2026-11-22T09:00"), emptySet()))
    }

    @Test
    fun avisaSoloSiSigueSinConfirmar() {
        assertEquals(CaregiverAlert.UNCONFIRMED, caregiverAlertFor(w8, null, today(intake(1, "08:00", false))))
        assertEquals(CaregiverAlert.NONE, caregiverAlertFor(w8, null, today(intake(1, "08:00", true))))
    }

    @Test
    fun sinConexionPideRevisarYSinVinculoNoSuena() {
        assertEquals(CaregiverAlert.CHECK_MANUALLY, caregiverAlertFor(w8, null, null))
        assertEquals(CaregiverAlert.CHECK_MANUALLY, caregiverAlertFor(w8, 500, null))
        assertEquals(CaregiverAlert.LINK_REVOKED, caregiverAlertFor(w8, 404, null))
    }
}
