package com.answufeng.store

/**
 * 存储配置数据类，用于 [MmkvDelegate] 的构造参数。
 *
 * 当配置参数较多时，使用配置类比多参数构造函数更清晰：
 *
 * ```kotlin
 * object SecureStore : MmkvDelegate(StoreConfig(
 *     mmapId = "secure",
 *     secureCryptKey = CryptKey.fromSecureRandom(),
 *     multiProcess = true
 * )) {
 *     var password by string()
 * }
 * ```
 *
 * @property mmapId MMKV 实例 ID，不同 ID 对应不同的存储文件，默认使用 MMKV 默认实例
 * @property cryptKey 加密密钥字符串，传入后使用 AES-CFB 加密，默认不加密。
 *                    不同 cryptKey 会自动使用不同的 MMKV 实例，避免数据覆盖。
 *                    推荐使用 [CryptKey] 替代直接传字符串。
 * @property secureCryptKey 安全加密密钥包装，优先级高于 [cryptKey]。
 *                          推荐使用 [CryptKey.fromSecureRandom] 生成安全随机密钥。
 * @property multiProcess 是否启用多进程模式，默认 false。
 *                         启用后使用 MMKV.MULTI_PROCESS_MODE，确保跨进程读写一致性。
 */
data class StoreConfig(
    val mmapId: String? = null,
    val cryptKey: String? = null,
    val secureCryptKey: CryptKey? = null,
    val multiProcess: Boolean = false
)
