package mx.cuidadosis.app.data.local

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Android 17 (API 37) exige el permiso ACCESS_LOCAL_NETWORK para conectarse a direcciones
 * de la red local, como 10.0.2.2 (emulador) o la IP de la PC con XAMPP.
 */
object LocalNetwork {
    const val PERMISSION = "android.permission.ACCESS_LOCAL_NETWORK"
    private const val API_LOCAL_NETWORK_PERMISSION = 37

    fun needsPermission(context: Context, baseUrl: String): Boolean =
        Build.VERSION.SDK_INT >= API_LOCAL_NETWORK_PERMISSION &&
            isLocalHost(baseUrl.toHttpUrlOrNull()?.host) &&
            ContextCompat.checkSelfPermission(context, PERMISSION) != PackageManager.PERMISSION_GRANTED

    // IPv4 privadas (10/8, 172.16/12, 192.168/16, 169.254/16) y nombres .local
    fun isLocalHost(host: String?): Boolean {
        if (host == null) return false
        if (host.endsWith(".local", ignoreCase = true)) return true
        val parts = host.split(".").map { it.toIntOrNull() ?: return false }
        if (parts.size != 4 || parts.any { it !in 0..255 }) return false
        val (a, b) = parts
        return a == 10 || (a == 172 && b in 16..31) || (a == 192 && b == 168) || (a == 169 && b == 254)
    }
}
