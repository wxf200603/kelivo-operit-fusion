package com.kelivo.operit.fusion.utils

import android.util.Log

/**
 * 统一日志工具
 */
object FusionLog {
    private const val TAG = "Fusion"

    fun d(tag: String, message: String) {
        Log.d("$TAG/$tag", message)
    }

    fun i(tag: String, message: String) {
        Log.i("$TAG/$tag", message)
    }

    fun w(tag: String, message: String) {
        Log.w("$TAG/$tag", message)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Log.e("$TAG/$tag", message, throwable)
    }
}
