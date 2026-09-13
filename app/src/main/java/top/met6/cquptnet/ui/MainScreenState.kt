package top.met6.cquptnet.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.met6.cquptnet.CQUPTNetSDK
import top.met6.cquptnet.SettingsManager

data class MainUiState(
    val status: String = "未知",
    val ipAddress: String = "0.0.0.0",
    val isLoading: Boolean = false
)

class MainScreenState internal constructor(
    private val context: Context,
    val settings: SettingsManager,
    private val scope: CoroutineScope
) {
    var uiState by mutableStateOf(MainUiState())
        private set

    fun refresh() = execute { sdk ->
        val loggedIn = sdk.checkStatus()
        MainUiState(
            status = if (loggedIn) "已登录" else "未登录",
            ipAddress = sdk.ipAddr.ifEmpty { "0.0.0.0" }
        )
    }

    fun login() = execute { sdk ->
        val result = sdk.login()
        MainUiState(
            status = if (result.result == "1" || result.msg.contains("已登录")) "已登录" else "未登录",
            ipAddress = sdk.ipAddr.ifEmpty { "0.0.0.0" }
        )
    }

    fun logout() = execute { sdk ->
        val result = sdk.logout()
        MainUiState(
            status = if (result.result == "1") "未登录" else "已登录",
            ipAddress = sdk.ipAddr.ifEmpty { "0.0.0.0" }
        )
    }

    private fun execute(operation: suspend (CQUPTNetSDK) -> MainUiState) {
        if (uiState.isLoading) return
        scope.launch {
            uiState = uiState.copy(isLoading = true)
            val sdk = CQUPTNetSDK(context, settings.studentId, settings.password, settings.isp)
            try {
                uiState = withContext(Dispatchers.IO) { operation(sdk) }.copy(isLoading = false)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                uiState = MainUiState(status = "无法连接校园网，请重试")
            } finally {
                uiState = uiState.copy(isLoading = false)
            }
        }
    }
}

@Composable
fun rememberMainScreenState(settings: SettingsManager): MainScreenState {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    return remember(context, settings, scope) { MainScreenState(context, settings, scope) }
}
