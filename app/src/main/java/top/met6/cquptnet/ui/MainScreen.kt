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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import top.met6.cquptnet.ui.components.UntrustedWifiDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(settings: SettingsManager) {
    val screenState = rememberMainScreenState(settings)
    val wifiGate = rememberWifiRequestGate()
    val uiState = screenState.uiState
    var showSettings by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        wifiGate.run(screenState::refresh)
    }

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
        containerColor = Color.Transparent
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF1A237E), Color(0xFF121212))))
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
                    onClick = { wifiGate.run(screenState::logout) }
                )
            }

            TextButton(
                onClick = { wifiGate.run(screenState::refresh) },
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = Color.White.copy(alpha = 0.6f))
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("刷新状态", fontSize = 14.sp)
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

    wifiGate.warningSsid?.let { ssid ->
        UntrustedWifiDialog(
            ssid = ssid,
            onContinue = wifiGate::continuePending,
            onCancel = wifiGate::cancelPending
        )
    }
}
