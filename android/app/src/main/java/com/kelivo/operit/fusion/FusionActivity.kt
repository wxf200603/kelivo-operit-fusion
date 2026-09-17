package com.kelivo.operit.fusion

import android.os.Bundle
import com.kelivo.operit.fusion.bridge.FusionBridge
import com.kelivo.operit.fusion.config.ContainerConfig
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine

/**
 * 融合版主 Activity
 *
 * 负责初始化 FusionBridge 并注册到 Flutter Engine
 * 同时初始化全局配置和工作区
 */
class FusionActivity : FlutterActivity() {

    private lateinit var fusionBridge: FusionBridge

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 初始化容器配置（使用默认配置）
        val containerConfig = ContainerConfig()

        // 初始化桥接器
        fusionBridge = FusionBridge(this, containerConfig)
        fusionBridge.initialize()
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        fusionBridge.registerWithEngine(flutterEngine)
    }

    override fun onDestroy() {
        super.onDestroy()
        fusionBridge.dispose()
    }
}
