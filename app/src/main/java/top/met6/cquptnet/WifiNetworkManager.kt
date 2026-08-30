package top.met6.cquptnet

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo

sealed interface WifiState {
    data object NotConnected : WifiState
    data class Connected(val ssid: String?) : WifiState
}

class WifiNetworkManager(context: Context) {
    private val connectivityManager =
        context.applicationContext.getSystemService(ConnectivityManager::class.java)

    fun currentState(): WifiState {
        val network = currentWifiNetwork() ?: return WifiState.NotConnected
        val capabilities = connectivityManager.getNetworkCapabilities(network)
            ?: return WifiState.NotConnected
        val wifiInfo = capabilities.transportInfo as? WifiInfo
        val ssid = wifiInfo?.ssid
            ?.removeSurrounding("\"")
            ?.takeUnless { it.isBlank() || it == "<unknown ssid>" }
        return WifiState.Connected(ssid)
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
