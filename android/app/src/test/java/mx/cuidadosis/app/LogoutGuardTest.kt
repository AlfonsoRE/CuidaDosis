package mx.cuidadosis.app

import mx.cuidadosis.app.data.canLogout
import mx.cuidadosis.app.data.pendingLogoutMessage
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Cerrar sesión no debe perder confirmaciones hechas sin red (Fase 8)
class LogoutGuardTest {
    @Test
    fun sinPendientesSePuedeSalir() {
        assertTrue(canLogout(0))
    }

    @Test
    fun conPendientesNoSePuedeSalir() {
        assertFalse(canLogout(1))
        assertFalse(canLogout(3))
    }

    @Test
    fun elMensajeDiceCuantasYQueHacer() {
        val one = pendingLogoutMessage(1)
        assertTrue(one.contains("1 confirmación que todavía no se envía"))
        assertTrue(one.contains("se perdería,"))
        val three = pendingLogoutMessage(3)
        assertTrue(three.contains("3 confirmaciones"))
        assertTrue(three.contains("se perderían"))
        assertTrue(three.contains("Reintentar envío"))
    }
}
