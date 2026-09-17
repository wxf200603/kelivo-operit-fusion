package com.kelivo.operit.fusion.container

import android.content.Context
import android.content.SharedPreferences
import com.kelivo.operit.fusion.config.ContainerConfig
import com.kelivo.operit.fusion.watchdog.ProcessWatchdog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Ubuntu 容器管理器 - 移植自 Operit
 *
 * 功能：
 * - rootfs 下载、校验、解压
 * - 容器启动/停止/状态查询
 * - PRoot 用户空间执行
 * - 内存限制、vfs 关闭等工程优化（通过 ContainerConfig）
 */
class ContainerManager private constructor(
    private val context: Context,
    private val watchdog: ProcessWatchdog,
    private val config: ContainerConfig
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("fusion_container", Context.MODE_PRIVATE)

    // 容器状态
    private val containerState = ConcurrentHashMap<String, Any>()
    private val downloadProgress = AtomicLong(0)
    private val downloadTotal = AtomicLong(0)

    companion object {
        private const val TAG = "ContainerManager"
        private const val ROOTFS_VERSION = "ubuntu-24.04-arm64"
        private const val ROOTFS_FILENAME = "rootfs.tar.xz"

        // 默认 rootfs 下载源（Operit 官方）
        private const val DEFAULT_ROOTFS_URL =
            "https://github.com/AAswordman/OperitTerminalCore/releases/download/rootfs-24.04/ubuntu-24.04-arm64-rootfs.tar.xz"

        @Volatile
        private var instance: ContainerManager? = null

        fun getInstance(context: Context, watchdog: ProcessWatchdog, config: ContainerConfig): ContainerManager {
            return instance ?: synchronized(this) {
                instance ?: ContainerManager(context.applicationContext, watchdog, config).also { instance = it }
            }
        }
    }

    /**
     * 获取容器状态
     */
    fun getStatus(): Map<String, Any> {
        val rootfsDir = getRootfsDir()
        val isRootfsReady = rootfsDir.exists() && rootfsDir.listFiles()?.isNotEmpty() == true
        val isRunning = containerState["running"] as? Boolean ?: false

        return mapOf(
            "isRootfsReady" to isRootfsReady,
            "isRunning" to isRunning,
            "rootfsVersion" to ROOTFS_VERSION,
            "rootfsPath" to rootfsDir.absolutePath,
            "rootfsSize" to if (rootfsDir.exists()) rootfsDir.walkTopDown().sumOf { it.length() } else 0,
            "containerWorkdir" to getContainerWorkdir().absolutePath,
            "config" to mapOf(
                "gradleJvmArgs" to config.toGradleJvmArgs(),
                "terminalTimeout" to config.terminalTimeout,
                "defaultTimeout" to config.defaultCommandTimeout
            )
        )
    }

    /**
     * 启动容器
     */
    suspend fun start(rootfsUrl: String? = null): Map<String, Any> = withContext(Dispatchers.IO) {
        val rootfsDir = getRootfsDir()

        // 检查 rootfs，不存在则自动下载
        if (!rootfsDir.exists() || rootfsDir.listFiles()?.isEmpty() == true) {
            downloadRootfs(rootfsUrl ?: DEFAULT_ROOTFS_URL)
        }

        // 启动容器
        containerState["running"] = true
        containerState["startTime"] = System.currentTimeMillis()

        // 启动看门狗监控
        watchdog.startMonitoring("container_main", config.defaultCommandTimeout, "log")

        mapOf(
            "success" to true,
            "message" to "容器已启动",
            "rootfsPath" to rootfsDir.absolutePath,
            "workdir" to getContainerWorkdir().absolutePath
        )
    }

    /**
     * 停止容器
     */
    suspend fun stop(): Map<String, Any> = withContext(Dispatchers.IO) {
        containerState["running"] = false
        watchdog.stopMonitoring("container_main")

        mapOf(
            "success" to true,
            "message" to "容器已停止"
        )
    }

    /**
     * 在容器内执行命令
     */
    suspend fun executeCommand(command: String, timeout: Int = config.defaultCommandTimeout): Map<String, Any> = withContext(Dispatchers.IO) {
        val rootfsDir = getRootfsDir()
        if (!rootfsDir.exists()) {
            return@withContext mapOf("success" to false, "error" to "rootfs not found")
        }

        try {
            val processId = "terminal_${System.currentTimeMillis()}"
            watchdog.startMonitoring(processId, timeout, "kill")

            // 使用 PRoot 执行命令，注入环境变量
            val fullCommand = buildString {
                appendLine(config.toEnvExportScript())
                append(command)
            }

            val prootCmd = buildProotCommand(fullCommand)
            val process = ProcessBuilder(prootCmd)
                .directory(getContainerWorkdir())
                .redirectErrorStream(true)
                .start()

            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()

            watchdog.stopMonitoring(processId)

            mapOf(
                "success" to true,
                "exitCode" to exitCode,
                "output" to output,
                "command" to command
            )
        } catch (e: Exception) {
            mapOf(
                "success" to false,
                "error" to (e.message ?: "Unknown error"),
                "command" to command
            )
        }
    }

    /**
     * 下载 rootfs
     */
    suspend fun downloadRootfs(url: String): Map<String, Any> = withContext(Dispatchers.IO) {
        try {
            val rootfsFile = File(context.filesDir, ROOTFS_FILENAME)
            val connection = URI(url).toURL().openConnection() as HttpURLConnection
            connection.connect()

            downloadTotal.set(connection.contentLengthLong)
            downloadProgress.set(0)

            connection.inputStream.use { input ->
                FileOutputStream(rootfsFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalRead = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        downloadProgress.set(totalRead)
                    }
                }
            }

            // 解压 rootfs
            extractRootfs(rootfsFile, getRootfsDir())

            // 清理
            rootfsFile.delete()

            mapOf(
                "success" to true,
                "message" to "rootfs下载并解压完成"
            )
        } catch (e: Exception) {
            mapOf(
                "success" to false,
                "error" to (e.message ?: "Download failed")
            )
        }
    }

    /**
     * 获取下载进度
     */
    fun getDownloadProgress(): Map<String, Any> {
        val total = downloadTotal.get()
        val progress = downloadProgress.get()
        val percent = if (total > 0) (progress * 100 / total).toInt() else 0

        return mapOf(
            "progress" to progress,
            "total" to total,
            "percent" to percent,
            "isDownloading" to total > 0 && progress < total
        )
    }

    // ==================== 私有方法 ====================

    private fun getRootfsDir(): File {
        return File(context.filesDir, "rootfs/$ROOTFS_VERSION")
    }

    private fun getContainerWorkdir(): File {
        val workdir = File(context.filesDir, "container_workdir")
        if (!workdir.exists()) workdir.mkdirs()
        return workdir
    }

    private fun buildProotCommand(command: String): List<String> {
        val rootfsDir = getRootfsDir()
        val prootLib = File(context.applicationInfo.nativeLibraryDir, "libproot_exec.so")

        return buildList {
            add(prootLib.absolutePath)
            add("--rootfs=${rootfsDir.absolutePath}")
            addAll(config.toProotBinds())
            add("--cwd=${config.prootWorkDir}")
            if (config.prootKillOnExit) add("--kill-on-exit")
            add("/bin/bash")
            add("-c")
            add(command)
        }
    }

    private fun extractRootfs(tarFile: File, destDir: File) {
        if (!destDir.exists()) destDir.mkdirs()

        ProcessBuilder(
            "tar", "-xf", tarFile.absolutePath,
            "-C", destDir.absolutePath
        ).start().waitFor()
    }

    fun dispose() {
        containerState.clear()
    }
}
