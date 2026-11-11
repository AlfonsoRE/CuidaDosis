package mx.cuidadosis.app

import mx.cuidadosis.app.data.local.LocalNetwork
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalNetworkTest {
    @Test
    fun detectaDireccionesDeRedLocal() {
        assertTrue(LocalNetwork.isLocalHost("10.0.2.2"))
        assertTrue(LocalNetwork.isLocalHost("192.168.1.50"))
        assertTrue(LocalNetwork.isLocalHost("172.16.0.1"))
        assertTrue(LocalNetwork.isLocalHost("172.31.255.1"))
        assertTrue(LocalNetwork.isLocalHost("mi-pc.local"))
    }

    @Test
    fun ignoraDireccionesPublicas() {
        assertFalse(LocalNetwork.isLocalHost("8.8.8.8"))
        assertFalse(LocalNetwork.isLocalHost("172.32.0.1"))
        assertFalse(LocalNetwork.isLocalHost("ejemplo.test"))
        assertFalse(LocalNetwork.isLocalHost("999.1.1.1"))
        assertFalse(LocalNetwork.isLocalHost(null))
    }
}
