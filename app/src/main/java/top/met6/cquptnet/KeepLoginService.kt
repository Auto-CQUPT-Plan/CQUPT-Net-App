package top.met6.cquptnet

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*

class KeepLoginService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var worker: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private lateinit var settings: SettingsManager

    override fun onCreate() {
        super.onCreate()
        settings = SettingsManager(this)
        if (Build.VERSION.SDK_INT >= 26) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL, "登录保持", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == STOP || !settings.keepLogin) {
            settings.keepLogin = false
            stopSelf()
            return START_NOT_STICKY
        }
        ServiceCompat.startForeground(this, 1, notification("每分钟检查，断线后自动登录"),
            if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0)
        if (worker?.isActive != true) {
            wakeLock = getSystemService(PowerManager::class.java)
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "$packageName:keepLogin")
                .apply { setReferenceCounted(false); acquire(120_000L) }
            worker = scope.launch {
                while (isActive && settings.keepLogin) {
                    wakeLock?.acquire(120_000L)
                    val started = SystemClock.elapsedRealtime()
                    val status = try {
                        when {
                            settings.studentId.isBlank() || settings.password.isBlank() -> "请在设置中填写账号和密码"
                            !WifiNetworkManager(this@KeepLoginService).isConnected() -> "等待连接 WiFi"
                            else -> {
                                val sdk = CQUPTNetSDK(this@KeepLoginService, settings.studentId, settings.password, settings.isp)
                                if (sdk.checkStatus()) "已登录，每分钟检查"
                                else {
                                    ensureActive()
                                    if (!settings.keepLogin) break
                                    val result = sdk.login()
                                    if (result.result == "1" || result.msg.contains("已登录")) "已自动登录，每分钟检查"
                                    else "登录未成功，1 分钟后重试"
                                }
                            }
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        "暂时无法检查，1 分钟后重试"
                    }
                    ensureActive()
                    getSystemService(NotificationManager::class.java).notify(1, notification(status))
                    delay((60_000L - (SystemClock.elapsedRealtime() - started)).coerceAtLeast(1_000L))
                }
            }
        }
        return START_STICKY
    }

    private fun notification(status: String) = NotificationCompat.Builder(this, CHANNEL)
        .setSmallIcon(android.R.drawable.stat_notify_sync)
        .setContentTitle("校园网登录保持")
        .setContentText(status)
        .setContentIntent(PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
        .addAction(0, "关闭登录保持", PendingIntent.getService(this, 1,
            Intent(this, KeepLoginService::class.java).setAction(STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .build()

    override fun onDestroy() {
        scope.cancel()
        wakeLock?.let { if (it.isHeld) it.release() }
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL = "keep_login"
        private const val STOP = "top.met6.cquptnet.STOP_KEEP_LOGIN"

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, KeepLoginService::class.java))
        }

        fun stop(context: Context) {
            SettingsManager(context).keepLogin = false
            context.stopService(Intent(context, KeepLoginService::class.java))
        }
    }
}
