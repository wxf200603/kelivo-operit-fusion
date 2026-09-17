package com.kelivo.operit.fusion

import android.os.Bundle
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine

/**
 * 融合版主 Activity
 *
 * 阶段1：仅初始化 Flutter，不加载原生桥接
 * 后续阶段启用 FusionBridge
 */
class FusionActivity : FlutterActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        // TODO: 阶段2启用 FusionBridge
    }
}
