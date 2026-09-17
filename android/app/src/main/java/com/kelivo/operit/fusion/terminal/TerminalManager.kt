package com.kelivo.operit.fusion.terminal

import android.content.Context
import com.kelivo.operit.fusion.watchdog.ProcessWatchdog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * 终端管理器 - 移植自 Operit
 *
 * 功能：
 * - 多会话终端管理
 * - 命令执行与输出回传
 * - 超时自动终止
 */
class TerminalManager private constructor(
    private val context: Context,
    private val watchdog: ProcessWatchdog
) {
    private val activeSessions = ConcurrentHashMap<String, TerminalSession>()

    companion object {
        private const val TAG = "TerminalManager"

        @Volatile
        private var instance: TerminalManager? = null

        fun getInstance(context: Context, watchdog: ProcessWatchdog): TerminalManager {
            return instance ?: synchronized(this) {
                instance ?: TerminalManager(context.applicationContext, watchdog).also { instance = it }
            }
        }
    }

    /**
     * 创建终端会话
     */
    fun createSession(): String {
        val sessionId = "term_${System.currentTimeMillis()}"
        activeSessions[sessionId] = TerminalSession(
            id = sessionId,
            createdAt = System.currentTimeMillis(),
            isActive = true
        )
        return sessionId
    }

    /**
     * 执行命令
     */
    suspend fun executeCommand(sessionId: String, command: String, timeout: Int = 300): Map<String, Any> = withContext(Dispatchers.IO) {
        val session = activeSessions[sessionId]
            ?: return@withContext mapOf("success" to false, "error" to "Session not found")

        try {
            // 通过容器管理器执行（简化版：直接 Runtime.exec）
            val process = Runtime.getRuntime().exec(arrayOf("/bin/sh", "-c", command))
            val processId = "terminal_${sessionId}_${System.currentTimeMillis()}"
            watchdog.startMonitoring(processId, timeout, "kill")

            val output = process.inputStream.bufferedReader().readText()
            val error = process.errorStream.bufferedReader().readText()
            val exitCode = process.waitFor()

            watchdog.stopMonitoring(processId)

            session.lastActivity = System.currentTimeMillis()
            session.commandCount++

            mapOf(
                "success" to (exitCode == 0),
                "exitCode" to exitCode,
                "output" to output,
                "error" to error,
                "command" to command
            )
        } catch (e: Exception) {
            mapOf(
                "success" to false,
                "error" to (e.message ?: "Execution failed")
            )
        }
    }

    /**
     * 关闭会话
     */
    fun closeSession(sessionId: String) {
        activeSessions.remove(sessionId)
        watchdog.stopMonitoring("terminal_$sessionId")
    }

    /**
     * 获取活跃会话列表
     */
    fun getActiveSessions(): List<Map<String, Any>> {
        return activeSessions.values.map { session ->
            mapOf(
                "id" to session.id,
                "createdAt" to session.createdAt,
                "lastActivity" to session.lastActivity,
                "commandCount" to session.commandCount,
                "isActive" to session.isActive
            )
        }
    }

    /**
     * 获取会话信息
     */
    fun getSession(sessionId: String): TerminalSession? {
        return activeSessions[sessionId]
    }

    fun dispose() {
        activeSessions.forEach { (id, _) ->
            watchdog.stopMonitoring("terminal_$id")
        }
        activeSessions.clear()
    }

    data class TerminalSession(
        val id: String,
        val createdAt: Long,
        var lastActivity: Long = System.currentTimeMillis(),
        var commandCount: Int = 0,
        var isActive: Boolean = true
    )
}
