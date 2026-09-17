package com.kelivo.operit.fusion.bridge

import android.content.Context
import com.kelivo.operit.fusion.config.ContainerConfig
import com.kelivo.operit.fusion.container.ContainerManager
import com.kelivo.operit.fusion.terminal.TerminalManager
import com.kelivo.operit.fusion.adb.AdbManager
import com.kelivo.operit.fusion.mcp.McpManager
import com.kelivo.operit.fusion.watchdog.ProcessWatchdog
import com.kelivo.operit.fusion.workspace.WorkspaceManager
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 融合版主桥接类 - Flutter 与原生能力的唯一入口
 *
 * 架构：
 * Flutter UI → MethodChannel → FusionBridge → 各模块 Manager
 *
 * 6 个通道：
 * - container    容器管理
 * - terminal     终端命令
 * - adb          ADB 自动化
 * - mcp          MCP 工具
 * - watchdog     看门狗保护
 * - workspace    全局工作区
 */
class FusionBridge(
    private val context: Context,
    private val containerConfig: ContainerConfig = ContainerConfig()
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // 各模块管理器
    lateinit var containerManager: ContainerManager
        private set
    lateinit var terminalManager: TerminalManager
        private set
    lateinit var adbManager: AdbManager
        private set
    lateinit var mcpManager: McpManager
        private set
    lateinit var watchdog: ProcessWatchdog
        private set
    lateinit var workspaceManager: WorkspaceManager
        private set

    private val activeChannels = mutableListOf<MethodChannel>()

    fun initialize() {
        // 初始化看门狗
        watchdog = ProcessWatchdog.getInstance(context)

        // 初始化容器管理器（传入配置）
        containerManager = ContainerManager.getInstance(context, watchdog, containerConfig)

        // 初始化终端管理器
        terminalManager = TerminalManager.getInstance(context, watchdog)

        // 初始化 ADB 管理器
        adbManager = AdbManager.getInstance(context)

        // 初始化 MCP 管理器
        mcpManager = McpManager.getInstance(context, terminalManager)

        // 初始化全局工作区管理器
        workspaceManager = WorkspaceManager.getInstance(context)
        workspaceManager.restoreWorkspaceState()
    }

    fun registerWithEngine(flutterEngine: FlutterEngine) {
        val binaryMessenger = flutterEngine.dartExecutor.binaryMessenger

        // 1. 容器管理通道
        MethodChannel(binaryMessenger, "com.kelivo.operit.fusion/container").also { channel ->
            channel.setMethodCallHandler { call, result -> handleContainerCall(call, result) }
            activeChannels.add(channel)
        }

        // 2. 终端通道
        MethodChannel(binaryMessenger, "com.kelivo.operit.fusion/terminal").also { channel ->
            channel.setMethodCallHandler { call, result -> handleTerminalCall(call, result) }
            activeChannels.add(channel)
        }

        // 3. ADB 自动化通道
        MethodChannel(binaryMessenger, "com.kelivo.operit.fusion/adb").also { channel ->
            channel.setMethodCallHandler { call, result -> handleAdbCall(call, result) }
            activeChannels.add(channel)
        }

        // 4. MCP 工具通道
        MethodChannel(binaryMessenger, "com.kelivo.operit.fusion/mcp").also { channel ->
            channel.setMethodCallHandler { call, result -> handleMcpCall(call, result) }
            activeChannels.add(channel)
        }

        // 5. 看门狗通道
        MethodChannel(binaryMessenger, "com.kelivo.operit.fusion/watchdog").also { channel ->
            channel.setMethodCallHandler { call, result -> handleWatchdogCall(call, result) }
            activeChannels.add(channel)
        }

        // 6. 工作区通道
        MethodChannel(binaryMessenger, "com.kelivo.operit.fusion/workspace").also { channel ->
            channel.setMethodCallHandler { call, result -> handleWorkspaceCall(call, result) }
            activeChannels.add(channel)
        }
    }

    // ==================== 容器管理 ====================

    private fun handleContainerCall(call: MethodCall, result: MethodChannel.Result) {
        scope.launch {
            try {
                when (call.method) {
                    "getStatus" -> result.success(containerManager.getStatus())
                    "start" -> {
                        val rootfsUrl = call.argument<String>("rootfsUrl")
                        result.success(containerManager.start(rootfsUrl))
                    }
                    "stop" -> result.success(containerManager.stop())
                    "executeCommand" -> {
                        val cmd = call.argument<String>("command") ?: ""
                        val timeout = call.argument<Int>("timeout") ?: containerConfig.defaultCommandTimeout
                        result.success(containerManager.executeCommand(cmd, timeout))
                    }
                    "downloadRootfs" -> {
                        val url = call.argument<String>("url") ?: ""
                        result.success(containerManager.downloadRootfs(url))
                    }
                    "getRootfsProgress" -> result.success(containerManager.getDownloadProgress())
                    "getConfig" -> result.success(mapOf(
                        "gradleJvmArgs" to containerConfig.toGradleJvmArgs(),
                        "gradleOpts" to containerConfig.toGradleOpts(),
                        "prootBinds" to containerConfig.toProotBinds(),
                        "envVars" to containerConfig.environmentVariables
                    ))
                    "setConfig" -> {
                        // 动态更新配置
                        val heapSize = call.argument<Int>("gradleMaxHeapSize")
                        if (heapSize != null) {
                            containerConfig.copy(gradleMaxHeapSize = heapSize)
                        }
                        result.success(true)
                    }
                    else -> result.notImplemented()
                }
            } catch (e: Exception) {
                result.error("CONTAINER_ERROR", e.message, null)
            }
        }
    }

    // ==================== 终端管理 ====================

    private fun handleTerminalCall(call: MethodCall, result: MethodChannel.Result) {
        scope.launch {
            try {
                when (call.method) {
                    "createSession" -> result.success(terminalManager.createSession())
                    "executeCommand" -> {
                        val sessionId = call.argument<String>("sessionId") ?: ""
                        val cmd = call.argument<String>("command") ?: ""
                        val timeout = call.argument<Int>("timeout") ?: containerConfig.terminalTimeout
                        result.success(terminalManager.executeCommand(sessionId, cmd, timeout))
                    }
                    "closeSession" -> {
                        val sessionId = call.argument<String>("sessionId") ?: ""
                        terminalManager.closeSession(sessionId)
                        result.success(true)
                    }
                    "getActiveSessions" -> result.success(terminalManager.getActiveSessions())
                    "getSession" -> {
                        val sessionId = call.argument<String>("sessionId") ?: ""
                        result.success(terminalManager.getSession(sessionId)?.let { session ->
                            mapOf(
                                "id" to session.id,
                                "createdAt" to session.createdAt,
                                "lastActivity" to session.lastActivity,
                                "commandCount" to session.commandCount,
                                "isActive" to session.isActive
                            )
                        })
                    }
                    else -> result.notImplemented()
                }
            } catch (e: Exception) {
                result.error("TERMINAL_ERROR", e.message, null)
            }
        }
    }

    // ==================== ADB 自动化 ====================

    private fun handleAdbCall(call: MethodCall, result: MethodChannel.Result) {
        scope.launch {
            try {
                when (call.method) {
                    "connect" -> {
                        val host = call.argument<String>("host") ?: "127.0.0.1"
                        val port = call.argument<Int>("port") ?: 5555
                        result.success(adbManager.connect(host, port))
                    }
                    "disconnect" -> result.success(adbManager.disconnect())
                    "isConnected" -> result.success(adbManager.isConnected())
                    "executeCommand" -> {
                        val cmd = call.argument<String>("command") ?: ""
                        result.success(adbManager.executeCommand(cmd))
                    }
                    "tap" -> {
                        val x = call.argument<Int>("x") ?: 0
                        val y = call.argument<Int>("y") ?: 0
                        result.success(adbManager.tap(x, y))
                    }
                    "swipe" -> {
                        val x1 = call.argument<Int>("x1") ?: 0
                        val y1 = call.argument<Int>("y1") ?: 0
                        val x2 = call.argument<Int>("x2") ?: 0
                        val y2 = call.argument<Int>("y2") ?: 0
                        val duration = call.argument<Int>("duration") ?: 300
                        result.success(adbManager.swipe(x1, y1, x2, y2, duration))
                    }
                    "screenshot" -> result.success(adbManager.screenshot())
                    "getScreenSize" -> result.success(adbManager.getScreenSize())
                    "startApp" -> {
                        val packageName = call.argument<String>("packageName") ?: ""
                        val activity = call.argument<String>("activity")
                        result.success(adbManager.startApp(packageName, activity))
                    }
                    "stopApp" -> {
                        val packageName = call.argument<String>("packageName") ?: ""
                        result.success(adbManager.stopApp(packageName))
                    }
                    "inputText" -> {
                        val text = call.argument<String>("text") ?: ""
                        result.success(adbManager.inputText(text))
                    }
                    "pressKey" -> {
                        val keyCode = call.argument<Int>("keyCode") ?: 0
                        result.success(adbManager.pressKey(keyCode))
                    }
                    "getInstalledApps" -> result.success(adbManager.getInstalledApps())
                    else -> result.notImplemented()
                }
            } catch (e: Exception) {
                result.error("ADB_ERROR", e.message, null)
            }
        }
    }

    // ==================== MCP 工具 ====================

    private fun handleMcpCall(call: MethodCall, result: MethodChannel.Result) {
        scope.launch {
            try {
                when (call.method) {
                    "listServers" -> result.success(mcpManager.listServers())
                    "addServer" -> {
                        val name = call.argument<String>("name") ?: ""
                        val config = call.argument<Map<String, Any>>("config") ?: emptyMap()
                        result.success(mcpManager.addServer(name, config))
                    }
                    "removeServer" -> {
                        val name = call.argument<String>("name") ?: ""
                        result.success(mcpManager.removeServer(name))
                    }
                    "startServer" -> {
                        val name = call.argument<String>("name") ?: ""
                        result.success(mcpManager.startServer(name))
                    }
                    "stopServer" -> {
                        val name = call.argument<String>("name") ?: ""
                        result.success(mcpManager.stopServer(name))
                    }
                    "callTool" -> {
                        val serverName = call.argument<String>("serverName") ?: ""
                        val toolName = call.argument<String>("toolName") ?: ""
                        val args = call.argument<Map<String, Any>>("arguments") ?: emptyMap()
                        val timeout = call.argument<Int>("timeout") ?: containerConfig.mcpTimeout
                        result.success(mcpManager.callTool(serverName, toolName, args, timeout))
                    }
                    "listTools" -> {
                        val serverName = call.argument<String>("serverName") ?: ""
                        result.success(mcpManager.listTools(serverName))
                    }
                    else -> result.notImplemented()
                }
            } catch (e: Exception) {
                result.error("MCP_ERROR", e.message, null)
            }
        }
    }

    // ==================== 看门狗 ====================

    private fun handleWatchdogCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "startMonitoring" -> {
                val processId = call.argument<String>("processId") ?: ""
                val timeout = call.argument<Int>("timeout") ?: 600
                val onTimeout = call.argument<String>("onTimeout") ?: "kill"
                watchdog.startMonitoring(processId, timeout, onTimeout)
                result.success(true)
            }
            "stopMonitoring" -> {
                val processId = call.argument<String>("processId") ?: ""
                watchdog.stopMonitoring(processId)
                result.success(true)
            }
            "heartbeat" -> {
                val processId = call.argument<String>("processId") ?: ""
                watchdog.heartbeat(processId)
                result.success(true)
            }
            "getActiveProcesses" -> result.success(watchdog.getActiveProcesses())
            "getProcessUptime" -> {
                val processId = call.argument<String>("processId") ?: ""
                result.success(watchdog.getProcessUptime(processId))
            }
            "isProcessAlive" -> {
                val processId = call.argument<String>("processId") ?: ""
                result.success(watchdog.isProcessAlive(processId))
            }
            else -> result.notImplemented()
        }
    }

    // ==================== 工作区 ====================

    private fun handleWorkspaceCall(call: MethodCall, result: MethodChannel.Result) {
        scope.launch {
            try {
                when (call.method) {
                    "bindSession" -> {
                        val sessionId = call.argument<String>("sessionId") ?: ""
                        val session = workspaceManager.bindSession(sessionId)
                        result.success(mapOf(
                            "sessionId" to session.sessionId,
                            "workspacePath" to session.workspacePath,
                            "bindTime" to session.bindTime
                        ))
                    }
                    "unbindSession" -> {
                        val sessionId = call.argument<String>("sessionId") ?: ""
                        workspaceManager.unbindSession(sessionId)
                        result.success(true)
                    }
                    "isSessionBound" -> {
                        val sessionId = call.argument<String>("sessionId") ?: ""
                        result.success(workspaceManager.isSessionBound(sessionId))
                    }
                    "getWorkspacePath" -> result.success(workspaceManager.getWorkspacePath())
                    "getWorkspaceSummary" -> result.success(workspaceManager.getWorkspaceSummary())
                    "createFile" -> {
                        val relativePath = call.argument<String>("relativePath") ?: ""
                        val content = call.argument<String>("content") ?: ""
                        val file = workspaceManager.createFile(relativePath, content)
                        result.success(file.absolutePath)
                    }
                    "readFile" -> {
                        val relativePath = call.argument<String>("relativePath") ?: ""
                        result.success(workspaceManager.readFile(relativePath))
                    }
                    "listDirectory" -> {
                        val relativePath = call.argument<String>("relativePath") ?: ""
                        result.success(workspaceManager.listDirectory(relativePath))
                    }
                    "deleteFile" -> {
                        val relativePath = call.argument<String>("relativePath") ?: ""
                        result.success(workspaceManager.deleteFile(relativePath))
                    }
                    "fileExists" -> {
                        val relativePath = call.argument<String>("relativePath") ?: ""
                        result.success(workspaceManager.fileExists(relativePath))
                    }
                    "getActiveSessionCount" -> result.success(workspaceManager.getActiveSessionCount())
                    "clearWorkspace" -> result.success(workspaceManager.clearWorkspace())
                    else -> result.notImplemented()
                }
            } catch (e: Exception) {
                result.error("WORKSPACE_ERROR", e.message, null)
            }
        }
    }

    fun dispose() {
        activeChannels.clear()
        if (::containerManager.isInitialized) containerManager.dispose()
        if (::terminalManager.isInitialized) terminalManager.dispose()
        if (::adbManager.isInitialized) adbManager.dispose()
        if (::mcpManager.isInitialized) mcpManager.dispose()
        if (::watchdog.isInitialized) watchdog.dispose()
        if (::workspaceManager.isInitialized) workspaceManager.dispose()
    }
}
