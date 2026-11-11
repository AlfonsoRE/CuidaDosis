package mx.cuidadosis.app

import mx.cuidadosis.app.data.local.BaseUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BaseUrlTest {
    @Test
    fun agregaDiagonalFinal() {
        assertEquals(
            "http://10.0.2.2/CuidaDosis/api/public/",
            BaseUrl.normalize("  http://10.0.2.2/CuidaDosis/api/public "),
        )
    }

    @Test
    fun conservaUrlValida() {
        assertEquals(
            "http://192.168.1.50/CuidaDosis/api/public/",
            BaseUrl.normalize("http://192.168.1.50/CuidaDosis/api/public/"),
        )
    }

    @Test
    fun aceptaHttps() {
        assertEquals("https://ejemplo.test/api/", BaseUrl.normalize("https://ejemplo.test/api"))
    }

    @Test
    fun rechazaSinEsquema() {
        assertNull(BaseUrl.normalize("10.0.2.2/CuidaDosis/api/public/"))
    }

    @Test
    fun rechazaOtrosEsquemas() {
        assertNull(BaseUrl.normalize("ftp://10.0.2.2/"))
    }

    @Test
    fun rechazaQuery() {
        assertNull(BaseUrl.normalize("http://10.0.2.2/api/public/?x=1"))
    }

    @Test
    fun rechazaVacia() {
        assertNull(BaseUrl.normalize("   "))
    }
}
