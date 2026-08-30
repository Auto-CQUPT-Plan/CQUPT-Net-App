package top.met6.cquptnet.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import top.met6.cquptnet.WifiNetworkManager
import top.met6.cquptnet.WifiState

class WifiRequestGate internal constructor(
    private val context: Context,
    private val wifiNetworkManager: WifiNetworkManager
) {
    var warningSsid by mutableStateOf<String?>(null)
        private set

    private var pendingAction: (() -> Unit)? = null
    internal var requestPermission: (String) -> Unit = {}

    fun run(action: () -> Unit) {
        val permission = wifiPermission()
        if (context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
            verifyWifi(action)
        } else {
            pendingAction = action
            requestPermission(permission)
        }
    }

    internal fun onPermissionResult() {
        val action = pendingAction
        pendingAction = null
        action?.let(::verifyWifi)
    }

    fun continuePending() {
        val action = pendingAction
        clearWarning()
        action?.invoke()
    }

    fun cancelPending() = clearWarning()

    private fun verifyWifi(action: () -> Unit) {
        when (val wifiState = wifiNetworkManager.currentState()) {
            WifiState.NotConnected -> Toast.makeText(
                context,
                "请先连接 WiFi，API 请求不会使用移动数据",
                Toast.LENGTH_LONG
            ).show()

            is WifiState.Connected -> {
                if (wifiState.ssid?.contains("CQUPT", ignoreCase = true) == true) {
                    action()
                } else {
                    pendingAction = action
                    warningSsid = wifiState.ssid ?: "无法读取 WiFi 名称"
                }
            }
        }
    }

    private fun clearWarning() {
        warningSsid = null
        pendingAction = null
    }

    private fun wifiPermission(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.NEARBY_WIFI_DEVICES
        } else {
            Manifest.permission.ACCESS_FINE_LOCATION
        }
}

@Composable
fun rememberWifiRequestGate(): WifiRequestGate {
    val context = LocalContext.current
    val gate = remember(context) { WifiRequestGate(context, WifiNetworkManager(context)) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { gate.onPermissionResult() }
    gate.requestPermission = permissionLauncher::launch
    return gate
}
