package com.answufeng.store

import android.os.Parcelable
import com.tencent.mmkv.MMKV
import com.tencent.mmkv.MMKVContentChangeNotification
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * 基于 MMKV 的高性能键值存储属性委托。
 *
 * 通过继承此类并使用类型委托工厂方法声明属性，即可实现类型安全的持久化存储。
 *
 * ```kotlin
 * object UserStore : MmkvDelegate() {
 *     var token by string("token", "")
 *     var userId by long("user_id", 0L)
 *     var isLoggedIn by boolean("is_logged_in", false)
 *     var score by float("score", 0f)
 *     var tags by stringSet("tags")
 *     var nickname by nullableString("nickname")
 * }
 * ```
 *
 * 加密存储：
 * ```kotlin
 * object SecureStore : MmkvDelegate(cryptKey = "my_secret_key") {
 *     var password by string("password", "")
 * }
 * ```
 *
 * > **注意**：未调用 [AwStore.init] 就访问属性会抛出 [IllegalStateException]。
 *
 * @param mmapId MMKV 实例 ID，不同 ID 对应不同的存储文件，默认使用 MMKV 默认实例
 * @param cryptKey 加密密钥，传入后该存储文件将使用 AES-CFB 加密，默认不加密。
 *                 不同 cryptKey 会自动使用不同的 MMKV 实例，避免数据覆盖
 */
open class MmkvDelegate(
    private val mmapId: String? = null,
    private val cryptKey: String? = null
) {

    private val mmkv: MMKV by lazy {
        AwStore.ensureInitialized()
        SpMigration.resolveMmkv(mmapId, cryptKey)
    }

    /** 清空当前 MMKV 实例中的所有键值对 */
    fun clear() {
        mmkv.clearAll()
    }

    /** 删除指定 [key] 对应的键值对 */
    fun remove(key: String) {
        mmkv.removeValueForKey(key)
    }

    /** 批量删除指定 [keys] 对应的键值对 */
    fun remove(keys: Array<String>) {
        mmkv.removeValuesForKeys(keys)
    }

    /** 检查指定 [key] 是否存在 */
    fun contains(key: String): Boolean = mmkv.containsKey(key)

    /** 获取所有键名，如果为空返回空数组 */
    fun allKeys(): Array<String> = mmkv.allKeys() ?: emptyArray()

    /** 获取当前存储文件的总大小（字节） */
    fun totalSize(): Long = mmkv.totalSize()

    /** 同步写入，等待数据写入磁盘完成后再返回 */
    fun sync() {
        mmkv.sync()
    }

    /** 异步写入，立即返回，数据在后台写入磁盘 */
    fun async() {
        mmkv.async()
    }

    /**
     * 注册跨进程数据变化监听。
     *
     * 当其他进程修改了 MMKV 数据时，[listener] 会被回调，参数为被修改的 MMKV 实例 ID（mmapID）。
     * 当前进程的修改不会触发回调。
     *
     * > 注意：此监听是全局注册的，多个 MmkvDelegate 实例调用此方法会互相覆盖。
     *
     * @param listener 回调函数，参数为被修改的 MMKV 实例 ID
     */
    fun registerContentChange(listener: (mmapID: String) -> Unit) {
        MMKV.registerContentChangeNotify(object : MMKVContentChangeNotification {
            override fun onContentChangedByOuterProcess(mmapID: String) {
                listener(mmapID)
            }
        })
    }

    /** 取消跨进程数据变化监听 */
    fun unregisterContentChange() {
        MMKV.unregisterContentChangeNotify()
    }

    /** String 类型属性委托 */
    fun string(key: String, default: String = "") = object : ReadWriteProperty<Any?, String> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): String {
            return mmkv.decodeString(key, default) ?: default
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
            mmkv.encode(key, value)
        }
    }

    /** Nullable String 类型属性委托，赋值 null 时删除对应键 */
    fun nullableString(key: String) = object : ReadWriteProperty<Any?, String?> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): String? {
            return mmkv.decodeString(key, null)
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: String?) {
            if (value != null) {
                mmkv.encode(key, value)
            } else {
                mmkv.removeValueForKey(key)
            }
        }
    }

    /** Int 类型属性委托 */
    fun int(key: String, default: Int = 0) = object : ReadWriteProperty<Any?, Int> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): Int {
            return mmkv.decodeInt(key, default)
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {
            mmkv.encode(key, value)
        }
    }

    /** Long 类型属性委托 */
    fun long(key: String, default: Long = 0L) = object : ReadWriteProperty<Any?, Long> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): Long {
            return mmkv.decodeLong(key, default)
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Long) {
            mmkv.encode(key, value)
        }
    }

    /** Float 类型属性委托 */
    fun float(key: String, default: Float = 0f) = object : ReadWriteProperty<Any?, Float> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): Float {
            return mmkv.decodeFloat(key, default)
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Float) {
            mmkv.encode(key, value)
        }
    }

    /** Double 类型属性委托 */
    fun double(key: String, default: Double = 0.0) = object : ReadWriteProperty<Any?, Double> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): Double {
            return mmkv.decodeDouble(key, default)
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Double) {
            mmkv.encode(key, value)
        }
    }

    /** Boolean 类型属性委托 */
    fun boolean(key: String, default: Boolean = false) = object : ReadWriteProperty<Any?, Boolean> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): Boolean {
            return mmkv.decodeBool(key, default)
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) {
            mmkv.encode(key, value)
        }
    }

    /** ByteArray 类型属性委托 */
    fun bytes(key: String, default: ByteArray = byteArrayOf()) = object : ReadWriteProperty<Any?, ByteArray> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): ByteArray {
            return mmkv.decodeBytes(key, default) ?: default
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: ByteArray) {
            mmkv.encode(key, value)
        }
    }

    /**
     * Set\<String\> 类型属性委托。
     *
     * 返回的是不可变集合，修改时需创建新集合并重新赋值。
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
     * 返回可空类型，赋值 null 时自动删除对应键。
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

    /** Parcelable 类型属性委托（reified 简化版） */
    inline fun <reified T : Parcelable> parcelable(
        key: String,
        default: T? = null
    ) = parcelable(key, T::class.java, default)
}
