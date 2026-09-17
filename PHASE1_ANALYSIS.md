# Kelivo × Operit 融合项目 - 阶段1：源码梳理与优缺点盘点

## 模块来源对照表

### Dart Flutter 层 (`lib/`)

| 模块文件 | 来源 | 说明 |
|---------|------|------|
| `core/` | Kelivo 源码移植 | 少量修改适配融合项目 |
| `main.dart` | Kelivo 源码移植 | 入口文件，修改路由指向融合页面 |
| `bridge/fusion_bridge.dart` | **全新编写** | 6 通道 Dart‑Native 通信桥 |
| `features/mcp_page.dart` | Kelivo UI 复用 | 复用 Kelivo MCP 页面 UI，底层调用 McpManager 适配器 |
| `features/container_page.dart` | 新增（参考 Kelivo 组件） | 容器管理页，部分复用 Kelivo 组件 |
| `features/automation_page.dart` | 新增（参考 Kelivo 组件） | 设备自动化页（含终端标签），部分复用 Kelivo 组件 |
| `features/workspace_page.dart` | **全新编写** | 融合版专属工作区管理页 |
| `features/watchdog_page.dart` | **全新编写** | 融合版专属看门狗监控页 |
| `features/settings_page.dart` | 新增（参考 Kelivo 组件） | 设置页，部分复用 Kelivo 组件 |

### Android 原生 Kotlin 层

#### 移植改造模块（来自 Operit，修改适配本项目）

| 模块文件 | 来源 | 说明 |
|---------|------|------|
| `container/ContainerManager.kt` | Operit 移植改造 | Ubuntu 容器管理（rootfs 下载、启动/停止、命令执行） |
| `terminal/TerminalManager.kt` | Operit 移植改造 | 终端命令执行（多会话管理、超时保护） |
| `adb/AdbManager.kt` | Operit 移植改造 | ADB 手机自动化（无线连接、点击滑动、截图、APP 启停） |

#### 全新编写模块（Operit、Kelivo 原版均无）

| 模块文件 | 说明 |
|---------|------|
| `watchdog/ProcessWatchdog.kt` | 看门狗心跳超时保护，卡死自动杀进程、抓取日志 |
| `workspace/WorkspaceManager.kt` | 全局统一工作区管理，改写 Operit 会话隔离逻辑 |
| `config/ContainerConfig.kt` | 容器 Gradle 内存、vfs、运行模式配置 |
| `core/FusionConfig.kt` | App 全局配置（首次启动、容器路径记忆等） |
| `utils/FusionLog.kt` | 统一日志工具 |

#### 适配器模块

| 模块文件 | 来源 | 说明 |
|---------|------|------|
| `bridge/FusionBridge.kt` | **全新编写** | 原生侧 6 通道桥接实现（container/terminal/adb/mcp/watchdog/workspace） |
| `mcp/McpManager.kt` | **全新编写** | MCP 适配器，对接 Kelivo UI 与 Operit MCP 底层，不重复实现 MCP 协议 |

### C++ Native 层

| 文件 | 来源 | 说明 |
|------|------|------|
| `cpp/native_bridge.cpp` | **全新编写** | 引入 Operit 底层 C++ 容器/ADB 原生代码，预留 FFI 高性能调用入口 |
| `cpp/CMakeLists.txt` | **全新编写** | Native 构建配置，引入 Operit C++ 子模块路径 |

---

## 一、项目概况

| 项目 | Kelivo | Operit |
|------|--------|--------|
| 仓库 | Chevey339/kelivo | AAswordman/Operit |
| 技术栈 | Flutter/Dart + Kotlin | Kotlin/Compose + Native |
| 许可证 | AGPL-3.0 | LGPL-3.0 |
| 定位 | 跨平台 LLM 聊天客户端 | Android 端 AI Agent 平台 |
| 包体 | ~50MB | ~380MB |

---

## 二、功能清单拆分

### Kelivo 保留部分（UI/会话层）
- ✅ Flutter 跨平台 UI 界面（Material You + 动态主题）
- ✅ 对话会话管理（分支、迁移、自动总结）
- ✅ 模型选择面板（OpenAI/Anthropic/Gemini/自定义）
- ✅ MCP 服务配置 UI（服务器管理、工具调用）
- ✅ 对话持久化存储（SQLite/Drift）
- ✅ 移动端适配交互（侧边栏、手势、响应式）
- ✅ 会话侧边栏（历史分组、搜索、锁定）
- ✅ 多模态输入（图片、PDF、Word、语音）
- ✅ Web 搜索（12+ 搜索引擎）
- ✅ TTS/STT 语音对话

### Operit 移植部分（原生能力层）
- 🔄 内置 Ubuntu 容器管理（PRoot 用户空间）
- 🔄 ADB 手机自动化（无线调试、点击、滑动、截图）
- 🔄 super_admin 终端 MCP 工具（STDIO 命令执行）
- 🔄 文件工作区管理（项目模板、代码编辑）
- 🔄 命令执行沙箱（权限控制、环境隔离）
- 🔄 日志捕获（Logcat、命令输出）
- 🔄 长任务进程管理（超时保护、心跳检测）
- 🔄 UI 自动化（无障碍、Shizuku、Root 通道）
- 🔒 角色/记忆系统（后续阶段考虑）
- 🔒 工作流引擎（后续阶段考虑）

### 融合增强（独有功能）
- 🆕 全局工作区绑定（新建会话不用重新绑定）
- 🆕 内存保护（Gradle 内存限制、OOM 防护）
- 🆕 心跳超时保护（5/10 分钟自动捕获日志）
- 🆕 工程优化（vfs 关闭、编译优化）

---

## 三、重复模块处理

| 模块 | Kelivo 实现 | Operit 实现 | 融合方案 |
|------|-----------|-----------|---------|
| MCP 客户端 | Dart 层 mcp_client | Kotlin 层 MCPRepository | 保留 Kelivo UI + Operit 底层执行 |
| 会话存储 | Drift/SQLite | ObjectBox/Room | 保留 Kelivo 实现 |
| 终端 | 无 | 完整终端系统 | 移植 Operit terminal |
| 容器 | 无 | PRoot + Ubuntu | 移植 Operit 容器 |
| ADB 自动化 | 无 | 完整 ADB 系统 | 移植 Operit 自动化 |
| 本地推理 | 无 | MNN/llama.cpp | 可选移植（后续阶段） |

---

## 四、已知缺陷与增强

| 缺陷 | 来源 | 融合方案 |
|------|------|---------|
| 缺少内存保护 | Operit | 新增 Gradle 内存限制、OOM 保护（ContainerConfig） |
| 进程卡死无恢复 | Operit | 新增心跳超时保护（5/10 分钟）（ProcessWatchdog） |
| 会话重新绑定工作区 | Operit | 改为全局工作区绑定（WorkspaceManager） |
| 无 Linux 容器 | Kelivo | 移植 Operit 容器模块 |
| 无 ADB 自动化 | Kelivo | 移植 Operit 自动化模块 |

---

## 五、依赖清单

### Flutter 侧（精简版 - 仅保留核心）
- sdk: ^3.12.1, flutter: ">=3.44.9"
- provider: ^6.0.5（状态管理）
- shared_preferences: ^2.2.3（本地存储）
- http: ^1.5.0, dio: 5.9.2（网络）
- uuid: ^4.5.0, path: ^1.9.1, path_provider: ^2.1.4
- intl: ^0.20.2（国际化）
- cupertino_icons: ^1.0.8, flutter_slidable: ^3.0.1

### Android 侧（Operit 核心依赖）
- compileSdk: 36, minSdk: 26, targetSdk: 36
- Kotlin 2.2.20 + Compose BOM
- Shizuku API 13.1.5
- libsu 6.0.0 (Root)
- OkHttp 4.12.0, Gson 2.10.1
- kotlinx-coroutines 1.10.2
- androidx.core-ktx 1.18.0, lifecycle 2.7.0
- io.modelcontextprotocol:kotlin-sdk-client 0.10.0

### 冲突排查
| 依赖 | Kelivo 版本 | Operit 版本 | 解决方案 |
|------|-----------|-----------|---------|
| compileSdk | 36 | 36 | 统一 36 |
| minSdk | 21 | 26 | 统一 26（容器需要） |
| Kotlin | 1.9.x | 2.2.20 | 统一 2.2.20 |
| SQLite | drift | Room | 共存（不同模块） |

---

## 六、开源协议合规

| 项目 | 协议 | 合规要求 |
|------|------|---------|
| Kelivo | AGPL-3.0 | 修改必须开源、保留版权声明 |
| Operit | LGPL-3.0 | 修改必须开源、保留版权声明 |
| 融合项目 | AGPL-3.0（从严） | 双协议声明、保留原作者版权声明 |

---

## 七、6 个 MethodChannel

| 通道名 | 功能 | 核心方法 |
|--------|------|---------|
| `container` | 容器管理 | getStatus, start, stop, executeCommand, downloadRootfs, getConfig |
| `terminal` | 终端命令 | createSession, executeCommand, closeSession, getActiveSessions |
| `adb` | ADB 自动化 | connect, disconnect, tap, swipe, screenshot, startApp, stopApp |
| `mcp` | MCP 工具 | listServers, addServer, removeServer, startServer, stopServer, callTool |
| `watchdog` | 看门狗 | startMonitoring, stopMonitoring, heartbeat, getActiveProcesses |
| `workspace` | 工作区 | bindSession, unbindSession, createFile, readFile, listDirectory |

---

## 八、阶段1详细执行计划

### 目标

把 Kelivo 的聊天对话、会话管理、模型选择、会话持久化核心 UI 与业务逻辑迁移到 `kelivo-operit-fusion` 项目。

**完成标准：** App 可正常打开 → 新建对话 → 选择模型 → 填入 API Key → 发送消息接收 AI 回复 → 会话持久化不丢失。

### 需要从 Kelivo 复制的文件清单

#### 1. 核心模型层 (`lib/core/models/`)

| 文件 | 说明 |
|------|------|
| `chat_message.dart` | 消息模型（role, content, timestamp, status） |
| `chat_message.g.dart` | JSON 序列化生成代码 |
| `conversation.dart` | 会话模型（id, title, messages, createdAt） |
| `conversation.g.dart` | JSON 序列化 |
| `message_part.dart` | 消息部分（text, image, tool_call） |
| `model_types.dart` | 模型类型定义（ModelInfo, ModelCapabilities） |
| `api_keys.dart` | API 密钥模型 |
| `provider_group.dart` | 模型提供方分组 |
| `preset_message.dart` | 预设消息模板 |
| `token_usage.dart` | Token 用量统计 |
| `auto_retry_options.dart` | 自动重试配置 |
| `compress_context_options.dart` | 上下文压缩配置 |

#### 2. 状态管理层 (`lib/core/providers/`)

| 文件 | 说明 |
|------|------|
| `settings_provider.dart` | 全局设置（主题、模型、API 配置）— **250KB，核心文件** |
| `model_provider.dart` | 模型注册表、模型选择、模型能力检测 |
| `user_provider.dart` | 用户信息（名称、头像） |
| `assistant_provider.dart` | 助手配置 |
| `tag_provider.dart` | 标签管理 |
| `quick_phrase_provider.dart` | 快捷短语 |
| `instruction_injection_provider.dart` | 指令注入 |
| `world_book_provider.dart` | 世界书 |
| `backup_provider.dart` | 备份管理 |
| `local_snapshot_provider.dart` | 本地快照 |
| `tts_provider.dart` | TTS 语音合成 |
| `asr_provider.dart` | ASR 语音识别 |
| `update_provider.dart` | 应用更新 |
| `hotkey_provider.dart` | 桌面快捷键 |
| `workspace_provider.dart` | 工作区绑定 |
| `memory_provider.dart` / `memory_provider_v2.dart` | 记忆系统 |
| `s3_backup_provider.dart` | S3 备份 |
| `backup_reminder_provider.dart` | 备份提醒 |
| `environment_provider.dart` | 环境变量 |
| `external_mounts_provider.dart` | 外部挂载 |

#### 3. 核心服务层 (`lib/core/services/`)

| 目录/文件 | 说明 |
|-----------|------|
| `storage/` | 本地存储（Hive/SQLite） |
| `chat/` | 聊天核心服务（消息发送、流式接收） |
| `api/` | API 请求（OpenAI/Anthropic/Gemini 兼容） |
| `auth/` | 认证（OAuth、API Key） |
| `network/` | 网络层（Dio HTTP 客户端） |
| `logging/` | 日志系统 |
| `search/` | Web 搜索 |
| `tts/` | TTS 服务 |
| `asr/` | ASR 服务 |
| `backup/` | 备份恢复 |
| `memory/` | 记忆管道 |
| `tools/` | 工具调用 |
| `scheduled_tasks_service.dart` | 定时任务 |
| `api_key_manager.dart` | API 密钥管理 |
| `notification_service.dart` | 通知服务 |
| `mobile_background.dart` | 后台运行 |
| `haptics.dart` | 震动反馈 |
| `screen_wakelock.dart` | 屏幕常亮 |
| `incoming_share_service.dart` | 分享接收 |
| `custom_request_merger.dart` | 自定义请求合并 |
| `model_override_payload_parser.dart` | 模型覆盖解析 |
| `model_override_resolver.dart` | 模型覆盖解析器 |
| `provider_balance_service.dart` | 提供方余额 |
| `legacy_data_retirement_service.dart` | 旧数据清理 |
| `learning_mode_store.dart` | 学习模式 |
| `instruction_injection_store.dart` | 指令注入存储 |
| `world_book_store.dart` | 世界书存储 |
| `world_book_activation.dart` | 世界书激活 |
| `quick_phrase_store.dart` | 快捷短语存储 |
| `memory_store.dart` | 记忆存储 |
| `json_blob_store.dart` | JSON 存储 |
| `hive_migration_marker.dart` | Hive 迁移标记 |
| `app_exit_flush.dart` | 退出刷新 |
| `background_icon_store.dart` | 后台图标 |
| `desktop_scheduled_tasks.dart` | 桌面定时任务 |
| `desktop_power_state.dart` | 桌面电源状态 |
| `android_process_text.dart` | Android 进程文本 |
| `native_file_save.dart` | 原生文件保存 |

#### 4. 聊天页面 (`lib/features/chat/`)

| 文件/目录 | 说明 |
|-----------|------|
| `pages/` | 聊天主页面、消息列表、输入框 |
| `widgets/` | 消息气泡、输入栏、工具面板、选择栏 |
| `models/` | 聊天状态模型 |
| `utils/` | 聊天工具函数 |
| `controllers/` | 聊天控制器 |

#### 5. 首页 (`lib/features/home/`)

| 文件/目录 | 说明 |
|-----------|------|
| `pages/home_page.dart` | 主页面（含侧边栏、聊天区、输入区） |
| `pages/home_mobile_layout.dart` | 移动端布局 |
| `pages/home_desktop_layout.dart` | 桌面端布局 |
| `widgets/` | 消息列表、输入栏、工具面板、滚动按钮 |
| `controllers/` | 首页控制器、滚动控制器 |
| `utils/` | 首页工具函数 |
| `services/` | 首页服务 |

#### 6. 模型选择 (`lib/features/model/`)

| 文件/目录 | 说明 |
|-----------|------|
| `pages/` | 模型选择页面、默认模型设置 |
| `widgets/` | 模型选择弹窗、模型列表 |
| `utils/` | 模型显示辅助、模型能力检测 |

#### 7. 提供方管理 (`lib/features/provider/`)

| 文件/目录 | 说明 |
|-----------|------|
| `pages/` | 提供方列表、提供方配置 |
| `widgets/` | 提供方卡片、配置表单 |

#### 8. 设置页面 (`lib/features/settings/`)

| 文件/目录 | 说明 |
|-----------|------|
| `pages/` | 设置主页、显示设置、网络代理、存储、关于 |
| `widgets/` | 设置卡片、列表项 |
| `services/` | 存储用量服务 |
| `logs/` | 日志查看器 |

#### 9. 助手配置 (`lib/features/assistant/`)

| 文件/目录 | 说明 |
|-----------|------|
| `pages/` | 助手设置页面 |
| `widgets/` | 助手卡片、配置表单 |
| `utils/` | 助手工具函数 |

#### 10. 共享组件 (`lib/shared/`)

| 目录 | 说明 |
|------|------|
| `animations/` | 动画组件 |
| `cache/` | 缓存组件 |
| `dialogs/` | 通用弹窗 |
| `pages/` | 通用页面 |
| `responsive/` | 响应式布局 |
| `utils/` | 通用工具 |
| `widgets/` | 通用组件（抽屉、表单、加载、提示条） |

#### 11. 主题系统 (`lib/theme/`)

| 文件 | 说明 |
|------|------|
| `theme_factory.dart` | 主题工厂（生成 ThemeData） |
| `theme_provider.dart` | 主题状态管理 |
| `palettes.dart` | 调色板 |
| `custom_theme.dart` | 自定义主题 |
| `chat_bubble_style.dart` | 聊天气泡样式 |
| `app_font_weights.dart` | 字体粗细 |
| `app_semantic_colors.dart` | 语义颜色 |
| `design_tokens.dart` | 设计令牌 |
| `surface_ladder.dart` | 表面层次 |

#### 12. 国际化 (`lib/l10n/`)

| 文件 | 说明 |
|------|------|
| `app_en.arb` | 英文翻译 |
| `app_zh.arb` | 中文翻译 |
| `app_zh_Hans.arb` | 简体中文翻译 |
| `app_zh_Hant.arb` | 繁体中文翻译 |
| `app_localizations.dart` | 国际化入口 |
| `app_localizations_en.dart` | 英文实现 |
| `app_localizations_zh.dart` | 中文实现 |

#### 13. 数据库层 (`lib/core/database/`)

| 文件 | 说明 |
|------|------|
| `app_database.dart` | 数据库入口 |
| `app_database.g.dart` | 数据库生成代码 |
| `business_preferences.dart` | 业务偏好存储 |
| `business_repository.dart` | 业务数据仓库 |
| `business_settings_router.dart` | 设置路由 |
| `business_settings_merger.dart` | 设置合并 |
| `chat_database_repository.dart` | 聊天数据仓库 |
| `chat_database_gateway.dart` | 聊天数据库网关 |
| `chat_database_observer.dart` | 聊天数据库观察者 |
| `schema_versions.dart` | 数据库版本 |
| `schema_migrations.dart` | 数据库迁移 |
| `backup_portability.dart` | 备份可移植性 |
| `business_data.dart` | 业务数据 |
| `business_migration_engine.dart` | 迁移引擎 |
| `business_restore_service.dart` | 恢复服务 |
| `business_startup_gate.dart` | 启动门控 |
| `database_installation_gate.dart` | 安装门控 |
| `extension_entity_store.dart` | 扩展实体存储 |
| `generation_run.dart` | 生成运行 |
| `generation_run_commands.dart` | 生成运行命令 |
| `sqlite_interrupt.dart` | SQLite 中断 |
| `startup_failure_report.dart` | 启动失败报告 |
| `startup_recovery_service.dart` | 启动恢复服务 |

#### 14. 依赖库 (`dependencies/`)

| 目录 | 说明 |
|------|------|
| `gpt_markdown/` | Markdown 渲染 |
| `mcp_client/` | MCP 客户端（仅 Dart 层 UI 部分） |
| `terminal_view/` | 终端视图 |
| `tray_manager/` | 托盘管理 |
| `flutter_math_fork/` | LaTeX 渲染 |
| `flutter_tts/` | TTS 本地实现 |
| `downsize/` | 图片压缩 |

### 需要剔除的 Kelivo 代码（重点）

| 剔除项 | 原因 |
|--------|------|
| Kelivo 原生 MCP 客户端原生通道 | 本项目使用 `fusion_bridge` 6 通道 + `McpManager.kt` 适配器 |
| Kelivo 自带的原生 MethodChannel | 统一使用 `fusion_bridge.dart` |
| Kelivo 的 Android 原生 Kotlin 代码 | 原生部分全部由本项目自己实现 |
| `lib/desktop/` 桌面端代码 | 本项目仅 Android |
| `lib/icons/` 图标适配器 | 使用 `lucide_icons_flutter` 直接替代 |
| `lib/secrets/` 密钥回退 | 本项目自行管理 |
| `lib/utils/sandbox_path_resolver.dart` | 沙箱路径解析（桌面端特有） |
| `lib/utils/avatar_cache.dart` | 头像缓存（桌面端特有） |
| `lib/utils/app_directories.dart` | 应用目录（桌面端特有） |
| `lib/utils/provider_grouping_logic.dart` | 提供方分组逻辑（桌面端特有） |
| `lib/utils/brand_assets.dart` | 品牌资源（桌面端特有） |
| `lib/utils/image_compressor.dart` | 图片压缩（桌面端特有） |
| `lib/utils/openai_model_compat.dart` | OpenAI 模型兼容（桌面端特有） |
| `lib/utils/kimi_model_compat.dart` | Kimi 模型兼容（桌面端特有） |
| `lib/core/services/sandbox/` | 沙箱服务（桌面端特有） |
| `lib/core/services/workspace/` | 工作区服务（桌面端特有） |
| `lib/core/services/skills/` | 技能服务（桌面端特有） |
| `lib/core/services/migration/` | 迁移服务（桌面端特有） |
| `lib/core/services/fonts/` | 字体服务（桌面端特有） |
| `lib/core/services/desktop_power_state.dart` | 桌面电源状态 |
| `lib/core/services/desktop_scheduled_tasks.dart` | 桌面定时任务 |
| `lib/features/migration/` | 迁移页面（桌面端特有） |
| `lib/features/scan/` | 扫描页面（桌面端特有） |
| `lib/features/stats/` | 统计页面（桌面端特有） |
| `lib/features/translate/` | 翻译页面（桌面端特有） |
| `lib/features/workspace/` | 工作区页面（桌面端特有） |
| `lib/features/world_book/` | 世界书页面（桌面端特有） |
| `lib/features/instruction_injection/` | 指令注入页面（桌面端特有） |
| `lib/features/quick_phrase/` | 快捷短语页面（桌面端特有） |
| `lib/features/scheduled_tasks/` | 定时任务页面（桌面端特有） |
| `lib/features/backup/` | 备份页面（桌面端特有） |
| `lib/features/search/` | 搜索页面（桌面端特有） |
| `lib/features/mcp/` | MCP 页面（使用本项目自己的 `mcp_page.dart`） |

### 包名替换映射

| Kelivo 原包名 | 融合项目包名 |
|---------------|-------------|
| `package:Kelivo/` | `package:kelivo_operit_fusion/` |
| `com.psyche.kelivo` | `com.kelivo.operit.fusion` |

### 执行步骤

1. **复制模型层** → 修改包名导入
2. **复制状态管理** → 修改包名导入，移除桌面端特有 Provider
3. **复制服务层** → 修改包名导入，移除桌面端特有服务
4. **复制聊天页面** → 修改包名导入，移除桌面端布局
5. **复制首页** → 修改包名导入，使用移动端布局
6. **复制模型选择** → 修改包名导入
7. **复制提供方管理** → 修改包名导入
8. **复制设置页面** → 修改包名导入，移除桌面端设置项
9. **复制助手配置** → 修改包名导入
10. **复制共享组件** → 修改包名导入
11. **复制主题系统** → 修改包名导入
12. **复制国际化** → 修改包名导入
13. **复制数据库层** → 修改包名导入
14. **复制依赖库** → 保持 path 依赖
15. **更新 `pubspec.yaml`** → 添加所有依赖
16. **更新 `main.dart`** → 适配本项目包名
17. **编译测试** → `flutter analyze` + `flutter build apk`

### 阶段1验收标准

- [ ] 编译运行 App，可正常打开
- [ ] 可以新建对话会话、切换会话
- [ ] 可以选择模型、填入 API 密钥，发送消息接收 AI 回复
- [ ] 会话记录本地持久化，关闭 App 重新打开会话不丢失
- [ ] 此时不调用任何容器、终端、ADB、看门狗相关能力

### 阶段1完成之后的下一步

完成阶段1验收通过，再进入阶段2：搭建 6 通道 FusionBridge 桥接框架。

---

### 为什么不先做模块移植？

如果先移植底层原生模块，UI 框架没搭建完成，你没法在界面上触发、调试底层逻辑，只能写单元测试，调试效率很低。
先把 Kelivo 的聊天核心 UI 跑起来，再一层层往下对接底层能力，排错更直观。

### 补充说明

阶段1 **只做 Dart Flutter 层**，完全不动 Android 原生代码。
把 Kelivo 的聊天核心跑通，之后再去对接原生底层模块。
好处：UI 问题、状态管理问题可以优先排查，后续接入底层模块时，只需要专注原生‑Dart 通信，不会混淆聊天业务 bug 和底层模块 bug。

---

## 九、开发执行阶段规划（8 阶段总览）

### 阶段1：项目骨架搭建，导入 Kelivo 核心 UI
1. 将 Kelivo 聊天对话页、模型选择页、会话管理、会话持久化逻辑迁移到本项目。
2. 保证 App 可以正常运行基础 AI 对话能力。

### 阶段2：搭建 Dart‑Native 桥接框架
1. 实现 `fusion_bridge.dart` 与 `FusionBridge.kt`，定义 6 个通信通道，完成空接口实现。
2. 验证 Dart 与原生双向通信链路通畅。

### 阶段3：移植 Operit 原生底层模块
1. 移植改造 `ContainerManager`、`TerminalManager`、`AdbManager`。
2. 接入 C++ Native 层，引入 Operit 底层容器与 ADB 原生代码。

### 阶段4：编写融合项目独有新增模块
1. `ProcessWatchdog.kt` 看门狗超时保护
2. `WorkspaceManager.kt` 全局统一工作区
3. `ContainerConfig.kt` 容器运行配置
4. `FusionConfig.kt`、`FusionLog.kt` 全局配置与日志工具

### 阶段5：实现 MCP 适配器
`McpManager.kt` 做适配层，复用 Kelivo MCP UI，对接 Operit MCP 底层实现。

### 阶段6：实现专属功能页面
开发 container、automation、workspace、watchdog、settings 页面，对接桥接通道。

### 阶段7：全链路联调测试
验证容器、终端、ADB 自动化、看门狗、全局工作区、MCP 工具调用完整流程。

### 阶段8：构建打包
编写 `fusion_build.sh` 脚本，编译生成 APK，整机真机测试。

---

### 为什么不先做模块移植？

如果先移植底层原生模块，UI 框架没搭建完成，你没法在界面上触发、调试底层逻辑，只能写单元测试，调试效率很低。
先把 Kelivo 的聊天核心 UI 跑起来，再一层层往下对接底层能力，排错更直观。

### 推荐执行顺序（原则：先 UI 骨架，再底层模块）

1. **第一步**：复制导入 Kelivo 核心 UI 页面（聊天对话页、模型选择页、会话侧边栏）
   - 把 Kelivo 原有：对话聊天界面、模型切换下拉、会话列表、对话持久化存储逻辑，复制到本项目 `lib/core`、`lib/features` 目录。
   - 保留 Kelivo 原本的会话状态管理，只做适配修改：不要改动原有聊天核心逻辑。
   - 完成后：App 可以正常打开、新建对话、切换大模型、发送消息，基础聊天能力完整可用。

2. **第二步**：搭建 6 通道 FusionBridge 桥接框架
   - 完成 Dart 侧 `fusion_bridge.dart` + Kotlin 侧 `FusionBridge.kt`，定义好 6 个通道的方法签名。
   - 先做空实现，Dart 可以调用原生、原生可以回传消息，保证桥接通信链路通。
   - 此时还不实现业务逻辑，只保证通信框架正常。

3. **第三步**：移植 Operit 原生底层模块，依次移植改造：
   `ContainerManager.kt` → `TerminalManager.kt` → `AdbManager.kt`
   - 把 Operit 的容器、终端、ADB 代码迁移过来，适配本项目的包名、桥接接口。
   - 同时引入 C++ native 层，把 Operit 底层 so 编译接入。

4. **第四步**：编写融合项目独有的三个全新模块
   `ProcessWatchdog.kt`（看门狗）、`WorkspaceManager.kt`（全局工作区）、`ContainerConfig.kt`（容器配置）
   - 同时实现 `FusionConfig.kt`、`FusionLog.kt` 统一日志与全局配置。

5. **第五步**：实现 `McpManager.kt` 适配器
   - UI 复用 Kelivo MCP 页面，底层对接 Operit 的 MCP STDIO/SSE 实现，做转发适配器，不要重复实现 MCP 协议。

6. **第六步**：开发新增功能页面
   `container_page.dart`、`automation_page.dart`、`workspace_page.dart`、`watchdog_page.dart`、`settings_page.dart`
   - 把 UI 页面和底层桥接通道对接，实现界面可以控制容器、工作区、看门狗状态。

7. **第七步**：联调完整链路
   - 测试完整流程：AI 对话中调用 `super_admin:terminal`、ADB 自动化；测试看门狗超时杀进程；测试全局工作区多会话共享。

8. **第八步**：编写 `fusion_build.sh` 构建脚本，打包 APK 做整机测试。
