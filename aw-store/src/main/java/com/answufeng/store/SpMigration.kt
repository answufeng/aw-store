package com.answufeng.store

import android.content.Context
import com.tencent.mmkv.MMKV

/**
 * 从 SharedPreferences 迁移到 MMKV 的工具类。
 *
 * 将已有的 SharedPreferences 数据一键迁移到 MMKV 实例中，迁移完成后自动清除原 SP 数据。
 * 迁移是幂等的，已经迁移过的 SP 文件再次调用不会重复写入。
 *
 * ```kotlin
 * val result = SpMigration.migrate(this, "app_prefs")
 * if (!result.isSuccess) {
 *     Log.w("Migration", "Skipped keys: ${result.skippedKeys}")
 * }
 * ```
 */
object SpMigration {

    /**
     * 将指定 SharedPreferences 中的数据迁移到 MMKV。
     *
     * @param context 任意 Context
     * @param spName SharedPreferences 文件名
     * @param mmapId 目标 MMKV 实例 ID，为 null 时使用默认实例
     * @param cryptKey 目标 MMKV 的加密密钥，为 null 时不加密
     * @param deleteAfterMigration 迁移成功后是否清除原 SP 数据，默认 true。当存在失败项时不会删除
     * @return [MigrationResult] 迁移结果
     */
    fun migrate(
        context: Context,
        spName: String,
        mmapId: String? = null,
        cryptKey: String? = null,
        deleteAfterMigration: Boolean = true
    ): MigrationResult {
        AwStore.ensureInitialized()

        val sp = context.applicationContext.getSharedPreferences(spName, Context.MODE_PRIVATE)
        val all = sp.all
        if (all.isNullOrEmpty()) return MigrationResult(0, 0, 0, emptyList())

        val mmkv = resolveMmkv(mmapId, cryptKey)

        var successCount = 0
        var failedCount = 0
        val skippedKeys = mutableListOf<String>()

        for ((key, value) in all) {
            try {
                when (value) {
                    null -> {
                        skippedKeys.add(key)
                        AwStoreLogger.d("SpMigration: skipped null value for key=$key")
                    }
                    is String -> { mmkv.encode(key, value); successCount++ }
                    is Int -> { mmkv.encode(key, value); successCount++ }
                    is Long -> { mmkv.encode(key, value); successCount++ }
                    is Float -> { mmkv.encode(key, value); successCount++ }
                    is Boolean -> { mmkv.encode(key, value); successCount++ }
                    is Set<*> -> {
                        @Suppress("UNCHECKED_CAST")
                        mmkv.encode(key, value as Set<String>)
                        successCount++
                    }
                    else -> {
                        skippedKeys.add(key)
                        AwStoreLogger.e("SpMigration: skipped unknown type for key=$key, type=${value.javaClass.simpleName}")
                    }
                }
            } catch (e: Exception) {
                failedCount++
                AwStoreLogger.e("SpMigration: failed to migrate key=$key", e)
            }
        }

        if (deleteAfterMigration && failedCount == 0) {
            sp.edit().clear().commit()
        }

        val result = MigrationResult(all.size, successCount, failedCount, skippedKeys)
        AwStoreLogger.d("SpMigration: $result")
        return result
    }

    internal fun resolveMmkv(mmapId: String?, cryptKey: String?): MMKV {
        return when {
            mmapId != null && cryptKey != null -> MMKV.mmkvWithID(mmapId, MMKV.SINGLE_PROCESS_MODE, cryptKey)
            mmapId != null -> MMKV.mmkvWithID(mmapId)
            cryptKey != null -> MMKV.mmkvWithID("aw_crypt_${cryptKey.hashCode()}", MMKV.SINGLE_PROCESS_MODE, cryptKey)
            else -> MMKV.defaultMMKV()
        }
    }
}
