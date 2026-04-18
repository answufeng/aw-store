package com.answufeng.store

import android.os.Parcelable
import com.tencent.mmkv.MMKV
import com.tencent.mmkv.MMKVContentChangeNotification
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

/**
 * 基于 MMKV 的高性能键值存储属性委托。
 *
 * 继承此类并使用类型委托工厂方法声明属性，即可实现类型安全的持久化存储。
 * Key 参数可省略，自动使用属性名作为 MMKV 键名。
 *
 * ```kotlin
 * object UserStore : MmkvDelegate() {
 *     var token by string()              // key="token", default=""
 *     var userId by long()               // key="userId", default=0L
 *     var isLoggedIn by boolean()        // key="isLoggedIn", default=false
 *     var score by float()               // key="score", default=0f
 *     var tags by stringSet()            // key="tags", default=emptySet()
 *     var nickname by nullableString()   // key="nickname", default=null
 *     var age by nullableInt()           // key="age", default=null
 * }
 * ```
 *
 * 显式指定 key（向后兼容）：
 * ```kotlin
 * var userId by long("user_id", 0L)
 * ```
 *
 * 加密存储：
 * ```kotlin
 * object SecureStore : MmkvDelegate(secureCryptKey = CryptKey.fromSecureRandom()) {
 *     var password by string()
 * }
 * ```
 *
 * 多进程模式：
 * ```kotlin
 * object SharedStore : MmkvDelegate(mmapId = "shared", multiProcess = true) {
 *     var counter by int()
 * }
 * ```
 *
 * > **注意**：未调用 [AwStore.init] 就访问属性会抛出 [IllegalStateException]。
 *
 * @param mmapId MMKV 实例 ID，不同 ID 对应不同的存储文件，默认使用 MMKV 默认实例
 * @param cryptKey 加密密钥字符串，传入后该存储文件将使用 AES-CFB 加密，默认不加密。
 *                 不同 cryptKey 会自动使用不同的 MMKV 实例，避免数据覆盖。
 *                 推荐使用 [CryptKey] 替代直接传字符串。
 * @param secureCryptKey 安全加密密钥包装，优先级高于 [cryptKey]。
 *                       推荐使用 [CryptKey.fromSecureRandom] 生成安全随机密钥。
 * @param multiProcess 是否启用多进程模式，默认 false。
 *                     启用后使用 MMKV.MULTI_PROCESS_MODE，确保跨进程读写一致性。
 */
open class MmkvDelegate(
    private val mmapId: String? = null,
    private val cryptKey: String? = null,
    private val secureCryptKey: CryptKey? = null,
    private val multiProcess: Boolean = false
) {

    private val effectiveCryptKey: String?
        get() = secureCryptKey?.value ?: cryptKey

    @PublishedApi
    internal val mmkv: MMKV by lazy {
        AwStore.ensureInitialized()
        SpMigration.resolveMmkv(mmapId, effectiveCryptKey, multiProcess)
    }

    private val effectiveMmapId: String
        get() = mmapId ?: "DefaultMMKV"

    /** 清空当前 MMKV 实例中的所有键值对 */
    fun clear() {
        mmkv.clearAll()
    }

    /** 删除指定 [key] 对应的键值对 */
    fun remove(key: String) {
        mmkv.removeValueForKey(key)
    }

    /** 批量删除指定 [keys] 对应的键值对 */
    fun remove(vararg keys: String) {
        mmkv.removeValuesForKeys(keys)
    }

    /** 检查指定 [key] 是否存在，支持 `key in store` 语法 */
    operator fun contains(key: String): Boolean = mmkv.containsKey(key)

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

    fun getString(key: String, default: String = ""): String = mmkv.decodeString(key, default) ?: default

    fun putString(key: String, value: String) {
        mmkv.encode(key, value)
    }

    fun getInt(key: String, default: Int = 0): Int = mmkv.decodeInt(key, default)

    fun putInt(key: String, value: Int) {
        mmkv.encode(key, value)
    }

    fun getLong(key: String, default: Long = 0L): Long = mmkv.decodeLong(key, default)

    fun putLong(key: String, value: Long) {
        mmkv.encode(key, value)
    }

    fun getFloat(key: String, default: Float = 0f): Float = mmkv.decodeFloat(key, default)

    fun putFloat(key: String, value: Float) {
        mmkv.encode(key, value)
    }

    fun getDouble(key: String, default: Double = 0.0): Double = mmkv.decodeDouble(key, default)

    fun putDouble(key: String, value: Double) {
        mmkv.encode(key, value)
    }

    fun getBoolean(key: String, default: Boolean = false): Boolean = mmkv.decodeBool(key, default)

    fun putBoolean(key: String, value: Boolean) {
        mmkv.encode(key, value)
    }

    fun getBytes(key: String, default: ByteArray = byteArrayOf()): ByteArray = mmkv.decodeBytes(key, default) ?: default

    fun putBytes(key: String, value: ByteArray) {
        mmkv.encode(key, value)
    }

    fun getStringSet(key: String, default: Set<String> = emptySet()): Set<String> = mmkv.decodeStringSet(key, default) ?: default

    fun putStringSet(key: String, value: Set<String>) {
        mmkv.encode(key, value)
    }

    fun <T : Any> getJson(key: String, clazz: KClass<T>): T? {
        val str = mmkv.decodeString(key, null) ?: return null
        return AwStoreJsonAdapter.fromJson(str, clazz)
    }

    fun <T : Any> putJson(key: String, value: T, clazz: KClass<T> = value::class as KClass<T>) {
        mmkv.encode(key, AwStoreJsonAdapter.toJson(value, clazz))
    }

    inline fun <reified T : Any> getJson(key: String): T? = getJson(key, T::class)

    inline fun <reified T : Any> putJson(key: String, value: T) = putJson(key, value, T::class)

    private val contentChangeListeners = ConcurrentHashMap<String, CopyOnWriteArrayList<(String) -> Unit>>()

    @Volatile
    private var globalNotificationRegistered = false

    private val globalNotification = object : MMKVContentChangeNotification {
        override fun onContentChangedByOuterProcess(mmapID: String) {
            contentChangeListeners[mmapID]?.forEach { it(mmapID) }
        }
    }

    /**
     * 注册跨进程数据变化监听。
     *
     * 当其他进程修改了 MMKV 数据时，[listener] 会被回调，参数为被修改的 MMKV 实例 ID。
     * 当前进程的修改不会触发回调。支持按 mmapId 过滤，多个 listener 不会互相覆盖。
     *
     * @param targetMmapId 监听的目标 MMKV 实例 ID，默认为当前实例的 mmapId
     * @param listener 回调函数，参数为被修改的 MMKV 实例 ID
     */
    fun registerContentChange(
        targetMmapId: String? = null,
        listener: (mmapID: String) -> Unit
    ) {
        val id = targetMmapId ?: effectiveMmapId
        contentChangeListeners.getOrPut(id) { CopyOnWriteArrayList() }.add(listener)
        if (!globalNotificationRegistered) {
            synchronized(this) {
                if (!globalNotificationRegistered) {
                    MMKV.registerContentChangeNotify(globalNotification)
                    globalNotificationRegistered = true
                }
            }
        }
    }

    /** 取消指定的跨进程数据变化监听 */
    fun unregisterContentChange(listener: (String) -> Unit) {
        contentChangeListeners.values.forEach { it.remove(listener) }
    }

    /** 取消所有跨进程数据变化监听，并注销全局通知 */
    fun unregisterAllContentChange() {
        contentChangeListeners.clear()
        if (globalNotificationRegistered) {
            synchronized(this) {
                if (globalNotificationRegistered) {
                    MMKV.unregisterContentChangeNotify()
                    globalNotificationRegistered = false
                }
            }
        }
    }

    /**
     * String 类型属性委托。
     *
     * @param key MMKV 键名，为 null 时自动使用属性名
     * @param default 默认值
     */
    fun string(key: String? = null, default: String = "") = object : ReadWriteProperty<Any?, String> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): String {
            val k = key ?: property.name
            return mmkv.decodeString(k, default) ?: default
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
            val k = key ?: property.name
            mmkv.encode(k, value)
        }
    }

    /**
     * Nullable String 类型属性委托，赋值 null 时删除对应键。
     *
     * @param key MMKV 键名，为 null 时自动使用属性名
     */
    fun nullableString(key: String? = null) = object : ReadWriteProperty<Any?, String?> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): String? {
            val k = key ?: property.name
            return mmkv.decodeString(k, null)
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: String?) {
            val k = key ?: property.name
            if (value != null) {
                mmkv.encode(k, value)
            } else {
                mmkv.removeValueForKey(k)
            }
        }
    }

    /**
     * Int 类型属性委托。
     *
     * @param key MMKV 键名，为 null 时自动使用属性名
     * @param default 默认值
     */
    fun int(key: String? = null, default: Int = 0) = object : ReadWriteProperty<Any?, Int> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): Int {
            val k = key ?: property.name
            return mmkv.decodeInt(k, default)
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {
            val k = key ?: property.name
            mmkv.encode(k, value)
        }
    }

    /**
     * Nullable Int 类型属性委托。
     *
     * key 不存在时返回 null，赋值 null 时删除对应键。
     * 可区分"key 不存在"和"值为 0"的场景。
     *
     * @param key MMKV 键名，为 null 时自动使用属性名
     */
    fun nullableInt(key: String? = null) = object : ReadWriteProperty<Any?, Int?> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): Int? {
            val k = key ?: property.name
            return if (mmkv.containsKey(k)) mmkv.decodeInt(k) else null
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Int?) {
            val k = key ?: property.name
            if (value != null) mmkv.encode(k, value) else mmkv.removeValueForKey(k)
        }
    }

    /**
     * Long 类型属性委托。
     *
     * @param key MMKV 键名，为 null 时自动使用属性名
     * @param default 默认值
     */
    fun long(key: String? = null, default: Long = 0L) = object : ReadWriteProperty<Any?, Long> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): Long {
            val k = key ?: property.name
            return mmkv.decodeLong(k, default)
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Long) {
            val k = key ?: property.name
            mmkv.encode(k, value)
        }
    }

    /**
     * Nullable Long 类型属性委托。
     *
     * key 不存在时返回 null，赋值 null 时删除对应键。
     *
     * @param key MMKV 键名，为 null 时自动使用属性名
     */
    fun nullableLong(key: String? = null) = object : ReadWriteProperty<Any?, Long?> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): Long? {
            val k = key ?: property.name
            return if (mmkv.containsKey(k)) mmkv.decodeLong(k) else null
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Long?) {
            val k = key ?: property.name
            if (value != null) mmkv.encode(k, value) else mmkv.removeValueForKey(k)
        }
    }

    /**
     * Float 类型属性委托。
     *
     * @param key MMKV 键名，为 null 时自动使用属性名
     * @param default 默认值
     */
    fun float(key: String? = null, default: Float = 0f) = object : ReadWriteProperty<Any?, Float> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): Float {
            val k = key ?: property.name
            return mmkv.decodeFloat(k, default)
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Float) {
            val k = key ?: property.name
            mmkv.encode(k, value)
        }
    }

    /**
     * Nullable Float 类型属性委托。
     *
     * key 不存在时返回 null，赋值 null 时删除对应键。
     *
     * @param key MMKV 键名，为 null 时自动使用属性名
     */
    fun nullableFloat(key: String? = null) = object : ReadWriteProperty<Any?, Float?> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): Float? {
            val k = key ?: property.name
            return if (mmkv.containsKey(k)) mmkv.decodeFloat(k) else null
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Float?) {
            val k = key ?: property.name
            if (value != null) mmkv.encode(k, value) else mmkv.removeValueForKey(k)
        }
    }

    /**
     * Double 类型属性委托。
     *
     * @param key MMKV 键名，为 null 时自动使用属性名
     * @param default 默认值
     */
    fun double(key: String? = null, default: Double = 0.0) = object : ReadWriteProperty<Any?, Double> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): Double {
            val k = key ?: property.name
            return mmkv.decodeDouble(k, default)
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Double) {
            val k = key ?: property.name
            mmkv.encode(k, value)
        }
    }

    /**
     * Nullable Double 类型属性委托。
     *
     * key 不存在时返回 null，赋值 null 时删除对应键。
     *
     * @param key MMKV 键名，为 null 时自动使用属性名
     */
    fun nullableDouble(key: String? = null) = object : ReadWriteProperty<Any?, Double?> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): Double? {
            val k = key ?: property.name
            return if (mmkv.containsKey(k)) mmkv.decodeDouble(k) else null
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Double?) {
            val k = key ?: property.name
            if (value != null) mmkv.encode(k, value) else mmkv.removeValueForKey(k)
        }
    }

    /**
     * Boolean 类型属性委托。
     *
     * @param key MMKV 键名，为 null 时自动使用属性名
     * @param default 默认值
     */
    fun boolean(key: String? = null, default: Boolean = false) = object : ReadWriteProperty<Any?, Boolean> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): Boolean {
            val k = key ?: property.name
            return mmkv.decodeBool(k, default)
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) {
            val k = key ?: property.name
            mmkv.encode(k, value)
        }
    }

    /**
     * Nullable Boolean 类型属性委托。
     *
     * key 不存在时返回 null，赋值 null 时删除对应键。
     * 可区分"key 不存在"和"值为 false"的场景。
     *
     * @param key MMKV 键名，为 null 时自动使用属性名
     */
    fun nullableBoolean(key: String? = null) = object : ReadWriteProperty<Any?, Boolean?> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): Boolean? {
            val k = key ?: property.name
            return if (mmkv.containsKey(k)) mmkv.decodeBool(k) else null
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean?) {
            val k = key ?: property.name
            if (value != null) mmkv.encode(k, value) else mmkv.removeValueForKey(k)
        }
    }

    /**
     * ByteArray 类型属性委托。
     *
     * @param key MMKV 键名，为 null 时自动使用属性名
     * @param default 默认值
     */
    fun bytes(key: String? = null, default: ByteArray = byteArrayOf()) = object : ReadWriteProperty<Any?, ByteArray> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): ByteArray {
            val k = key ?: property.name
            return mmkv.decodeBytes(k, default) ?: default
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: ByteArray) {
            val k = key ?: property.name
            mmkv.encode(k, value)
        }
    }

    /**
     * Nullable ByteArray 类型属性委托。
     *
     * key 不存在时返回 null，赋值 null 时删除对应键。
     *
     * @param key MMKV 键名，为 null 时自动使用属性名
     */
    fun nullableBytes(key: String? = null) = object : ReadWriteProperty<Any?, ByteArray?> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): ByteArray? {
            val k = key ?: property.name
            return if (mmkv.containsKey(k)) mmkv.decodeBytes(k) else null
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: ByteArray?) {
            val k = key ?: property.name
            if (value != null) mmkv.encode(k, value) else mmkv.removeValueForKey(k)
        }
    }

    /**
     * Set\<String\> 类型属性委托。
     *
     * 返回的是不可变集合，修改时需创建新集合并重新赋值。
     *
     * @param key MMKV 键名，为 null 时自动使用属性名
     * @param default 默认值
     */
    fun stringSet(key: String? = null, default: Set<String> = emptySet()) = object : ReadWriteProperty<Any?, Set<String>> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): Set<String> {
            val k = key ?: property.name
            return mmkv.decodeStringSet(k, default) ?: default
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Set<String>) {
            val k = key ?: property.name
            mmkv.encode(k, value)
        }
    }

    /**
     * Nullable Set\<String\> 类型属性委托。
     *
     * key 不存在时返回 null，赋值 null 时删除对应键。
     * 可区分"key 不存在"和"值为空集合"的场景。
     *
     * @param key MMKV 键名，为 null 时自动使用属性名
     */
    fun nullableStringSet(key: String? = null) = object : ReadWriteProperty<Any?, Set<String>?> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): Set<String>? {
            val k = key ?: property.name
            return if (mmkv.containsKey(k)) mmkv.decodeStringSet(k) else null
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Set<String>?) {
            val k = key ?: property.name
            if (value != null) mmkv.encode(k, value) else mmkv.removeValueForKey(k)
        }
    }

    /**
     * Parcelable 类型属性委托。
     *
     * 返回可空类型，赋值 null 时自动删除对应键。
     *
     * @param key MMKV 键名，为 null 时自动使用属性名
     * @param clazz Parcelable 的 Class 对象
     * @param default 默认值
     */
    fun <T : Parcelable> parcelable(
        key: String? = null,
        clazz: Class<T>,
        default: T? = null
    ) = object : ReadWriteProperty<Any?, T?> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): T? {
            val k = key ?: property.name
            return mmkv.decodeParcelable(k, clazz, default)
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: T?) {
            val k = key ?: property.name
            if (value != null) {
                mmkv.encode(k, value)
            } else {
                mmkv.removeValueForKey(k)
            }
        }
    }

    /**
     * Parcelable 类型属性委托（reified 简化版）。
     *
     * ```kotlin
     * var profile by parcelable<UserProfile>()
     * ```
     *
     * @param key MMKV 键名，为 null 时自动使用属性名
     * @param default 默认值
     */
    inline fun <reified T : Parcelable> parcelable(
        key: String? = null,
        default: T? = null
    ) = parcelable(key, T::class.java, default)

    /**
     * Serializable 类型属性委托（reified 简化版）。
     *
     * 返回可空类型，赋值 null 时自动删除对应键。
     * 使用字节数组存储 Serializable 对象。
     *
     * ```kotlin
     * var config by serializable<AppConfig>()
     * ```
     *
     * @param key MMKV 键名，为 null 时自动使用属性名
     * @param default 默认值
     */
    inline fun <reified T : java.io.Serializable> serializable(
        key: String? = null,
        default: T? = null
    ) = object : ReadWriteProperty<Any?, T?> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): T? {
            val k = key ?: property.name
            val bytes = mmkv.decodeBytes(k) ?: return default
            return java.io.ByteArrayInputStream(bytes).use { bis ->
                java.io.ObjectInputStream(bis).use { ois ->
                    ois.readObject() as? T
                }
            }
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: T?) {
            val k = key ?: property.name
            if (value != null) {
                val bos = java.io.ByteArrayOutputStream()
                java.io.ObjectOutputStream(bos).use { oos ->
                    oos.writeObject(value)
                }
                mmkv.encode(k, bos.toByteArray())
            } else {
                mmkv.removeValueForKey(k)
            }
        }
    }

    /**
     * JSON 对象属性委托（reified 简化版）。
     *
     * 将对象序列化为 JSON 字符串存储，支持任意 data class。
     * 需先通过 [AwStoreJsonAdapter.setAdapter] 设置 JSON 适配器（Gson/Moshi/Kotlin Serialization）。
     *
     * ```kotlin
     * AwStoreJsonAdapter.setAdapter(GsonAdapter())
     *
     * object Store : MmkvDelegate() {
     *     var user by json<UserProfile>()
     * }
     * ```
     *
     * @param key MMKV 键名，为 null 时自动使用属性名
     * @param default 默认值
     * @throws IllegalStateException 未设置 JSON 适配器时抛出
     */
    inline fun <reified T : Any> json(
        key: String? = null,
        default: T? = null
    ) = jsonDelegate(key, T::class, default)

    @PublishedApi
    internal fun <T : Any> jsonDelegate(
        key: String?,
        clazz: KClass<T>,
        default: T?
    ) = object : ReadWriteProperty<Any?, T?> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): T? {
            val k = key ?: property.name
            val str = mmkv.decodeString(k, null) ?: return default
            return AwStoreJsonAdapter.fromJson(str, clazz)
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: T?) {
            val k = key ?: property.name
            if (value != null) {
                mmkv.encode(k, AwStoreJsonAdapter.toJson(value, clazz))
            } else {
                mmkv.removeValueForKey(k)
            }
        }
    }
}
