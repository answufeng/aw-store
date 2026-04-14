package com.answufeng.store

import java.security.SecureRandom

/**
 * MMKV 加密密钥的安全包装类。
 *
 * 防止密钥在日志、调试输出中意外泄露。推荐使用 [fromSecureRandom] 生成安全随机密钥，
 * 避免在源码中硬编码密钥字符串。
 *
 * ```kotlin
 * // 推荐：安全随机密钥
 * object SecureStore : MmkvDelegate(secureCryptKey = CryptKey.fromSecureRandom()) {
 *     var password by string()
 * }
 *
 * // 从字符串创建（不推荐在源码中硬编码）
 * val key = CryptKey.fromString("my_secret_key")
 *
 * // 从字节数组创建
 * val key = CryptKey.fromBytes(byteArrayOf(0x01, 0x02, 0x03))
 * ```
 *
 * 注意：[toString] 返回 `"CryptKey(****)"`，不会暴露实际密钥内容。
 */
class CryptKey private constructor(private val key: String) {

    /** 密钥的字符串值，仅供内部传递给 MMKV 使用 */
    val value: String get() = key

    companion object {

        /**
         * 从字符串创建 [CryptKey]。
         *
         * 不推荐在源码中硬编码密钥字符串，建议使用 [fromSecureRandom]。
         */
        fun fromString(key: String): CryptKey = CryptKey(key)

        /**
         * 从字节数组创建 [CryptKey]。
         *
         * 字节数组会被转换为十六进制字符串作为密钥。
         */
        fun fromBytes(bytes: ByteArray): CryptKey {
            val hex = bytes.joinToString("") { "%02x".format(it) }
            return CryptKey(hex)
        }

        /**
         * 使用 [SecureRandom] 生成安全随机密钥。
         *
         * 这是创建加密密钥的推荐方式，每次应用安装后会生成不同的密钥。
         * 注意：卸载重装后密钥会变化，之前加密的数据将无法解密。
         *
         * @param length 随机字节长度，默认 16（128 位）
         */
        fun fromSecureRandom(length: Int = 16): CryptKey {
            val bytes = ByteArray(length).also { SecureRandom().nextBytes(it) }
            return fromBytes(bytes)
        }
    }

    override fun toString(): String = "CryptKey(****)"

    override fun equals(other: Any?): Boolean = other is CryptKey && other.key == key

    override fun hashCode(): Int = key.hashCode()
}
