package top.met6.cquptnet

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("cqupt_net_prefs", Context.MODE_PRIVATE)

    var keepLogin: Boolean
        get() = prefs.getBoolean("keep_login", false)
        set(value) = prefs.edit().putBoolean("keep_login", value).apply()

    var studentId: String
        get() = prefs.getString("student_id", "") ?: ""
        set(value) = prefs.edit().putString("student_id", value).apply()

    var password: String
        get() = prefs.getString("password", "") ?: ""
        set(value) = prefs.edit().putString("password", value).apply()

    var isp: String
        get() = prefs.getString("isp", "xyw") ?: "xyw"
        set(value) = prefs.edit().putString("isp", value).apply()
}
