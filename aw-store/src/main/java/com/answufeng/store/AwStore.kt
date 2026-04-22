package com.answufeng.store

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import com.tencent.mmkv.MMKV
import java.io.File

/**
 * MMKV 存储库的全局入口。
 *
 * 负责初始化 MMKV 引擎，必须在 [android.app.Application.onCreate] 中调用 [init]。
 *
 * ```kotlin
 * class MyApp : Application() {
 *     override fun onCreate() {
 *         super.onCreate()
 *         AwStore.init(this, logEnabled = BuildConfig.DEBUG)
 *     }
 * }
 * ```
 */
object AwStore {

    @Volatile
    private var initialized = false

    /**
     * 初始化 MMKV。整个应用生命周期只需调用一次。
     *
     * 若重复调用且传入与首次不一致的 [rootDir]，将始终 **Log.w** 告警；在 **debuggable**
     * 包上会额外抛出 [IllegalStateException]，避免静默误用。重复调用时仍会应用最新的 [logEnabled]。
     *
     * @param context 任意 Context（内部自动取 applicationContext）
     * @param rootDir 自定义存储根目录，为 null 时使用默认目录（`files/mmkv`）
     * @param logEnabled 是否启用调试日志，默认 false
     */
    fun init(context: Context, rootDir: String? = null, logEnabled: Boolean = false) {
        val appContext = context.applicationContext
        synchronized(this) {
            if (initialized) {
                if (rootDir != null && !sameRootDirectory(MMKV.getRootDir(), rootDir)) {
                    val msg =
                        "AwStore: MMKV already initialized at '${MMKV.getRootDir()}', " +
                            "cannot switch to rootDir='$rootDir'. Use a single init with the intended path."
                    Log.w("AwStore", msg)
                    if (isDebuggable(appContext)) {
                        error(msg)
                    }
                }
                AwStoreLogger.enabled = logEnabled
                return
            }
            rootDir?.let { MMKV.initialize(appContext, it) } ?: MMKV.initialize(appContext)
            AwStoreLogger.enabled = logEnabled
            initialized = true
            AwStoreLogger.d("AwStore.init: complete, rootDir=${MMKV.getRootDir()}")
        }
    }

    private fun isDebuggable(context: Context): Boolean =
        (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    private fun sameRootDirectory(current: String, requested: String): Boolean {
        return try {
            File(current).canonicalPath == File(requested).canonicalPath
        } catch (_: Exception) {
            current.trimEnd('/', '\\') == requested.trimEnd('/', '\\')
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

    /**
     * 是否已初始化。
     *
     * 可用于检查 MMKV 是否已完成初始化，避免重复调用 [init]。
     * 在访问任何存储属性前应确保此值为 `true`。
     */
    val isInitialized: Boolean get() = initialized

    /** MMKV 根目录路径，访问前需确保已初始化 */
    val rootDir: String
        get() {
            ensureInitialized()
            return MMKV.getRootDir()
        }
}
