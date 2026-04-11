package com.answufeng.store

import android.content.Context
import com.tencent.mmkv.MMKV

/**
 * brick-store 模块初始化入口，基于腾讯 MMKV 提供高性能键值存储。
 *
 * ### 初始化
 * 在 `Application.onCreate()` 中调用：
 * ```kotlin
 * BrickStore.init(this)
 * ```
 *
 * ### 自定义存储目录
 * ```kotlin
 * BrickStore.init(this, rootDir = "${filesDir}/mmkv_custom")
 * ```
 */
object BrickStore {

    @Volatile
    private var initialized = false

    /**
     * 初始化 MMKV。整个应用生命周期只需调用一次。
     *
     * @param context 任意 Context（内部自动取 applicationContext）
     * @param rootDir 自定义存储根目录，为 null 时使用默认目录（`files/mmkv`）
     * @return MMKV 根目录路径
     */
    fun init(context: Context, rootDir: String? = null): String {
        if (initialized) return MMKV.getRootDir()
        synchronized(this) {
            if (initialized) return MMKV.getRootDir()
            val result = if (rootDir != null) {
                MMKV.initialize(context.applicationContext, rootDir)
            } else {
                MMKV.initialize(context.applicationContext)
            }
            initialized = true
            return result
        }
    }

    /**
     * 确保已初始化，否则抛出异常。
     */
    internal fun ensureInitialized() {
        check(initialized) {
            "BrickStore 尚未初始化，请先在 Application.onCreate() 中调用 BrickStore.init(context)"
        }
    }

    /**
     * 是否已初始化。
     */
    val isInitialized: Boolean get() = initialized
}
