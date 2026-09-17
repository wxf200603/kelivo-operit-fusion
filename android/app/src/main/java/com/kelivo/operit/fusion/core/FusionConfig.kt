package com.kelivo.operit.fusion.core

import android.content.Context
import android.content.SharedPreferences

/**
 * 应用全局配置
 */
class FusionConfig private constructor(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("fusion_config", Context.MODE_PRIVATE)

    companion object {
        @Volatile
        private var instance: FusionConfig? = null

        fun getInstance(context: Context): FusionConfig {
            return instance ?: synchronized(this) {
                instance ?: FusionConfig(context.applicationContext).also { instance = it }
            }
        }
    }

    var isFirstLaunch: Boolean
        get() = prefs.getBoolean("first_launch", true)
        set(value) = prefs.edit().putBoolean("first_launch", value).apply()

    var lastContainerPath: String?
        get() = prefs.getString("container_path", null)
        set(value) = prefs.edit().putString("container_path", value).apply()

    var autoStartContainer: Boolean
        get() = prefs.getBoolean("auto_start_container", false)
        set(value) = prefs.edit().putBoolean("auto_start_container", value).apply()
}
