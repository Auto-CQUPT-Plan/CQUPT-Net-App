package top.met6.cquptnet.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.met6.cquptnet.SettingsManager
import top.met6.cquptnet.ui.components.ActionButton
import top.met6.cquptnet.ui.components.SettingsDialog
import top.met6.cquptnet.ui.components.StatusCard
import top.met6.cquptnet.KeepLoginService
import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Switch
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.DisposableEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(settings: SettingsManager) {
    val screenState = rememberMainScreenState(settings)
    val wifiGate = rememberWifiRequestGate()
    val uiState = screenState.uiState
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var keepLogin by remember { mutableStateOf(settings.keepLogin) }
    val notifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    fun setKeepLogin(enabled: Boolean, requestNotification: Boolean = true) {
        if (enabled && (settings.studentId.isBlank() || settings.password.isBlank())) {
            Toast.makeText(context, "请先在设置中填写账号和密码", Toast.LENGTH_LONG).show()
            return
        }
        settings.keepLogin = enabled
        try {
            if (enabled) KeepLoginService.start(context) else KeepLoginService.stop(context)
            keepLogin = enabled
            if (enabled && requestNotification && Build.VERSION.SDK_INT >= 33 &&
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } catch (_: Exception) {
            settings.keepLogin = false
            keepLogin = false
            Toast.makeText(context, "登录保持启动失败，请重试", Toast.LENGTH_LONG).show()
        }
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                keepLogin = settings.keepLogin
                if (keepLogin) setKeepLogin(true, requestNotification = false)
                wifiGate.run(screenState::refresh)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    var showSettings by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("重邮校园网", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 12.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("登录保持", color = Color.White)
                        Text("每分钟检查，断线后自动登录", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                    }
                    Switch(checked = keepLogin, onCheckedChange = { setKeepLogin(it) })
                }
                TextButton(onClick = { wifiGate.run(screenState::refresh) }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("刷新状态")
                }
            }
        },
        modifier = Modifier.background(Brush.verticalGradient(listOf(Color(0xFF1A237E), Color(0xFF121212)))),
        containerColor = Color.Transparent
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                StatusCard(uiState.status, uiState.ipAddress, uiState.isLoading)
                Spacer(modifier = Modifier.height(48.dp))
                ActionButton(
                    text = "登录",
                    icon = Icons.AutoMirrored.Filled.Login,
                    color = Color(0xFF4CAF50),
                    onClick = { wifiGate.run(screenState::login) }
                )
                Spacer(modifier = Modifier.height(16.dp))
                ActionButton(
                    text = "登出",
                    icon = Icons.AutoMirrored.Filled.Logout,
                    color = Color(0xFFF44336),
                    onClick = { setKeepLogin(false); wifiGate.run(screenState::logout) }
                )
            }

        }
    }

    if (showSettings) {
        SettingsDialog(
            settings = screenState.settings,
            onDismiss = { showSettings = false },
            onSave = { wifiGate.run(screenState::refresh) }
        )
    }

}
