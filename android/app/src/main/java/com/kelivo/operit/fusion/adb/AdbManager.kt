package com.kelivo.operit.fusion.adb

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * ADB 手机自动化管理器 - 移植自 Operit
 *
 * 功能：
 * - 无线 ADB 连接管理
 * - 屏幕点击、滑动、截图
 * - APP 启停控制
 * - 文本输入、按键模拟
 */
class AdbManager private constructor(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("fusion_adb", Context.MODE_PRIVATE)

    private var connectedHost: String? = null
    private var connectedPort: Int = 0
    private var isConnected: Boolean = false

    companion object {
        private const val TAG = "AdbManager"
        private const val DEFAULT_PORT = 5555

        @Volatile
        private var instance: AdbManager? = null

        fun getInstance(context: Context): AdbManager {
            return instance ?: synchronized(this) {
                instance ?: AdbManager(context.applicationContext).also { instance = it }
            }
        }
    }

    /**
     * 连接到无线 ADB
     */
    suspend fun connect(host: String = "127.0.0.1", port: Int = DEFAULT_PORT): Map<String, Any> = withContext(Dispatchers.IO) {
        try {
            val process = Runtime.getRuntime().exec(arrayOf("adb", "connect", "$host:$port"))
            val exitCode = process.waitFor()
            val output = process.inputStream.bufferedReader().readText()

            isConnected = exitCode == 0 && output.contains("connected")
            connectedHost = host
            connectedPort = port

            mapOf(
                "success" to isConnected,
                "message" to if (isConnected) "ADB连接成功" else "ADB连接失败: $output",
                "host" to host,
                "port" to port
            )
        } catch (e: Exception) {
            mapOf(
                "success" to false,
                "error" to (e.message ?: "ADB连接异常")
            )
        }
    }

    /**
     * 断开连接
     */
    suspend fun disconnect(): Map<String, Any> = withContext(Dispatchers.IO) {
        try {
            connectedHost?.let { host ->
                Runtime.getRuntime().exec(arrayOf("adb", "disconnect", "$host:$connectedPort")).waitFor()
            }
            isConnected = false
            connectedHost = null

            mapOf("success" to true, "message" to "已断开连接")
        } catch (e: Exception) {
            mapOf("success" to false, "error" to (e.message ?: "断开失败"))
        }
    }

    /**
     * 检查连接状态
     */
    fun isConnected(): Boolean = isConnected

    /**
     * 执行 ADB 命令
     */
    suspend fun executeCommand(command: String): Map<String, Any> = withContext(Dispatchers.IO) {
        try {
            val fullCommand = if (connectedHost != null) {
                "adb -s ${connectedHost}:$connectedPort shell $command"
            } else {
                "adb shell $command"
            }

            val process = Runtime.getRuntime().exec(fullCommand)
            val output = process.inputStream.bufferedReader().readText()
            val error = process.errorStream.bufferedReader().readText()
            val exitCode = process.waitFor()

            mapOf(
                "success" to (exitCode == 0),
                "output" to output,
                "error" to error,
                "exitCode" to exitCode
            )
        } catch (e: Exception) {
            mapOf("success" to false, "error" to (e.message ?: "Command failed"))
        }
    }

    /**
     * 点击屏幕
     */
    suspend fun tap(x: Int, y: Int): Map<String, Any> = withContext(Dispatchers.IO) {
        executeCommand("input tap $x $y")
    }

    /**
     * 滑动屏幕
     */
    suspend fun swipe(x1: Int, y1: Int, x2: Int, y2: Int, duration: Int = 300): Map<String, Any> = withContext(Dispatchers.IO) {
        executeCommand("input swipe $x1 $y1 $x2 $y2 $duration")
    }

    /**
     * 截图
     */
    suspend fun screenshot(): String? = withContext(Dispatchers.IO) {
        try {
            val timestamp = System.currentTimeMillis()
            val localFile = File(context.cacheDir, "screenshot_$timestamp.png")
            val remotePath = "/sdcard/screenshot_$timestamp.png"

            // 截图到设备
            executeCommand("screencap -p $remotePath")

            // 拉到本地
            val pullCmd = if (connectedHost != null) {
                "adb -s ${connectedHost}:$connectedPort pull $remotePath ${localFile.absolutePath}"
            } else {
                "adb pull $remotePath ${localFile.absolutePath}"
            }

            Runtime.getRuntime().exec(pullCmd).waitFor()

            if (localFile.exists()) {
                localFile.absolutePath
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 获取屏幕尺寸
     */
    suspend fun getScreenSize(): Map<String, Any> = withContext(Dispatchers.IO) {
        val result = executeCommand("wm size")
        val output = result["output"] as? String ?: ""

        val regex = Regex("(\\d+)x(\\d+)")
        val match = regex.find(output)

        if (match != null) {
            mapOf(
                "width" to match.groupValues[1].toInt(),
                "height" to match.groupValues[2].toInt()
            )
        } else {
            mapOf("width" to 0, "height" to 0)
        }
    }

    /**
     * 启动 APP
     */
    suspend fun startApp(packageName: String, activity: String? = null): Map<String, Any> = withContext(Dispatchers.IO) {
        val cmd = if (activity != null) {
            "am start -n $packageName/$activity"
        } else {
            "monkey -p $packageName -c android.intent.category.LAUNCHER 1"
        }
        executeCommand(cmd)
    }

    /**
     * 停止 APP
     */
    suspend fun stopApp(packageName: String): Map<String, Any> = withContext(Dispatchers.IO) {
        executeCommand("am force-stop $packageName")
    }

    /**
     * 输入文本
     */
    suspend fun inputText(text: String): Map<String, Any> = withContext(Dispatchers.IO) {
        executeCommand("input text \"$text\"")
    }

    /**
     * 按键
     */
    suspend fun pressKey(keyCode: Int): Map<String, Any> = withContext(Dispatchers.IO) {
        executeCommand("input keyevent $keyCode")
    }

    /**
     * 获取已安装应用列表
     */
    suspend fun getInstalledApps(): List<Map<String, String>> = withContext(Dispatchers.IO) {
        val result = executeCommand("pm list packages -3")
        val output = result["output"] as? String ?: ""

        output.lines()
            .filter { it.startsWith("package:") }
            .map { line ->
                val packageName = line.removePrefix("package:").trim()
                mapOf("packageName" to packageName)
            }
    }

    fun dispose() {
        isConnected = false
        connectedHost = null
    }
}
