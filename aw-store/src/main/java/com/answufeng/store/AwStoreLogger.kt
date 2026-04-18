package com.answufeng.store

import android.util.Log

/**
 * aw-store 调试日志工具。
 *
 * 默认关闭，设置 [enabled] 为 `true` 后输出初始化、迁移等日志到 Logcat（Tag: `AwStore`）。
 * 可通过 [setLogger] 自定义日志输出，例如重定向到文件或远程日志系统。
 *
 * ```kotlin
 * AwStoreLogger.enabled = BuildConfig.DEBUG
 *
 * // 自定义日志输出
 * AwStoreLogger.setLogger { level, tag, msg, t ->
 *     MyLogger.log(level, tag, msg, t)
 * }
 * ```
 */
object AwStoreLogger {

    /** 日志级别 */
    enum class Level {
        DEBUG, INFO, WARN, ERROR
    }

    /** 是否启用日志输出，默认 `false` */
    var enabled: Boolean = false

    @Volatile
    private var customLogger: ((level: Level, tag: String, msg: String, t: Throwable?) -> Unit)? = null

    /**
     * 设置自定义日志输出。
     *
     * 设置后，当 [enabled] 为 `true` 时，日志会通过此回调输出，
     * 不再输出到 Logcat。设置为 `null` 恢复默认 Logcat 输出。
     *
     * @param logger 自定义日志回调，参数为 (级别, 标签, 消息, 异常)
     */
    fun setLogger(logger: ((level: Level, tag: String, msg: String, t: Throwable?) -> Unit)?) {
        customLogger = logger
    }

    /** 输出 DEBUG 级别日志 */
    fun d(msg: String) {
        if (enabled) {
            customLogger?.invoke(Level.DEBUG, "AwStore", msg, null) ?: Log.d("AwStore", msg)
        }
    }

    /** 输出 INFO 级别日志 */
    fun i(msg: String) {
        if (enabled) {
            customLogger?.invoke(Level.INFO, "AwStore", msg, null) ?: Log.i("AwStore", msg)
        }
    }

    /** 输出 WARN 级别日志 */
    fun w(msg: String, t: Throwable? = null) {
        if (enabled) {
            customLogger?.invoke(Level.WARN, "AwStore", msg, t) ?: Log.w("AwStore", msg, t)
        }
    }

    /** 输出 ERROR 级别日志 */
    fun e(msg: String, t: Throwable? = null) {
        if (enabled) {
            customLogger?.invoke(Level.ERROR, "AwStore", msg, t) ?: Log.e("AwStore", msg, t)
        }
    }
}
