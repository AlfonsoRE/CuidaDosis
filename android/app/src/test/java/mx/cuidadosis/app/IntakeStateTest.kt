package mx.cuidadosis.app

import mx.cuidadosis.app.data.IntakeState
import mx.cuidadosis.app.data.intakeState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.time.LocalDateTime

// Estado calculado (RF-11, ADR-005)
class IntakeStateTest {
    private val t = LocalDateTime.parse("2026-11-22T08:00")

    @Test
    fun pendienteHastaTMasTreinta() {
        assertEquals(IntakeState.PENDING, intakeState(t, false, t.minusMinutes(10)))
        assertEquals(IntakeState.PENDING, intakeState(t, false, t.plusMinutes(29)))
        assertEquals(IntakeState.PENDING, intakeState(t, false, t.plusMinutes(30)))
    }

    @Test
    fun sinConfirmarDespuesDeTMasTreinta() {
        assertEquals(IntakeState.UNCONFIRMED, intakeState(t, false, t.plusMinutes(30).plusSeconds(1)))
        assertEquals(IntakeState.UNCONFIRMED, intakeState(t, false, t.plusHours(5)))
    }

    @Test
    fun confirmadaSinImportarLaHora() {
        assertEquals(IntakeState.CONFIRMED, intakeState(t, true, t.minusHours(1)))
        assertEquals(IntakeState.CONFIRMED, intakeState(t, true, t.plusHours(3)))
    }

    @Test
    fun etiquetasNuncaDicenTomada() {
        assertEquals(
            listOf("Pendiente", "Confirmada por el paciente", "Sin confirmar"),
            IntakeState.entries.map { it.label },
        )
        IntakeState.entries.forEach { assertFalse(it.label.contains("Tomada")) }
    }
}
