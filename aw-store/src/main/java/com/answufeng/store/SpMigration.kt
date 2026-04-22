package com.answufeng.store

import android.content.Context
import com.tencent.mmkv.MMKV
import java.security.MessageDigest

/**
 * 从 SharedPreferences 迁移到 MMKV 的工具类。
 *
 * 使用 MMKV 原生 [MMKV.importFromSharedPreferences] 进行高效批量迁移。
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
     * @param cryptKey 目标 MMKV 的加密密钥字符串，为 null 时不加密
     * @param secureCryptKey 目标 MMKV 的安全加密密钥，优先级高于 [cryptKey]
     * @param multiProcess 是否使用多进程模式，默认 false
     * @param deleteAfterMigration 当 [MigrationResult.failedCount] 为 0 时是否在返回前同步清空原 SP
     *                            （[android.content.SharedPreferences.Editor.commit]）；
     *                            若存在失败项则**不会**清除，避免数据丢失，可修复后重试迁移。
     * @return [MigrationResult] 迁移结果
     */
    fun migrate(
        context: Context,
        spName: String,
        mmapId: String? = null,
        cryptKey: String? = null,
        secureCryptKey: CryptKey? = null,
        multiProcess: Boolean = false,
        deleteAfterMigration: Boolean = true
    ): MigrationResult {
        AwStore.ensureInitialized()

        val sp = context.applicationContext.getSharedPreferences(spName, Context.MODE_PRIVATE)
        val all = sp.all
        if (all.isNullOrEmpty()) return MigrationResult(0, 0, 0, emptyList())

        val effectiveCryptKey = secureCryptKey?.value ?: cryptKey
        val mmkv = resolveMmkv(mmapId, effectiveCryptKey, multiProcess)

        val importedCount = mmkv.importFromSharedPreferences(sp)
        val skippedKeys = all.filter { it.value == null }.keys.toList()
        val failedCount = all.size - importedCount - skippedKeys.size

        if (deleteAfterMigration && failedCount == 0) {
            sp.edit().clear().commit()
        }

        val result = MigrationResult(all.size, importedCount, failedCount, skippedKeys)
        AwStoreLogger.d("SpMigration: $result")
        return result
    }

    /**
     * 批量迁移多个 SharedPreferences 文件到 MMKV。
     *
     * 每个 SP 文件迁移到对应的 MMKV 实例（mmapId 默认等于 spName）。
     * 如果某个 SP 文件迁移失败，不影响其他文件的迁移。
     *
     * @param context 任意 Context
     * @param spNames SharedPreferences 文件名列表
     * @param deleteAfterMigration 迁移成功后是否清除原 SP 数据，默认 true
     * @return 每个 SP 文件的迁移结果列表，顺序与 [spNames] 一致
     */
    fun migrateAll(
        context: Context,
        spNames: List<String>,
        deleteAfterMigration: Boolean = true
    ): List<MigrationResult> {
        return spNames.map { spName ->
            migrate(context, spName, mmapId = spName, deleteAfterMigration = deleteAfterMigration)
        }
    }

    /**
     * 根据参数解析 MMKV 实例。
     *
     * 使用 SHA-256 前 64 位作为 cryptKey 的稳定标识，避免 String.hashCode() 碰撞。
     */
    internal fun resolveMmkv(mmapId: String?, cryptKey: String?, multiProcess: Boolean = false): MMKV {
        val mode = if (multiProcess) MMKV.MULTI_PROCESS_MODE else MMKV.SINGLE_PROCESS_MODE
        return when {
            mmapId != null && cryptKey != null -> MMKV.mmkvWithID(mmapId, mode, cryptKey)
            mmapId != null -> MMKV.mmkvWithID(mmapId, mode)
            cryptKey != null -> {
                val stableId = stableIdForCryptKey(cryptKey)
                MMKV.mmkvWithID("aw_crypt_$stableId", mode, cryptKey)
            }
            else -> if (multiProcess) MMKV.mmkvWithID("aw_default_multi", mode) else MMKV.defaultMMKV()
        }
    }

    /**
     * SHA-256 前 8 字节的十六进制，用作仅 [cryptKey] 时的 [mmapId] 后缀，避免用 [String.hashCode] 碰撞。
     * 为 [internal] 供同模块内部复用；行为变更视为兼容敏感变更。
     */
    internal fun stableIdForCryptKey(cryptKey: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(cryptKey.toByteArray())
        return digest.take(8).joinToString("") { "%02x".format(it) }
    }
}
