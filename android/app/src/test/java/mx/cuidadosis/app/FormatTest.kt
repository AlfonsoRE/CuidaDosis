package mx.cuidadosis.app

import mx.cuidadosis.app.data.normalizeInviteCode
import mx.cuidadosis.app.ui.common.formatApiDate
import mx.cuidadosis.app.ui.common.formatApiDateTime
import mx.cuidadosis.app.ui.common.formatApiTime
import mx.cuidadosis.app.ui.common.isTimePast
import mx.cuidadosis.app.ui.common.summaryText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class FormatTest {
    @Test
    fun formateaFechasDeLaApi() {
        assertEquals("13/11/2026 10:15", formatApiDateTime("2026-11-13 10:15:57"))
        assertEquals("08:03", formatApiTime("2026-11-11 08:03:00"))
        assertEquals("11/11/2026", formatApiDate("2026-11-11"))
    }

    @Test
    fun fechaInvalidaOVaciaNoTruena() {
        assertEquals("", formatApiDateTime(null))
        assertEquals("", formatApiTime("no es fecha"))
        assertEquals("", formatApiDate(""))
    }

    @Test
    fun detectaHoraPasada() {
        val now = LocalTime.of(14, 30)
        assertTrue(isTimePast("08:00", now))
        assertFalse(isTimePast("20:00", now))
        assertFalse(isTimePast("xx", now))
    }

    @Test
    fun textoDeResumen() {
        assertEquals("1 de 3 tomas registradas", summaryText(1, 3))
        assertEquals("Sin tomas programadas hoy", summaryText(0, 0))
        assertEquals("0 de 1 toma registrada", summaryText(0, 1))
    }

    @Test
    fun normalizaCodigoDeInvitacion() {
        assertEquals("ABCD2345", normalizeInviteCode(" abcd-2345 "))
        assertEquals("SINT0002", normalizeInviteCode("sint 0002"))
    }
}
