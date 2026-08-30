package top.met6.cquptnet

import android.content.Context
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.util.regex.Pattern
import kotlin.random.Random

data class AuthResult(val result: String, val msg: String, val retCode: Int)
data class UnbindResult(val result: String, val msg: String)

class CQUPTNetSDK(
    context: Context,
    private val stuId: String,
    private val password: String,
    private val isp: String = "xyw"
) {
    private val wifiNetworkManager = WifiNetworkManager(context)

    private fun wifiClient(): OkHttpClient = OkHttpClient.Builder()
        .socketFactory(wifiNetworkManager.requireWifiNetwork().socketFactory)
        .followRedirects(false)
        .build()

    private val HOST = "192.168.200.2:801"
    private val REFERER = "http://192.168.200.2/"
    private val REQUEST_URL = "http://192.168.200.2:801/eportal/"

    var ipAddr: String = ""
    var isLoggedIn: Boolean = false

    suspend fun checkStatus(): Boolean {
        val request = Request.Builder()
            .url(REFERER)
            .header("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 18_7 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/26.4 Mobile/15E148 Safari/604.1")
            .build()

        return try {
            wifiClient().newCall(request).execute().use { response ->
                val text = response.body?.string() ?: ""
                isLoggedIn = text.contains("<title>注销页</title>")
                
                val patterns = listOf("v4ip\\s*=\\s*['\"]([^'\"]+)['\"]", "v46ip\\s*=\\s*['\"]([^'\"]+)['\"]")
                for (p in patterns) {
                    val pattern = Pattern.compile(p)
                    val matcher = pattern.matcher(text)
                    if (matcher.find()) {
                        val ip = matcher.group(1)?.trim()?.trimEnd('.')
                        if (!ip.isNullOrEmpty() && ip != "0.0.0.0" && ip != "000.000.000.000") {
                            ipAddr = ip
                            break
                        }
                    }
                }
                isLoggedIn
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun login(): AuthResult {
        checkStatus()
        if (isLoggedIn) return AuthResult("1", "当前设备已登录", 0)
        if (ipAddr.isEmpty()) return AuthResult("0", "未能获取到IPv4地址", -1)

        val device = 1 // Mobile
        val mac = "000000000000"

        val url = REQUEST_URL.toHttpUrlOrNull()!!.newBuilder()
            .addQueryParameter("c", "Portal")
            .addQueryParameter("a", "login")
            .addQueryParameter("callback", "dr1003")
            .addQueryParameter("login_method", "1")
            .addQueryParameter("user_account", ",$device,$stuId@$isp")
            .addQueryParameter("user_password", password)
            .addQueryParameter("wlan_user_ip", ipAddr)
            .addQueryParameter("wlan_user_mac", mac)
            .addQueryParameter("jsVersion", "3.3.3")
            .build()

        val request = Request.Builder()
            .url(url)
            .header("Host", HOST)
            .header("Referer", REFERER)
            .header("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 18_7 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/26.4 Mobile/15E148 Safari/604.1")
            .build()

        return try {
            wifiClient().newCall(request).execute().use { response ->
                val text = response.body?.string() ?: ""
                val resultDict = parseJsonp(text)
                AuthResult(
                    resultDict["result"] ?: "",
                    resultDict["msg"] ?: "",
                    resultDict["ret_code"]?.toIntOrNull() ?: 0
                )
            }
        } catch (e: Exception) {
            AuthResult("0", e.message ?: "Unknown error", -1)
        }
    }

    suspend fun logout(): UnbindResult {
        checkStatus()
        if (!isLoggedIn) return UnbindResult("1", "当前设备未登录")
        if (ipAddr.isEmpty()) return UnbindResult("0", "未能获取到IPv4地址")

        val mac = "000000000000"
        val url = REQUEST_URL.toHttpUrlOrNull()!!.newBuilder()
            .addQueryParameter("c", "Portal")
            .addQueryParameter("a", "unbind_mac")
            .addQueryParameter("callback", "dr1002")
            .addQueryParameter("user_account", "$stuId@$isp")
            .addQueryParameter("wlan_user_mac", mac)
            .addQueryParameter("wlan_user_ip", ipAddr)
            .addQueryParameter("jsVersion", "3.3.3")
            .addQueryParameter("v", Random.nextInt(1000, 9999).toString())
            .build()

        val request = Request.Builder()
            .url(url)
            .header("Host", HOST)
            .header("Referer", REFERER)
            .header("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 18_7 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/26.4 Mobile/15E148 Safari/604.1")
            .build()

        return try {
            wifiClient().newCall(request).execute().use { response ->
                val text = response.body?.string() ?: ""
                val resultDict = parseJsonp(text)
                UnbindResult(
                    resultDict["result"] ?: "",
                    resultDict["msg"] ?: ""
                )
            }
        } catch (e: Exception) {
            UnbindResult("0", e.message ?: "Unknown error")
        }
    }

    private fun parseJsonp(text: String): Map<String, String> {
        val start = text.indexOf("(")
        val end = text.lastIndexOf(")")
        if (start != -1 && end != -1) {
            val json = text.substring(start + 1, end)
            // Simple regex based JSON parser for this specific case
            val map = mutableMapOf<String, String>()
            val pattern = Pattern.compile("\"(\\w+)\":\\s*\"?([^\",}]+)\"?")
            val matcher = pattern.matcher(json)
            while (matcher.find()) {
                map[matcher.group(1)!!] = matcher.group(2)!!
            }
            return map
        }
        return emptyMap()
    }
}
