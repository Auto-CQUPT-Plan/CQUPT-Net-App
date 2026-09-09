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
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import top.met6.cquptnet.WifiNetworkManager
import top.met6.cquptnet.WifiState

class WifiRequestGate internal constructor(
    private val context: Context,
    private val wifiNetworkManager: WifiNetworkManager,
    private val scope: CoroutineScope
) {
    var warningSsid by mutableStateOf<String?>(null)
        private set

    private var pendingAction: (() -> Unit)? = null
    private var verificationJob: Job? = null
    internal var requestPermissions: (Array<String>) -> Unit = {}

    fun run(action: () -> Unit) {
        val permissions = wifiPermissions()
        if (permissions.all { context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }) {
            verifyWifi(action)
        } else {
            pendingAction = action
            requestPermissions(permissions)
        }
    }

    internal fun onPermissionResult(grants: Map<String, Boolean>) {
        val action = pendingAction
        pendingAction = null
        if (wifiPermissions().all { grants[it] == true || context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }) {
            action?.let(::verifyWifi)
        } else {
            Toast.makeText(
                context,
                "需要附近设备和精确位置权限才能读取 WiFi 名称",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun continuePending() {
        val action = pendingAction
        clearWarning()
        action?.invoke()
    }

    fun cancelPending() = clearWarning()

    private fun verifyWifi(action: () -> Unit) {
        verificationJob?.cancel()
        verificationJob = scope.launch {
            if (!wifiNetworkManager.isLocationEnabled()) {
                Toast.makeText(context, "请开启系统定位服务后重试，以便读取 WiFi 名称", Toast.LENGTH_LONG).show()
                return@launch
            }
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
    }

    private fun clearWarning() {
        warningSsid = null
        pendingAction = null
    }

    private fun wifiPermissions(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.NEARBY_WIFI_DEVICES,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
}

@Composable
fun rememberWifiRequestGate(): WifiRequestGate {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val gate = remember(context, scope) { WifiRequestGate(context, WifiNetworkManager(context), scope) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants -> gate.onPermissionResult(grants) }
    gate.requestPermissions = permissionLauncher::launch
    return gate
}
