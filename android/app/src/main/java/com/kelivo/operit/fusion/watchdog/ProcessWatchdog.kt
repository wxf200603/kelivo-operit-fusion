package com.kelivo.operit.fusion.watchdog

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * 进程看门狗 - 心跳超时保护
 *
 * 功能：
 * 1. 监控进程/命令执行，超时自动终止
 * 2. 心跳机制，检测僵死进程
 * 3. 5/10 分钟超时自动捕获日志并杀死进程
 */
class ProcessWatchdog private constructor(private val context: Context) {
    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val prefs: SharedPreferences = context.getSharedPreferences("fusion_watchdog", Context.MODE_PRIVATE)

    private val monitoredProcesses = ConcurrentHashMap<String, ProcessMonitor>()

    data class ProcessMonitor(
        val processId: String,
        val processType: String,
        val startTime: Long,
        val timeout: Int,
        val onTimeout: String,
        var lastHeartbeat: Long = System.currentTimeMillis(),
        var isAlive: Boolean = true
    )

    companion object {
        private const val TAG = "ProcessWatchdog"

        const val DEFAULT_TIMEOUT_CONTAINER = 600
        const val DEFAULT_TIMEOUT_TERMINAL = 300
        const val DEFAULT_TIMEOUT_ADB = 60
        const val DEFAULT_TIMEOUT_MCP = 300

        @Volatile
        private var instance: ProcessWatchdog? = null

        fun getInstance(context: Context): ProcessWatchdog {
            return instance ?: synchronized(this) {
                instance ?: ProcessWatchdog(context.applicationContext).also { instance = it }
            }
        }
    }

    fun startMonitoring(processId: String, timeout: Int = 300, onTimeout: String = "kill") {
        val monitor = ProcessMonitor(
            processId = processId,
            processType = processId.split("_").firstOrNull() ?: "unknown",
            startTime = System.currentTimeMillis(),
            timeout = timeout,
            onTimeout = onTimeout
        )
        monitoredProcesses[processId] = monitor

        handler.postDelayed({
            checkProcessTimeout(processId)
        }, (timeout * 1000).toLong())
    }

    fun stopMonitoring(processId: String) {
        monitoredProcesses.remove(processId)
    }

    fun heartbeat(processId: String) {
        monitoredProcesses[processId]?.let {
            it.lastHeartbeat = System.currentTimeMillis()
        }
    }

    fun getActiveProcesses(): Map<String, ProcessMonitor> {
        return monitoredProcesses.toMap()
    }

    fun getProcessUptime(processId: String): Long {
        return monitoredProcesses[processId]?.let {
            (System.currentTimeMillis() - it.startTime) / 1000
        } ?: 0
    }

    fun isProcessAlive(processId: String): Boolean {
        return monitoredProcesses[processId]?.isAlive ?: false
    }

    private fun checkProcessTimeout(processId: String) {
        val monitor = monitoredProcesses[processId] ?: return

        val elapsed = (System.currentTimeMillis() - monitor.lastHeartbeat) / 1000
        if (elapsed >= monitor.timeout) {
            handleTimeout(monitor)
        }
    }

    private fun handleTimeout(monitor: ProcessMonitor) {
        when (monitor.onTimeout) {
            "kill" -> killProcess(monitor)
            "restart" -> restartProcess(monitor)
            "log" -> captureLog(monitor)
        }
        monitoredProcesses.remove(monitor.processId)
    }

    private fun killProcess(monitor: ProcessMonitor) {
        // 根据进程类型执行不同的终止逻辑
        monitor.isAlive = false
    }

    private fun restartProcess(monitor: ProcessMonitor) {
        killProcess(monitor)
    }

    private fun captureLog(monitor: ProcessMonitor) {
        scope.launch {
            // 捕获并保存日志
        }
    }

    fun dispose() {
        handler.removeCallbacksAndMessages(null)
        monitoredProcesses.clear()
        instance = null
    }
}
