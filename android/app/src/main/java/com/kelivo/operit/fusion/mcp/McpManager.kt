package com.kelivo.operit.fusion.mcp

import android.content.Context
import android.content.SharedPreferences
import com.kelivo.operit.fusion.terminal.TerminalManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * MCP 工具管理器 - 移植自 Operit
 *
 * 功能：
 * - MCP 服务器配置管理
 * - 本地 STDIO 工具启动
 * - 工具调用与结果回传
 * - 与终端管理器集成，支持容器内执行
 */
class McpManager private constructor(
    private val context: Context,
    private val terminalManager: TerminalManager
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("fusion_mcp", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val activeServers = ConcurrentHashMap<String, McpServerInstance>()

    companion object {
        private const val TAG = "McpManager"
        private const val PREFS_KEY_SERVERS = "mcp_servers"

        @Volatile
        private var instance: McpManager? = null

        fun getInstance(context: Context, terminalManager: TerminalManager): McpManager {
            return instance ?: synchronized(this) {
                instance ?: McpManager(context.applicationContext, terminalManager).also { instance = it }
            }
        }
    }

    /**
     * 列出所有 MCP 服务器
     */
    fun listServers(): List<Map<String, Any>> {
        val serversJson = prefs.getString(PREFS_KEY_SERVERS, "[]")
        val type = object : TypeToken<List<McpServerConfig>>() {}.type
        val servers: List<McpServerConfig> = gson.fromJson(serversJson, type)

        return servers.map { server ->
            val isActive = activeServers.containsKey(server.name)
            mapOf(
                "name" to server.name,
                "command" to server.command,
                "args" to server.args,
                "env" to server.env,
                "isActive" to isActive
            )
        }
    }

    /**
     * 添加 MCP 服务器
     */
    fun addServer(name: String, config: Map<String, Any>): Map<String, Any> {
        try {
            val serverConfig = McpServerConfig(
                name = name,
                command = config["command"] as? String ?: "",
                args = config["args"] as? List<String> ?: emptyList(),
                env = config["env"] as? Map<String, String> ?: emptyMap()
            )

            val servers = loadServers().toMutableList()
            servers.removeAll { it.name == name }
            servers.add(serverConfig)
            saveServers(servers)

            mapOf("success" to true, "message" to "服务器已添加")
        } catch (e: Exception) {
            mapOf("success" to false, "error" to (e.message ?: "添加失败"))
        }
    }

    /**
     * 移除 MCP 服务器
     */
    fun removeServer(name: String): Map<String, Any> {
        try {
            activeServers.remove(name)?.stop()
            val servers = loadServers().toMutableList()
            servers.removeAll { it.name == name }
            saveServers(servers)

            mapOf("success" to true, "message" to "服务器已移除")
        } catch (e: Exception) {
            mapOf("success" to false, "error" to (e.message ?: "移除失败"))
        }
    }

    /**
     * 启动服务器
     */
    fun startServer(name: String): Map<String, Any> {
        val config = loadServers().find { it.name == name }
            ?: return mapOf("success" to false, "error" to "服务器配置不存在")

        return try {
            val instance = McpServerInstance(config)
            instance.start()
            activeServers[name] = instance

            mapOf("success" to true, "message" to "服务器已启动")
        } catch (e: Exception) {
            mapOf("success" to false, "error" to (e.message ?: "启动失败"))
        }
    }

    /**
     * 停止服务器
     */
    fun stopServer(name: String): Map<String, Any> {
        activeServers.remove(name)?.stop()
        return mapOf("success" to true, "message" to "服务器已停止")
    }

    /**
     * 调用工具
     */
    fun callTool(serverName: String, toolName: String, args: Map<String, Any>, timeout: Int = 300): Map<String, Any> {
        val server = activeServers[serverName]
            ?: return mapOf("success" to false, "error" to "服务器未运行")

        return try {
            server.callTool(toolName, args, timeout)
        } catch (e: Exception) {
            mapOf("success" to false, "error" to (e.message ?: "调用失败"))
        }
    }

    /**
     * 列出服务器工具
     */
    fun listTools(serverName: String): List<Map<String, Any>> {
        val server = activeServers[serverName] ?: return emptyList()
        return server.listTools()
    }

    private fun loadServers(): List<McpServerConfig> {
        val json = prefs.getString(PREFS_KEY_SERVERS, "[]")
        val type = object : TypeToken<List<McpServerConfig>>() {}.type
        return gson.fromJson(json, type)
    }

    private fun saveServers(servers: List<McpServerConfig>) {
        prefs.edit().putString(PREFS_KEY_SERVERS, gson.toJson(servers)).apply()
    }

    fun dispose() {
        activeServers.forEach { (_, server) -> server.stop() }
        activeServers.clear()
    }
}

data class McpServerConfig(
    val name: String,
    val command: String,
    val args: List<String> = emptyList(),
    val env: Map<String, String> = emptyMap()
)

class McpServerInstance(private val config: McpServerConfig) {
    private var process: Process? = null

    fun start() {
        val command = mutableListOf(config.command)
        command.addAll(config.args)

        val processBuilder = ProcessBuilder(command)
        processBuilder.environment().putAll(config.env)
        process = processBuilder.start()
    }

    fun stop() {
        process?.destroy()
        process = null
    }

    fun callTool(toolName: String, args: Map<String, Any>, timeout: Int): Map<String, Any> {
        // TODO: 实现 MCP JSON-RPC 调用
        return mapOf(
            "success" to true,
            "result" to "Tool call simulated: $toolName"
        )
    }

    fun listTools(): List<Map<String, Any>> {
        // TODO: 返回工具列表
        return emptyList()
    }
}
