package com.kelivo.operit.fusion.workspace

import android.content.Context
import android.content.SharedPreferences
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * 全局工作区管理器 - 融合版新增
 *
 * 解决 Operit 原生「每个会话重新绑定工作区」的痛点。
 * 全局绑定同一个容器工作目录，所有对话会话共用一套容器环境。
 *
 * 功能：
 * - 全局唯一工作区路径
 * - 会话-工作区映射
 * - 工作区状态持久化
 * - 跨会话文件共享
 */
class WorkspaceManager private constructor(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("fusion_workspace", Context.MODE_PRIVATE)

    // 全局工作区根目录
    private val workspaceRoot: File by lazy {
        val dir = File(context.filesDir, "workspace")
        if (!dir.exists()) dir.mkdirs()
        dir
    }

    // 当前绑定的会话列表
    private val boundSessions = ConcurrentHashMap<String, WorkspaceSession>()

    // 全局工作区状态
    private val workspaceState = ConcurrentHashMap<String, Any>()

    // 工作区操作计数
    private val operationCount = AtomicLong(0)

    companion object {
        private const val TAG = "WorkspaceManager"

        @Volatile
        private var instance: WorkspaceManager? = null

        fun getInstance(context: Context): WorkspaceManager {
            return instance ?: synchronized(this) {
                instance ?: WorkspaceManager(context.applicationContext).also { instance = it }
            }
        }
    }

    /**
     * 获取工作区根目录
     */
    fun getWorkspaceRoot(): File = workspaceRoot

    /**
     * 获取工作区路径
     */
    fun getWorkspacePath(): String = workspaceRoot.absolutePath

    /**
     * 绑定会话到工作区
     */
    fun bindSession(sessionId: String): WorkspaceSession {
        val session = WorkspaceSession(
            sessionId = sessionId,
            workspacePath = workspaceRoot.absolutePath,
            bindTime = System.currentTimeMillis()
        )
        boundSessions[sessionId] = session
        persistWorkspaceState()
        return session
    }

    /**
     * 解绑会话
     */
    fun unbindSession(sessionId: String) {
        boundSessions.remove(sessionId)
        persistWorkspaceState()
    }

    /**
     * 检查会话是否已绑定
     */
    fun isSessionBound(sessionId: String): Boolean {
        return boundSessions.containsKey(sessionId)
    }

    /**
     * 获取会话信息
     */
    fun getSession(sessionId: String): WorkspaceSession? {
        return boundSessions[sessionId]
    }

    /**
     * 获取所有绑定的会话
     */
    fun getAllBoundSessions(): List<WorkspaceSession> {
        return boundSessions.values.toList()
    }

    /**
     * 获取活跃会话数
     */
    fun getActiveSessionCount(): Int = boundSessions.size

    /**
     * 在工作区内创建文件
     */
    fun createFile(relativePath: String, content: String): File {
        val file = File(workspaceRoot, relativePath)
        file.parentFile?.mkdirs()
        file.writeText(content)
        incrementOperation()
        return file
    }

    /**
     * 读取工作区文件
     */
    fun readFile(relativePath: String): String? {
        val file = File(workspaceRoot, relativePath)
        return if (file.exists()) file.readText() else null
    }

    /**
     * 列出工作区目录
     */
    fun listDirectory(relativePath: String = ""): List<Map<String, Any>> {
        val dir = if (relativePath.isEmpty()) workspaceRoot else File(workspaceRoot, relativePath)
        return dir.listFiles()?.map { file ->
            mapOf(
                "name" to file.name,
                "isDirectory" to file.isDirectory,
                "size" to file.length(),
                "lastModified" to file.lastModified()
            )
        } ?: emptyList()
    }

    /**
     * 删除工作区文件
     */
    fun deleteFile(relativePath: String): Boolean {
        val file = File(workspaceRoot, relativePath)
        val result = file.delete()
        if (result) incrementOperation()
        return result
    }

    /**
     * 检查工作区文件是否存在
     */
    fun fileExists(relativePath: String): Boolean {
        return File(workspaceRoot, relativePath).exists()
    }

    /**
     * 获取工作区状态摘要
     */
    fun getWorkspaceSummary(): Map<String, Any> {
        val totalFiles = workspaceRoot.walkTopDown().count { it.isFile }
        val totalSize = workspaceRoot.walkTopDown().filter { it.isFile }.sumOf { it.length() }

        return mapOf(
            "workspacePath" to workspaceRoot.absolutePath,
            "totalFiles" to totalFiles,
            "totalSize" to totalSize,
            "activeSessions" to boundSessions.size,
            "operationCount" to operationCount.get(),
            "sessions" to boundSessions.values.map { session ->
                mapOf(
                    "sessionId" to session.sessionId,
                    "bindTime" to session.bindTime,
                    "lastAccessTime" to session.lastAccessTime
                )
            }
        )
    }

    /**
     * 获取工作区大小
     */
    fun getWorkspaceSize(): Long {
        return workspaceRoot.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    /**
     * 清空工作区（危险操作）
     */
    fun clearWorkspace(): Boolean {
        return try {
            workspaceRoot.listFiles()?.forEach { it.deleteRecursively() }
            incrementOperation()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 更新会话最后访问时间
     */
    fun touchSession(sessionId: String) {
        boundSessions[sessionId]?.let {
            it.lastAccessTime = System.currentTimeMillis()
        }
    }

    /**
     * 持久化工作区状态
     */
    private fun persistWorkspaceState() {
        prefs.edit()
            .putStringSet("bound_sessions", boundSessions.keys)
            .putLong("last_modified", System.currentTimeMillis())
            .apply()
    }

    /**
     * 恢复工作区状态
     */
    fun restoreWorkspaceState() {
        val savedSessions = prefs.getStringSet("bound_sessions", emptySet()) ?: return
        // 只恢复映射，不恢复进程状态
        savedSessions.forEach { sessionId ->
            if (!boundSessions.containsKey(sessionId)) {
                bindSession(sessionId)
            }
        }
    }

    /**
     * 增加操作计数
     */
    private fun incrementOperation() {
        operationCount.incrementAndGet()
    }

    /**
     * 释放资源
     */
    fun dispose() {
        boundSessions.clear()
        workspaceState.clear()
    }
}

/**
 * 工作区会话信息
 */
data class WorkspaceSession(
    val sessionId: String,
    val workspacePath: String,
    val bindTime: Long,
    var lastAccessTime: Long = System.currentTimeMillis(),
    var isActive: Boolean = true
)
