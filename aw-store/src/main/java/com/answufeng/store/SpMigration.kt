package com.answufeng.store

import android.content.Context
import android.content.SharedPreferences
import com.tencent.mmkv.MMKV

/**
 * 从 SharedPreferences 迁移到 MMKV 的工具类。
 *
 * 将已有的 SharedPreferences 数据一键迁移到 MMKV 实例中，迁移完成后自动清除原 SP 数据。
 *
 * ### 使用示例
 * ```kotlin
 * // 在 Application.onCreate() 中初始化后执行迁移
 * BrickStore.init(this)
 *
 * // 迁移默认 SP 到默认 MMKV
 * SpMigration.migrate(this, "app_prefs")
 *
 * // 迁移到指定 MMKV 实例
 * SpMigration.migrate(this, "app_prefs", mmapId = "user_store")
 * ```
 *
 * > 迁移是幂等的，已经迁移过的 SP 文件再次调用不会重复写入。
 */
object SpMigration {

    /**
     * 将指定 SharedPreferences 中的数据迁移到 MMKV。
     *
     * @param context 任意 Context
     * @param spName SharedPreferences 文件名
     * @param mmapId 目标 MMKV 实例 ID，为 null 时使用默认实例
     * @param cryptKey 目标 MMKV 的加密密钥，为 null 时不加密
     * @param deleteAfterMigration 迁移成功后是否清除原 SP 数据，默认 true
     * @return 迁移的键值对数量
     */
    fun migrate(
        context: Context,
        spName: String,
        mmapId: String? = null,
        cryptKey: String? = null,
        deleteAfterMigration: Boolean = true
    ): Int {
        BrickStore.ensureInitialized()

        val sp = context.applicationContext.getSharedPreferences(spName, Context.MODE_PRIVATE)
        val all = sp.all
        if (all.isNullOrEmpty()) return 0

        val mmkv = when {
            mmapId != null && cryptKey != null -> MMKV.mmkvWithID(mmapId, MMKV.SINGLE_PROCESS_MODE, cryptKey)
            mmapId != null -> MMKV.mmkvWithID(mmapId)
            cryptKey != null -> MMKV.mmkvWithID("default", MMKV.SINGLE_PROCESS_MODE, cryptKey)
            else -> MMKV.defaultMMKV()
        }

        var count = 0
        for ((key, value) in all) {
            when (value) {
                is String -> mmkv.encode(key, value)
                is Int -> mmkv.encode(key, value)
                is Long -> mmkv.encode(key, value)
                is Float -> mmkv.encode(key, value)
                is Boolean -> mmkv.encode(key, value)
                is Set<*> -> {
                    @Suppress("UNCHECKED_CAST") // Safe: SP only stores Set<String>
                    mmkv.encode(key, value as Set<String>)
                }
            }
            count++
        }

        if (deleteAfterMigration) {
            // 使用 commit() 同步清除，确保 MMKV 写入与 SP 清除的原子性，
            // 避免 apply() 异步清除在进程意外终止时导致数据重复。
            sp.edit().clear().commit()
        }

        return count
    }
}
