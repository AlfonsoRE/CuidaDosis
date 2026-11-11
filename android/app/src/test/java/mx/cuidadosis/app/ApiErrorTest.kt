package mx.cuidadosis.app

import mx.cuidadosis.app.data.remote.parseApiError
import mx.cuidadosis.app.ui.medications.formatTime
import mx.cuidadosis.app.ui.medications.validateMedication
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiErrorTest {
    @Test
    fun leeMensajeYDetallesDeLaApi() {
        val body = """{"error":"Datos inválidos.","details":{"email":"No es un correo válido."}}"""
        val error = parseApiError(422, body)
        assertEquals("Datos inválidos.", error.message)
        assertEquals(mapOf("email" to "No es un correo válido."), error.fieldErrors)
        assertEquals(422, error.code)
    }

    @Test
    fun leeErrorSinDetalles() {
        val error = parseApiError(401, """{"error":"Correo o contraseña incorrectos."}""")
        assertEquals("Correo o contraseña incorrectos.", error.message)
        assertTrue(error.fieldErrors.isEmpty())
    }

    @Test
    fun cuerpoNoJsonDaMensajeGenerico() {
        val error = parseApiError(500, "<html>Error</html>")
        assertEquals("Error del servidor (código 500).", error.message)
    }

    @Test
    fun cuerpoVacioDaMensajeGenerico() {
        assertEquals("Error del servidor (código 404).", parseApiError(404, null).message)
    }

    @Test
    fun formateaHoraConCeros() {
        assertEquals("08:05", formatTime(8, 5))
        assertEquals("23:59", formatTime(23, 59))
    }

    @Test
    fun validaMedicamentoSinInterpretarContenido() {
        assertTrue(validateMedication("Medicamento SINTETICO", "texto libre", listOf("08:00")).isEmpty())
        val errors = validateMedication("  ", "", emptyList())
        assertEquals(setOf("name", "dose", "times"), errors.keys)
        assertTrue(validateMedication("a".repeat(101), "x", listOf("08:00")).containsKey("name"))
    }
}
