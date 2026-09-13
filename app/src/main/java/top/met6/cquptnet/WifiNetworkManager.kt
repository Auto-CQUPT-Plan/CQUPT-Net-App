package top.met6.cquptnet

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities

class WifiNetworkManager(context: Context) {
    private val connectivityManager = context.applicationContext
        .getSystemService(ConnectivityManager::class.java)

    fun isConnected(): Boolean = currentWifiNetwork() != null

    fun requireWifiNetwork(): Network = currentWifiNetwork()
        ?: throw IllegalStateException("未连接 WiFi，API 请求已取消")

    private fun currentWifiNetwork(): Network? = connectivityManager.allNetworks.firstOrNull {
        connectivityManager.getNetworkCapabilities(it)
            ?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
    }
}
