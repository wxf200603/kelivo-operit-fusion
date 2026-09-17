# Kelivo × Operit 融合版

## 项目简介

**Kelivo × Operit 融合版** 是一个将两个优秀开源项目合二为一的 Android AI Agent 平台：

- **Kelivo**（Flutter/Dart）：提供现代化的跨平台 UI、对话会话管理、模型选择面板、MCP 服务配置
- **Operit**（Kotlin/Compose）：提供 Android 原生能力，包括 Ubuntu 容器管理、ADB 手机自动化、终端命令执行

融合版以 Kelivo 的 Flutter 前端 UI 为主体，移植 Operit 的核心原生能力，实现「手机装完即跑、动手干活」的完整 AI Agent 体验。

## 架构设计

```
┌─────────────────────────────────────────────────────────┐
│  上层 (Dart/Flutter) - Kelivo UI主体                    │
│  - 对话页、MCP服务管理页、设置页                          │
│  - 新增：容器管理页、设备自动化页、终端页、工作区页        │
│  - MethodChannel 调用原生能力                            │
├─────────────────────────────────────────────────────────┤
│  桥接层 (Kotlin) - FusionBridge                         │
│  - MethodChannelHandler (6个通道)                       │
│  - ContainerBridge / TerminalBridge / AdbBridge         │
│  - McpBridge / WatchdogBridge / WorkspaceBridge         │
├─────────────────────────────────────────────────────────┤
│  底层能力层 - Operit核心模块                              │
│  - container: Ubuntu 24.04 ARM64 用户空间 (PRoot)        │
│  - terminal: 多会话终端管理                               │
│  - adb: 无线ADB手机自动化                                │
│  - mcp: MCP 工具执行引擎                                 │
│  - watchdog: 心跳超时保护                                │
│  - workspace: 全局工作区管理                             │
│  - config: 容器Gradle/内存配置                           │
└─────────────────────────────────────────────────────────┘
```

## 6 个 MethodChannel

| 通道 | 功能 | 说明 |
|------|------|------|
| `container` | 容器管理 | rootfs下载、启动/停止、命令执行 |
| `terminal` | 终端命令 | 多会话管理、命令执行 |
| `adb` | ADB自动化 | 无线连接、点击滑动、截图、APP启停 |
| `mcp` | MCP工具 | 服务器管理、工具调用 |
| `watchdog` | 看门狗 | 心跳超时保护、进程监控 |
| `workspace` | 工作区 | 全局工作区绑定、文件管理 |

## 功能清单

### 来自 Kelivo（UI/会话层）
- ✅ 现代化 Material You 设计语言
- ✅ 多模型支持（OpenAI/Anthropic/Gemini/自定义）
- ✅ 对话会话管理（分支、迁移、自动总结）
- ✅ MCP 服务配置 UI
- ✅ 多模态输入（图片、PDF、Word）
- ✅ Web 搜索（12+ 搜索引擎）
- ✅ TTS/STT 语音对话
- ✅ 数据备份与恢复

### 来自 Operit（原生能力层）
- 🔄 Ubuntu 24.04 容器管理（PRoot 用户空间）
- 🔄 ADB 无线手机自动化（点击、滑动、截图）
- 🔄 终端命令执行（多会话）
- 🔄 MCP 工具执行引擎
- 🔄 进程心跳超时保护

### 融合增强（独有功能）
- 🆕 全局工作区绑定（新建会话不用重新绑定）
- 🆕 内存保护（Gradle 内存限制、OOM 防护）
- 🆕 心跳超时保护（5/10 分钟自动捕获日志）
- 🆕 工程优化（vfs 关闭、编译优化）

## 构建指南

### 环境要求

- Flutter 3.47.4+ (Dart 3.13+)
- Android SDK 36 (compileSdk/targetSdk)
- NDK 28.2.13676358
- CMake 3.22.1+
- JDK 11+

### 构建步骤

```bash
# 1. 克隆仓库
git clone <repository-url>
cd kelivo-operit-fusion

# 2. 获取依赖
flutter pub get

# 3. 构建 APK
./fusion_build.sh release

# 4. 安装到设备
adb install -r build/app/outputs/apk/release/app-release.apk
```

### 签名配置

创建 `android/app/key.properties`:

```properties
storeFile=ks.jks
storePassword=your_password
keyAlias=your_alias
keyPassword=your_password
```

## 开源协议

本项目融合自两个开源项目，遵守各自许可证：

| 项目 | 许可证 | 版权声明 |
|------|--------|---------|
| Kelivo | AGPL-3.0 | Copyright (c) Chevey339 |
| Operit | LGPL-3.0 | Copyright (c) AAswordman |
| 融合版 | AGPL-3.0（从严） | 保留双方原作者版权声明 |

## 开发进度

- [x] 阶段1：源码梳理与优缺点盘点
- [x] 阶段2：项目架构重构（三层解耦）
- [ ] 阶段3：模块移植与功能合并
  - [x] 容器管理模块（基础框架）
  - [x] 终端管理器（基础框架）
  - [x] ADB 自动化（基础框架）
  - [x] MCP 工具执行引擎
  - [x] 全局工作区管理
  - [x] 容器配置管理
  - [ ] 统一会话&工作区逻辑
- [ ] 阶段4：冲突修复、功能联调测试
- [ ] 阶段5：打包、签名、优化
- [ ] 阶段6：迭代完善

## 项目结构

```
kelivo-operit-fusion/
├── lib/                          # Flutter UI层 (Dart)
│   ├── bridge/fusion_bridge.dart              ← Dart↔原生 通信桥
│   ├── core/{app.dart, providers/, utils/}    ← 核心层
│   ├── features/
│   │   ├── container/container_page.dart     ← 容器管理页
│   │   ├── automation/automation_page.dart   ← 设备自动化页（含终端）
│   │   ├── workspace/workspace_page.dart     ← 工作区管理页
│   │   ├── mcp/mcp_page.dart                 ← MCP服务页
│   │   ├── watchdog/watchdog_page.dart       ← 看门狗监控页
│   │   └── settings/settings_page.dart       ← 设置页
│   └── main.dart                              ← 入口文件
├── android/                      # Android原生层 (Kotlin)
│   └── app/src/main/
│       ├── java/com/kelivo/operit/fusion/
│       │   ├── FusionActivity.kt              ← 主Activity
│       │   ├── bridge/FusionBridge.kt         ← 原生桥接（6通道）
│       │   ├── container/ContainerManager.kt  ← 容器管理器
│       │   ├── terminal/TerminalManager.kt    ← 终端管理器
│       │   ├── adb/AdbManager.kt              ← ADB自动化
│       │   ├── mcp/McpManager.kt              ← MCP工具适配器
│       │   ├── watchdog/ProcessWatchdog.kt    ← 看门狗（新增）
│       │   ├── workspace/WorkspaceManager.kt  ← 全局工作区（新增）
│       │   └── config/ContainerConfig.kt      ← 容器配置（新增）
│       ├── cpp/CMakeLists.txt                 ← Native构建
│       └── AndroidManifest.xml
├── fusion_build.sh               ← 构建脚本
├── pubspec.yaml
└── README.md
```

## 贡献指南

欢迎提交 Pull Request 和 Issue。在提交前请确保：

1. 代码通过 `flutter analyze`
2. 遵循现有代码风格
3. 更新相关文档
4. 保留所有版权声明
