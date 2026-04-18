package com.answufeng.store

import android.util.Log

/**
 * aw-store 调试日志工具。
 *
 * 默认关闭，设置 [enabled] 为 `true` 后输出初始化、迁移等日志到 Logcat（Tag: `AwStore`）。
 *
 * ```kotlin
 * AwStoreLogger.enabled = BuildConfig.DEBUG
 * ```
 */
object AwStoreLogger {

    /** 是否启用日志输出，默认 `false` */
    var enabled: Boolean = false

    /** 输出 DEBUG 级别日志 */
    fun d(msg: String) {
        if (enabled) Log.d("AwStore", msg)
    }

    /** 输出 INFO 级别日志 */
    fun i(msg: String) {
        if (enabled) Log.i("AwStore", msg)
    }

    /** 输出 WARN 级别日志 */
    fun w(msg: String, t: Throwable? = null) {
        if (enabled) Log.w("AwStore", msg, t)
    }

    /** 输出 ERROR 级别日志 */
    fun e(msg: String, t: Throwable? = null) {
        if (enabled) Log.e("AwStore", msg, t)
    }
}
