package com.answufeng.store

import android.content.Context
import com.tencent.mmkv.MMKV

object AwStore {

    @Volatile
    private var initialized = false

    /**
     * 初始化 MMKV。整个应用生命周期只需调用一次。
     *
     * @param context 任意 Context（内部自动取 applicationContext）
     * @param rootDir 自定义存储根目录，为 null 时使用默认目录（`files/mmkv`）
     */
    fun init(context: Context, rootDir: String? = null) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            if (rootDir != null) {
                MMKV.initialize(context.applicationContext, rootDir)
            } else {
                MMKV.initialize(context.applicationContext)
            }
            initialized = true
            AwStoreLogger.d("AwStore.init: complete, rootDir=${MMKV.getRootDir()}")
        }
    }

    /**
     * 确保已初始化，否则抛出 [IllegalStateException]。
     */
    internal fun ensureInitialized() {
        check(initialized) {
            "AwStore 尚未初始化，请先在 Application.onCreate() 中调用 AwStore.init(context)"
        }
    }

    /** 是否已初始化 */
    val isInitialized: Boolean get() = initialized

    /** MMKV 根目录路径，访问前需确保已初始化 */
    val rootDir: String
        get() {
            ensureInitialized()
            return MMKV.getRootDir()
        }
}
