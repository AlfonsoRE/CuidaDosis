package mx.cuidadosis.app

import mx.cuidadosis.app.data.linkErrorMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Mensajes al cuidador ante código vencido o demasiados intentos (RF-08; G-01, G-05)
class LinkMessagesTest {
    @Test
    fun codigoVencidoExplicaLosDiezMinutos() {
        assertTrue(linkErrorMessage(410, "x").contains("venció"))
        assertTrue(linkErrorMessage(410, "x").contains("10 minutos"))
    }

    @Test
    fun demasiadosIntentosPideEsperar() {
        assertTrue(linkErrorMessage(429, "x").contains("Espera 10 minutos"))
    }

    @Test
    fun otrosErroresUsanElMensajeDelServidor() {
        assertEquals("Sin conexión", linkErrorMessage(null, "Sin conexión"))
        assertEquals("Datos inválidos.", linkErrorMessage(422, "Datos inválidos."))
    }
}
