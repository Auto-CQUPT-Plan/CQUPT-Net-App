package top.met6.cquptnet.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import top.met6.cquptnet.WifiNetworkManager

class WifiRequestGate internal constructor(private val context: Context) {
    fun run(action: () -> Unit) {
        if (WifiNetworkManager(context).isConnected()) action()
        else Toast.makeText(context, "请先连接 WiFi，API 请求不会使用移动数据", Toast.LENGTH_LONG).show()
    }
}

@Composable
fun rememberWifiRequestGate(): WifiRequestGate {
    val context = LocalContext.current
    return remember(context) { WifiRequestGate(context) }
}
