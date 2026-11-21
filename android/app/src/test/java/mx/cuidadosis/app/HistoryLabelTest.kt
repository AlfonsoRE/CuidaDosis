package mx.cuidadosis.app

import mx.cuidadosis.app.ui.common.historyDayLabel
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

// Encabezados de día del historial de 7 días (RF-15)
class HistoryLabelTest {
    private val today = LocalDate.parse("2026-11-22")

    @Test
    fun hoyYAyer() {
        assertEquals("Hoy, 22/11/2026", historyDayLabel("2026-11-22", today))
        assertEquals("Ayer, 21/11/2026", historyDayLabel("2026-11-21", today))
    }

    @Test
    fun diasAnterioresConFecha() {
        assertEquals("16/11/2026", historyDayLabel("2026-11-16", today))
    }

    @Test
    fun fechaInvalidaSeMuestraTalCual() {
        assertEquals("no-es-fecha", historyDayLabel("no-es-fecha", today))
    }
}
