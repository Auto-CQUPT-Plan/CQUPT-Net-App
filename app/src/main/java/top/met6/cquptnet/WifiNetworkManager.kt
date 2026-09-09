package top.met6.cquptnet

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.net.NetworkRequest
import android.os.Build
import android.location.LocationManager
import androidx.core.location.LocationManagerCompat
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withTimeoutOrNull

sealed interface WifiState {
    data object NotConnected : WifiState
    data class Connected(val ssid: String?) : WifiState
}

class WifiNetworkManager(context: Context) {
    private val appContext = context.applicationContext
    private val connectivityManager =
        context.applicationContext.getSystemService(ConnectivityManager::class.java)

    fun isLocationEnabled(): Boolean = LocationManagerCompat.isLocationEnabled(
        appContext.getSystemService(LocationManager::class.java)
    )

    @Suppress("DEPRECATION")
    suspend fun currentState(): WifiState {
        val network = currentWifiNetwork() ?: return WifiState.NotConnected
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            val ssid = try {
                appContext.getSystemService(WifiManager::class.java).connectionInfo?.ssid
            } catch (_: SecurityException) { null }
            return WifiState.Connected(normalizeSsid(ssid))
        }
        // Synchronous capabilities deliberately redact location-sensitive WifiInfo.
        val updates = Channel<WifiState>(Channel.CONFLATED)
        val callback = object : ConnectivityManager.NetworkCallback(FLAG_INCLUDE_LOCATION_INFO) {
            override fun onCapabilitiesChanged(n: Network, capabilities: NetworkCapabilities) {
                if (n == network) {
                    updates.trySend(WifiState.Connected(normalizeSsid((capabilities.transportInfo as? WifiInfo)?.ssid)))
                }
            }

            override fun onLost(n: Network) {
                if (n == network) updates.trySend(WifiState.NotConnected)
            }
        }
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        var registered = false
        return try {
            connectivityManager.registerNetworkCallback(request, callback)
            registered = true
            withTimeoutOrNull(3000) { updates.receive() } ?: WifiState.Connected(null)
        } catch (_: SecurityException) {
            WifiState.Connected(null)
        } finally {
            if (registered) connectivityManager.unregisterNetworkCallback(callback)
            updates.close()
        }
    }

    fun requireWifiNetwork(): Network = currentWifiNetwork()
        ?: throw IllegalStateException("未连接 WiFi，API 请求已取消")

    private fun currentWifiNetwork(): Network? {
        return connectivityManager.allNetworks.firstOrNull { network ->
            connectivityManager.getNetworkCapabilities(network)
                ?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        }
    }
}

internal fun normalizeSsid(ssid: String?): String? = ssid
    ?.removeSurrounding("\"")
    ?.takeUnless { it.isBlank() || it == "<unknown ssid>" }
