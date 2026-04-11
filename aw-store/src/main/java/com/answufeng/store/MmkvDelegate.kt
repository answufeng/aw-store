package com.answufeng.store

import android.content.Context
import android.os.Parcelable
import com.tencent.mmkv.MMKV
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * 基于 MMKV 的高性能键值存储属性委托，用法与 SharedPreferences 类似，但性能提升显著。
 *
 * ### 定义
 * ```kotlin
 * object UserStore : MmkvDelegate() {
 *     var token by string("token", "")
 *     var userId by long("user_id", 0L)
 *     var isLoggedIn by boolean("is_logged_in", false)
 *     var score by float("score", 0f)
 *     var tags by stringSet("tags")
 * }
 * ```
 *
 * ### 初始化（Application.onCreate 中调用一次）
 * ```kotlin
 * BrickStore.init(applicationContext)
 * ```
 *
 * ### 读写
 * ```kotlin
 * UserStore.token = "abc123"
 * val t = UserStore.token   // "abc123"
 * ```
 *
 * ### 加密存储
 * ```kotlin
 * object SecureStore : MmkvDelegate(cryptKey = "my_secret_key") {
 *     var password by string("password", "")
 * }
 * ```
 *
 * > **注意**：未调用 [BrickStore.init] 就访问属性会抛出 [IllegalStateException]。
 *
 * @param mmapId MMKV 实例 ID，不同 ID 对应不同的存储文件，默认使用 MMKV 默认实例
 * @param cryptKey 加密密钥，传入后该存储文件将使用 AES-CFB 加密，默认不加密
 */
open class MmkvDelegate(
    private val mmapId: String? = null,
    private val cryptKey: String? = null
) {

    private val mmkv: MMKV by lazy {
        BrickStore.ensureInitialized()
        when {
            mmapId != null && cryptKey != null -> MMKV.mmkvWithID(mmapId, MMKV.SINGLE_PROCESS_MODE, cryptKey)
            mmapId != null -> MMKV.mmkvWithID(mmapId)
            cryptKey != null -> MMKV.mmkvWithID("default", MMKV.SINGLE_PROCESS_MODE, cryptKey)
            else -> MMKV.defaultMMKV()
        }
    }

    /**
     * 清空当前 MMKV 实例中的所有键值对。
     */
    fun clear() {
        mmkv.clearAll()
    }

    /**
     * 删除指定 [key] 对应的键值对。
     *
     * @param key 要删除的键名
     */
    fun remove(key: String) {
        mmkv.removeValueForKey(key)
    }

    /**
     * 批量删除指定 [keys] 对应的键值对。
     *
     * @param keys 要删除的键名数组
     */
    fun remove(keys: Array<String>) {
        mmkv.removeValuesForKeys(keys)
    }

    /**
     * 检查指定 [key] 是否存在。
     *
     * @param key 键名
     * @return 是否包含该键
     */
    fun contains(key: String): Boolean = mmkv.containsKey(key)

    /**
     * 获取所有键名。
     *
     * @return 键名数组，如果为空返回空数组
     */
    fun allKeys(): Array<String> = mmkv.allKeys() ?: emptyArray()

    /**
     * 获取当前存储文件的总大小（字节）。
     */
    fun totalSize(): Long = mmkv.totalSize()

    // ==================== 类型委托工厂 ====================

    /**
     * String 类型属性委托。
     *
     * @param key MMKV 键名
     * @param default 默认值，默认为空字符串
     */
    fun string(key: String, default: String = "") = object : ReadWriteProperty<Any?, String> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): String {
            return mmkv.decodeString(key, default) ?: default
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
            mmkv.encode(key, value)
        }
    }

    /**
     * Int 类型属性委托。
     *
     * @param key MMKV 键名
     * @param default 默认值，默认为 0
     */
    fun int(key: String, default: Int = 0) = object : ReadWriteProperty<Any?, Int> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): Int {
            return mmkv.decodeInt(key, default)
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {
            mmkv.encode(key, value)
        }
    }

    /**
     * Long 类型属性委托。
     *
     * @param key MMKV 键名
     * @param default 默认值，默认为 0L
     */
    fun long(key: String, default: Long = 0L) = object : ReadWriteProperty<Any?, Long> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): Long {
            return mmkv.decodeLong(key, default)
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Long) {
            mmkv.encode(key, value)
        }
    }

    /**
     * Float 类型属性委托。
     *
     * @param key MMKV 键名
     * @param default 默认值，默认为 0f
     */
    fun float(key: String, default: Float = 0f) = object : ReadWriteProperty<Any?, Float> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): Float {
            return mmkv.decodeFloat(key, default)
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Float) {
            mmkv.encode(key, value)
        }
    }

    /**
     * Double 类型属性委托。
     *
     * @param key MMKV 键名
     * @param default 默认值，默认为 0.0
     */
    fun double(key: String, default: Double = 0.0) = object : ReadWriteProperty<Any?, Double> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): Double {
            return mmkv.decodeDouble(key, default)
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Double) {
            mmkv.encode(key, value)
        }
    }

    /**
     * Boolean 类型属性委托。
     *
     * @param key MMKV 键名
     * @param default 默认值，默认为 false
     */
    fun boolean(key: String, default: Boolean = false) = object : ReadWriteProperty<Any?, Boolean> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): Boolean {
            return mmkv.decodeBool(key, default)
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) {
            mmkv.encode(key, value)
        }
    }

    /**
     * ByteArray 类型属性委托。
     *
     * @param key MMKV 键名
     * @param default 默认值，默认为空 ByteArray
     */
    fun bytes(key: String, default: ByteArray = byteArrayOf()) = object : ReadWriteProperty<Any?, ByteArray> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): ByteArray {
            return mmkv.decodeBytes(key, default) ?: default
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: ByteArray) {
            mmkv.encode(key, value)
        }
    }

    /**
     * Set<String> 类型属性委托。
     *
     * @param key MMKV 键名
     * @param default 默认值，默认为空集合
     */
    fun stringSet(key: String, default: Set<String> = emptySet()) = object : ReadWriteProperty<Any?, Set<String>> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): Set<String> {
            return mmkv.decodeStringSet(key, default) ?: default
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Set<String>) {
            mmkv.encode(key, value)
        }
    }

    /**
     * Parcelable 类型属性委托。
     *
     * @param key MMKV 键名
     * @param clazz Parcelable 的 Class 对象
     * @param default 默认值
     */
    fun <T : Parcelable> parcelable(
        key: String,
        clazz: Class<T>,
        default: T? = null
    ) = object : ReadWriteProperty<Any?, T?> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): T? {
            return mmkv.decodeParcelable(key, clazz, default)
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: T?) {
            if (value != null) {
                mmkv.encode(key, value)
            } else {
                mmkv.removeValueForKey(key)
            }
        }
    }

    inline fun <reified T : Parcelable> parcelable(
        key: String,
        default: T? = null
    ) = parcelable(key, T::class.java, default)
}
