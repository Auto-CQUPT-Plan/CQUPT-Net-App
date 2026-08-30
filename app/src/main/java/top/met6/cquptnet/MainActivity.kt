package top.met6.cquptnet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import top.met6.cquptnet.ui.MainScreen
import top.met6.cquptnet.ui.theme.CQUPTNetTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settings = SettingsManager(this)

        setContent {
            CQUPTNetTheme {
                MainScreen(settings)
            }
        }
    }
}
