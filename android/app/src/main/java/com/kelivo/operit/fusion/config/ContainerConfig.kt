package com.kelivo.operit.fusion.config

/**
 * 容器环境配置 - 融合版新增
 *
 * 统一管理容器内的 Gradle 配置、内存限制、vfs 开关等工程优化。
 * 解决 Operit 原生缺少内存保护、进程卡死恢复的问题。
 *
 * 配置项：
 * - Gradle JVM 内存限制（-Xmx4g 避免 OOM）
 * - vfs 开关（关闭不必要的虚拟文件系统）
 * - 心跳超时时间（5/10 分钟自动捕获日志）
 * - 编译并行度限制
 * - 环境变量预设
 */
data class ContainerConfig(
    // ==================== Gradle 内存配置 ====================

    /** Gradle JVM 最大堆内存（MB） */
    val gradleMaxHeapSize: Int = 4096,

    /** Gradle JVM 最大元空间大小（MB） */
    val gradleMaxMetaspaceSize: Int = 1024,

    /** Gradle JVM 预留代码缓存（MB） */
    val gradleReservedCodeCacheSize: Int = 512,

    /** 是否启用 HeapDumpOnOutOfMemoryError */
    val gradleHeapDumpOnOOM: Boolean = true,

    // ==================== 编译并行度 ====================

    /** Cargo 编译并行任务数 */
    val cargoBuildJobs: Int = 2,

    /** CMake 编译并行度 */
    val cmakeBuildParallelLevel: Int = 2,

    /** Kotlin Daemon 并行编译 */
    val kotlinDaemonParallel: Boolean = true,

    // ==================== VFS 开关 ====================

    /** 是否绑定 /proc */
    val bindProc: Boolean = true,

    /** 是否绑定 /sys */
    val bindSys: Boolean = true,

    /** 是否绑定 /dev */
    val bindDev: Boolean = true,

    /** 是否绑定 /data */
    val bindData: Boolean = true,

    /** 是否绑定 /sdcard */
    val bindSdcard: Boolean = true,

    /** 额外绑定的自定义路径 */
    val extraBinds: List<String> = emptyList(),

    // ==================== 心跳超时保护 ====================

    /** 容器命令默认超时（秒） */
    val defaultCommandTimeout: Int = 600,

    /** 终端会话超时（秒） */
    val terminalTimeout: Int = 300,

    /** ADB 命令超时（秒） */
    val adbTimeout: Int = 60,

    /** MCP 工具调用超时（秒） */
    val mcpTimeout: Int = 300,

    /** 是否启用看门狗 */
    val watchdogEnabled: Boolean = true,

    /** 看门狗心跳间隔（秒） */
    val watchdogHeartbeatInterval: Int = 30,

    // ==================== 环境变量 ====================

    /** 预设环境变量 */
    val environmentVariables: Map<String, String> = defaultEnvironmentVariables(),

    // ==================== PRoot 配置 ====================

    /** PRoot 工作目录 */
    val prootWorkDir: String = "/workspace",

    /** 是否启用 --kill-on-exit */
    val prootKillOnExit: Boolean = true,

    /** 链接器库路径 */
    val linkerLibraryPath: String = "/system/lib64"
) {
    companion object {
        /**
         * 默认环境变量
         */
        fun defaultEnvironmentVariables(): Map<String, String> {
            return mapOf(
                // Java/Gradle
                "JAVA_HOME" to "/usr/lib/jvm/java-17-openjdk-arm64",
                "GRADLE_OPTS" to "-Dorg.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g -XX:+HeapDumpOnOutOfMemoryError",

                // Android SDK
                "ANDROID_HOME" to "/opt/android-sdk",
                "ANDROID_SDK_ROOT" to "/opt/android-sdk",

                // Cargo/Rust
                "CARGO_HOME" to "/root/.cargo",
                "RUSTUP_HOME" to "/root/.rustup",
                "CARGO_BUILD_JOBS" to "2",

                // CMake
                "CMAKE_BUILD_PARALLEL_LEVEL" to "2",

                // 通用
                "PATH" to "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/opt/android-sdk/cmdline-tools/latest/bin",
                "LANG" to "en_US.UTF-8",
                "LC_ALL" to "en_US.UTF-8",
                "TERM" to "xterm-256color"
            )
        }

        /**
         * 低内存模式配置（适用于 4GB 以下设备）
         */
        fun lowMemoryMode(): ContainerConfig {
            return ContainerConfig(
                gradleMaxHeapSize = 2048,
                gradleMaxMetaspaceSize = 512,
                cargoBuildJobs = 1,
                cmakeBuildParallelLevel = 1,
                terminalTimeout = 180,
                mcpTimeout = 180
            )
        }

        /**
         * 高性能模式配置（适用于 8GB+ 设备）
         */
        fun highPerformanceMode(): ContainerConfig {
            return ContainerConfig(
                gradleMaxHeapSize = 8192,
                gradleMaxMetaspaceSize = 2048,
                cargoBuildJobs = 4,
                cmakeBuildParallelLevel = 4,
                terminalTimeout = 600,
                mcpTimeout = 600
            )
        }
    }

    /**
     * 生成 Gradle JVM 参数字符串
     */
    fun toGradleJvmArgs(): String {
        return "-Xmx${gradleMaxHeapSize}m -XX:MaxMetaspaceSize=${gradleMaxMetaspaceSize}m " +
            "-XX:ReservedCodeCacheSize=${gradleReservedCodeCacheSize}m" +
            if (gradleHeapDumpOnOOM) " -XX:+HeapDumpOnOutOfMemoryError" else ""
    }

    /**
     * 生成 GRADLE_OPTS 环境变量值
     */
    fun toGradleOpts(): String {
        return "-Dorg.gradle.jvmargs=$toGradleJvmArgs()"
    }

    /**
     * 生成 PRoot 绑定参数列表
     */
    fun toProotBinds(): List<String> {
        val binds = mutableListOf<String>()
        if (bindProc) binds.add("--bind=/proc")
        if (bindSys) binds.add("--bind=/sys")
        if (bindDev) binds.add("--bind=/dev")
        if (bindData) binds.add("--bind=/data")
        if (bindSdcard) binds.add("--bind=/sdcard")
        binds.addAll(extraBinds.map { "--bind=$it}" })
        return binds
    }

    /**
     * 生成环境变量字符串（用于 bash -c 导出）
     */
    fun toEnvExportScript(): String {
        return environmentVariables.entries.joinToString("\n") { (key, value) ->
            "export $key=\"$value\""
        }
    }

    /**
     * 生成 PRoot 完整命令行前缀
     */
    fun toProotCommandPrefix(): List<String> {
        return buildList {
            add("--rootfs=\${ROOTFS_PATH}")
            addAll(toProotBinds())
            add("--cwd=$prootWorkDir")
            if (prootKillOnExit) add("--kill-on-exit")
        }
    }

    /**
     * 验证配置是否有效
     */
    fun validate(): Boolean {
        return gradleMaxHeapSize >= 512 &&
            gradleMaxMetaspaceSize >= 256 &&
            cargoBuildJobs >= 1 &&
            cmakeBuildParallelLevel >= 1 &&
            defaultCommandTimeout >= 30
    }
}
