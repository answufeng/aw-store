package com.answufeng.store

import android.os.Parcelable
import com.tencent.mmkv.MMKV
import com.tencent.mmkv.MMKVContentChangeNotification
import java.math.BigDecimal
import java.math.BigInteger
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
 * > **注意**：若既未通过 [AwStoreInitializer] 完成自动初始化，也未调用 [AwStore.init]，访问存储会抛出 [IllegalStateException]。
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

    constructor(config: StoreConfig) : this(
        mmapId = config.mmapId,
        cryptKey = config.cryptKey,
        secureCryptKey = config.secureCryptKey,
        multiProcess = config.multiProcess
    )

    private val effectiveCryptKey: String?
        get() = secureCryptKey?.value ?: cryptKey

    @PublishedApi
    internal val mmkv: MMKV by lazy {
        AwStore.ensureInitialized()
        SpMigration.resolveMmkv(mmapId, effectiveCryptKey, multiProcess)
    }

    /**
     * 获取底层 MMKV 实例，用于访问 MMKV 的高级功能（如 `trim`、`close` 等）。
     *
     * 通常不需要直接使用此属性，库已封装了常用操作。
     */
    val mmkvInstance: MMKV get() = mmkv

    private val onKeyChangedListeners = CopyOnWriteArrayList<(String) -> Unit>()

    /**
     * 保证 [getOrPutString] 等同系列「先查后写」操作在同一代理实例上的原子性，避免并发下多次执行 default 回调。
     * 粒度：每个 [MmkvDelegate] 实例一把锁；不同 store 互不阻塞。
     */
    private val getOrPutLock = Any()

    /**
     * 注册单进程内的键值变更回调。
     *
     * 当通过属性委托或命令式 API 写入/删除键时触发，**[clear] 时会对清空前存在的每个 key 各触发一次**（键已删除），
     * 适用于同进程内的数据变化监听。跨进程数据变化请使用 [registerContentChange]。
     *
     * ```kotlin
     * UserStore.onKeyChanged { key ->
     *     Log.d("Store", "Key changed: $key")
     * }
     * ```
     *
     * @param listener 回调函数，参数为变更的键名
     */
    fun registerOnKeyChanged(listener: (key: String) -> Unit) {
        onKeyChangedListeners.add(listener)
    }

    /** @suppress 使用 [registerOnKeyChanged] 替代 */
    @Deprecated("Use registerOnKeyChanged instead", ReplaceWith("registerOnKeyChanged(listener)"))
    fun onKeyChanged(listener: (key: String) -> Unit) = registerOnKeyChanged(listener)

    /**
     * 取消单进程内的键值变更回调。
     *
     * @param listener 要取消的回调函数
     */
    fun unregisterOnKeyChanged(listener: (key: String) -> Unit) {
        onKeyChangedListeners.remove(listener)
    }

    /** @suppress 使用 [unregisterOnKeyChanged] 替代 */
    @Deprecated("Use unregisterOnKeyChanged instead", ReplaceWith("unregisterOnKeyChanged(listener)"))
    fun removeOnKeyChanged(listener: (key: String) -> Unit) = unregisterOnKeyChanged(listener)

    /**
     * 取消所有单进程内的键值变更回调。
     */
    fun clearOnKeyChangedListeners() {
        onKeyChangedListeners.clear()
    }

    @PublishedApi
    internal fun notifyKeyChanged(key: String) {
        onKeyChangedListeners.forEach { it(key) }
    }

    /**
     * 清空当前 MMKV 实例中的所有键值对。
     *
     * 此操作会立即生效，所有存储的数据将被删除且无法恢复。清空前会对当时存在的每个 key
     * 各调用一次 [registerOnKeyChanged] 的回调，便于与 [remove] 等行为一致地刷新 UI。
     */
    fun clear() {
        val keys = mmkv.allKeys()
        mmkv.clearAll()
        keys?.forEach { notifyKeyChanged(it) }
    }

    /** 删除指定 [key] 对应的键值对 */
    fun remove(key: String) {
        mmkv.removeValueForKey(key)
        notifyKeyChanged(key)
    }

    /** 批量删除指定 [keys] 对应的键值对 */
    fun remove(vararg keys: String) {
        mmkv.removeValuesForKeys(keys)
        keys.forEach { notifyKeyChanged(it) }
    }

    /**
     * 检查指定 [key] 是否存在，支持 `key in store` 操作符语法。
     *
     * @return `true` 如果 key 存在，否则返回 `false`
     */
    operator fun contains(key: String): Boolean = mmkv.containsKey(key)

    /**
     * 获取当前存储实例中的所有键名。
     *
     * @return 键名数组，如果存储为空则返回空数组
     */
    fun allKeys(): Array<String> = mmkv.allKeys() ?: emptyArray()

    /**
     * 获取当前存储实例对应的文件总大小。
     *
     * @return 文件大小，单位为字节
     */
    fun totalSize(): Long = mmkv.totalSize()

    /**
     * 同步写入，等待数据完全写入磁盘后再返回。
     *
     * 适用于关键数据（如用户凭证、支付信息等），确保数据不会因应用崩溃而丢失。
     */
    fun sync() {
        mmkv.sync()
    }

    /**
     * 异步写入，立即返回，数据在后台线程写入磁盘。
     *
     * 适用于非关键数据（如 UI 状态、临时配置等），写入性能更好，但极端情况下可能丢失数据。
     */
    fun async() {
        mmkv.async()
    }

    /**
     * 批量写入事务。
     *
     * 在 [block] 中进行多次写入操作，MMKV 会自动合并为一次写操作，提高写入效率。
     *
     * ```kotlin
     * store.edit {
     *     encode("key1", "value1")
     *     encode("key2", 42)
     * }
     * ```
     *
     * 若需使用 [MMKV] 的其它写入（如带过期时间的 [MMKV.encode] 重载），可通过 [MmkvEditScope.mmkv] 调用并在写入后
     * 调用 [MmkvEditScope.markKeyChanged]，否则键变更监听器收不到这些键的回调。
     */
    fun edit(block: MmkvEditScope.() -> Unit) {
        val changedKeys = mutableSetOf<String>()
        MmkvEditScope(mmkv, changedKeys).block()
        changedKeys.forEach { notifyKeyChanged(it) }
    }

    /** 读取字符串值。 */
    fun getString(key: String, default: String = ""): String = mmkv.decodeString(key, default) ?: default

    /** 写入字符串值，写入后触发键变更通知。 */
    fun putString(key: String, value: String) {
        mmkv.encode(key, value)
        notifyKeyChanged(key)
    }

    /** 读取整数值。 */
    fun getInt(key: String, default: Int = 0): Int = mmkv.decodeInt(key, default)

    /** 写入整数值，写入后触发键变更通知。 */
    fun putInt(key: String, value: Int) {
        mmkv.encode(key, value)
        notifyKeyChanged(key)
    }

    /** 读取长整数值。 */
    fun getLong(key: String, default: Long = 0L): Long = mmkv.decodeLong(key, default)

    /** 写入长整数值，写入后触发键变更通知。 */
    fun putLong(key: String, value: Long) {
        mmkv.encode(key, value)
        notifyKeyChanged(key)
    }

    /** 读取浮点数值。 */
    fun getFloat(key: String, default: Float = 0f): Float = mmkv.decodeFloat(key, default)

    /** 写入浮点数值，写入后触发键变更通知。 */
    fun putFloat(key: String, value: Float) {
        mmkv.encode(key, value)
        notifyKeyChanged(key)
    }

    /** 读取双精度浮点数值。 */
    fun getDouble(key: String, default: Double = 0.0): Double = mmkv.decodeDouble(key, default)

    /** 写入双精度浮点数值，写入后触发键变更通知。 */
    fun putDouble(key: String, value: Double) {
        mmkv.encode(key, value)
        notifyKeyChanged(key)
    }

    /** 读取布尔值。 */
    fun getBoolean(key: String, default: Boolean = false): Boolean = mmkv.decodeBool(key, default)

    /** 写入布尔值，写入后触发键变更通知。 */
    fun putBoolean(key: String, value: Boolean) {
        mmkv.encode(key, value)
        notifyKeyChanged(key)
    }

    /** 读取字节数组。 */
    fun getBytes(key: String, default: ByteArray = EMPTY_BYTE_ARRAY): ByteArray = mmkv.decodeBytes(key, default) ?: default

    /** 写入字节数组，写入后触发键变更通知。 */
    fun putBytes(key: String, value: ByteArray) {
        mmkv.encode(key, value)
        notifyKeyChanged(key)
    }

    /** 读取字符串集合。 */
    fun getStringSet(key: String, default: Set<String> = emptySet()): Set<String> = mmkv.decodeStringSet(key, default) ?: default

    /** 写入字符串集合，写入后触发键变更通知。 */
    fun putStringSet(key: String, value: Set<String>) {
        mmkv.encode(key, value)
        notifyKeyChanged(key)
    }

    /** 读取 Parcelable 对象。 */
    fun <T : Parcelable> getParcelable(key: String, clazz: Class<T>, default: T? = null): T? {
        return mmkv.decodeParcelable(key, clazz, default)
    }

    /** 读取 Parcelable 对象（reified 泛型版本）。 */
    inline fun <reified T : Parcelable> getParcelable(key: String, default: T? = null): T? {
        return getParcelable(key, T::class.java, default)
    }

    /** 写入 Parcelable 对象，写入后触发键变更通知。 */
    fun <T : Parcelable> putParcelable(key: String, value: T) {
        mmkv.encode(key, value)
        notifyKeyChanged(key)
    }

    /**
     * 读取 Serializable 对象。
     *
     * 注意：Java 序列化性能较差且存在兼容性风险，推荐使用 [parcelable] 或 [json] 替代。
     */
    inline fun <reified T : java.io.Serializable> getSerializable(key: String, default: T? = null): T? {
        val bytes = mmkv.decodeBytes(key) ?: return default
        return try {
            java.io.ByteArrayInputStream(bytes).use { bis ->
                java.io.ObjectInputStream(bis).use { ois ->
                    ois.readObject() as? T
                }
            }
        } catch (e: Exception) {
            AwStoreLogger.w("getSerializable failed for key=$key", e)
            default
        }
    }

    /** 写入 Serializable 对象，写入后触发键变更通知。推荐使用 [putParcelable] 或 [putJson] 替代。 */
    fun <T : java.io.Serializable> putSerializable(key: String, value: T) {
        val bos = java.io.ByteArrayOutputStream()
        java.io.ObjectOutputStream(bos).use { oos ->
            oos.writeObject(value)
        }
        mmkv.encode(key, bos.toByteArray())
        notifyKeyChanged(key)
    }

    /**
     * 读取字符串值，若 key 不存在则写入 [defaultValue] 并返回。
     *
     * 写入后会触发键变更通知。同一 [MmkvDelegate] 实例上并发调用时，[defaultValue] 至多执行一次（见 [getOrPutLock]）。
     */
    fun getOrPutString(key: String, defaultValue: () -> String): String {
        synchronized(getOrPutLock) {
            if (!mmkv.containsKey(key)) {
                val value = defaultValue()
                mmkv.encode(key, value)
                notifyKeyChanged(key)
                return value
            }
            return mmkv.decodeString(key, "") ?: ""
        }
    }

    /**
     * 读取整数值，若 key 不存在则写入 [defaultValue] 并返回。写入后触发键变更通知。
     * 同一实例上 [defaultValue] 在并发下至多执行一次。
     */
    fun getOrPutInt(key: String, defaultValue: () -> Int): Int {
        synchronized(getOrPutLock) {
            if (!mmkv.containsKey(key)) {
                val value = defaultValue()
                mmkv.encode(key, value)
                notifyKeyChanged(key)
                return value
            }
            return mmkv.decodeInt(key, 0)
        }
    }

    /**
     * 读取长整数值，若 key 不存在则写入 [defaultValue] 并返回。写入后触发键变更通知。
     * 同一实例上 [defaultValue] 在并发下至多执行一次。
     */
    fun getOrPutLong(key: String, defaultValue: () -> Long): Long {
        synchronized(getOrPutLock) {
            if (!mmkv.containsKey(key)) {
                val value = defaultValue()
                mmkv.encode(key, value)
                notifyKeyChanged(key)
                return value
            }
            return mmkv.decodeLong(key, 0L)
        }
    }

    /**
     * 读取布尔值，若 key 不存在则写入 [defaultValue] 并返回。写入后触发键变更通知。
     * 同一实例上 [defaultValue] 在并发下至多执行一次。
     */
    fun getOrPutBoolean(key: String, defaultValue: () -> Boolean): Boolean {
        synchronized(getOrPutLock) {
            if (!mmkv.containsKey(key)) {
                val value = defaultValue()
                mmkv.encode(key, value)
                notifyKeyChanged(key)
                return value
            }
            return mmkv.decodeBool(key, false)
        }
    }

    /**
     * 读取浮点数值，若 key 不存在则写入 [defaultValue] 并返回。写入后触发键变更通知。
     * 同一实例上 [defaultValue] 在并发下至多执行一次。
     */
    fun getOrPutFloat(key: String, defaultValue: () -> Float): Float {
        synchronized(getOrPutLock) {
            if (!mmkv.containsKey(key)) {
                val value = defaultValue()
                mmkv.encode(key, value)
                notifyKeyChanged(key)
                return value
            }
            return mmkv.decodeFloat(key, 0f)
        }
    }

    /**
     * 读取双精度浮点数值，若 key 不存在则写入 [defaultValue] 并返回。写入后触发键变更通知。
     * 同一实例上 [defaultValue] 在并发下至多执行一次。
     */
    fun getOrPutDouble(key: String, defaultValue: () -> Double): Double {
        synchronized(getOrPutLock) {
            if (!mmkv.containsKey(key)) {
                val value = defaultValue()
                mmkv.encode(key, value)
                notifyKeyChanged(key)
                return value
            }
            return mmkv.decodeDouble(key, 0.0)
        }
    }

    /**
     * 读取字符串集合，若 key 不存在则写入 [defaultValue] 并返回。写入后触发键变更通知。
     * 同一实例上 [defaultValue] 在并发下至多执行一次。
     */
    fun getOrPutStringSet(key: String, defaultValue: () -> Set<String>): Set<String> {
        synchronized(getOrPutLock) {
            if (!mmkv.containsKey(key)) {
                val value = defaultValue()
                mmkv.encode(key, value)
                notifyKeyChanged(key)
                return value
            }
            return mmkv.decodeStringSet(key, emptySet()) ?: emptySet()
        }
    }

    /**
     * 读取字节数组，若 key 不存在则写入 [defaultValue] 并返回。写入后触发键变更通知。
     * 同一实例上 [defaultValue] 在并发下至多执行一次。
     */
    fun getOrPutBytes(key: String, defaultValue: () -> ByteArray): ByteArray {
        synchronized(getOrPutLock) {
            if (!mmkv.containsKey(key)) {
                val value = defaultValue()
                mmkv.encode(key, value)
                notifyKeyChanged(key)
                return value
            }
            return mmkv.decodeBytes(key, EMPTY_BYTE_ARRAY) ?: EMPTY_BYTE_ARRAY
        }
    }

    /**
     * 读取 JSON 对象；若 key 不存在则序列化 [defaultValue] 的结果并写入后返回。
     *
     * 若 key 存在但反序列化失败（损坏或非 JSON），则视为需重建：会用 [defaultValue] 覆盖写入并返回新值。
     * 使用前须已注册 [AwStoreJsonAdapter]。
     *
     * 同一实例上「缺失时的 default」在并发下至多执行一次（与 [getOrPutString] 相同锁）。
     */
    inline fun <reified T : Any> getOrPutJson(key: String, noinline defaultValue: () -> T): T =
        getOrPutJsonImpl(key, T::class, defaultValue)

    @PublishedApi
    internal fun <T : Any> getOrPutJsonImpl(key: String, clazz: KClass<T>, defaultValue: () -> T): T {
        synchronized(getOrPutLock) {
            if (!mmkv.containsKey(key)) {
                val value = defaultValue()
                putJsonInternal(key, value, clazz)
                return value
            }
            val existing = getJsonInternal(key, clazz)
            if (existing != null) return existing
            val value = defaultValue()
            putJsonInternal(key, value, clazz)
            return value
        }
    }

    /**
     * 导出当前存储的所有键值对为 Map。
     *
     * 支持的类型：String、Int、Long、Float、Double、Boolean、ByteArray、Set\<String\>。
     * 其他类型（如 Parcelable、Serializable、JSON）以原始字节或字符串形式导出。
     *
     * **说明**：MMKV 在存储侧按类型做区分，但公开 API 不提供按 key 查询值类型的能力。本方法按固定顺序
     * 试解码，对绝大多数「每个 key 只使用一种 [encode] 类型」的数据是正确的；`Boolean` 与 `Int`（例如
     * `0` / `false`）等理论上有歧义，且若某 key 恰好存的是 [Int.MAX_VALUE] 会落入后续 [Long] / 浮点分支；
     * Parcelable/自定义字节可能被判为 [ByteArray]。请勿用于对类型精度有严格要求的场景。
     */
    fun exportToMap(): Map<String, Any?> {
        val result = mutableMapOf<String, Any?>()
        val keys = mmkv.allKeys() ?: return emptyMap()
        for (key in keys) {
            result[key] = decodeValueForExport(mmkv, key)
        }
        return result
    }

    /**
     * 从 Map 导入键值对到当前存储。
     *
     * 支持的类型：String、Int、Long、Float、Double、Boolean、ByteArray、Set\<String\>、[Byte]、[Short]、
     * [java.math.BigDecimal]、[java.math.BigInteger]（[BigInteger] 须落在 [Long] 精确范围内，否则跳过并记 WARN；[BigDecimal] 以 [Double] 存储可能有精度损失）；`null` 会删除该 key。已存在的键会被覆盖。
     *
     * 不支持的 [Map] 项会被跳过，并在 [AwStoreLogger.enabled] 为 `true` 时打 WARN 日志，便于排查问题。
     *
     * @param notifyKeyChanges 为 `true`（默认）时每成功写入/删除立即触发 [registerOnKeyChanged]；为 `false` 时在整次导入结束后按 key 去重各触发一次，适合大批量导入以减少 UI 刷新次数。
     * @return 成功写入或删除（`null` 项）的键数量
     */
    fun importFromMap(map: Map<String, Any?>, notifyKeyChanges: Boolean = true): Int {
        val deferredNotify = if (notifyKeyChanges) null else mutableSetOf<String>()
        fun onKeyImported(key: String) {
            if (notifyKeyChanges) {
                notifyKeyChanged(key)
            } else {
                deferredNotify!!.add(key)
            }
        }
        var count = 0
        var skipped = 0
        for ((key, value) in map) {
            when (value) {
                is String -> {
                    mmkv.encode(key, value)
                    onKeyImported(key)
                    count++
                }
                is Int -> {
                    mmkv.encode(key, value)
                    onKeyImported(key)
                    count++
                }
                is Long -> {
                    mmkv.encode(key, value)
                    onKeyImported(key)
                    count++
                }
                is Float -> {
                    mmkv.encode(key, value)
                    onKeyImported(key)
                    count++
                }
                is Double -> {
                    mmkv.encode(key, value)
                    onKeyImported(key)
                    count++
                }
                is Boolean -> {
                    mmkv.encode(key, value)
                    onKeyImported(key)
                    count++
                }
                is ByteArray -> {
                    mmkv.encode(key, value)
                    onKeyImported(key)
                    count++
                }
                is Set<*> -> {
                    if (value.isEmpty()) {
                        mmkv.encode(key, emptySet())
                        onKeyImported(key)
                        count++
                    } else {
                        val asStrings = value.mapNotNull { it as? String }
                        if (asStrings.size == value.size) {
                            mmkv.encode(key, asStrings.toSet())
                            onKeyImported(key)
                            count++
                        } else {
                            skipped++
                            AwStoreLogger.w("importFromMap: skip key=\"$key\", Set must contain only String (got ${value.map { it?.javaClass?.simpleName }})")
                        }
                    }
                }
                is Byte -> {
                    mmkv.encode(key, value.toInt())
                    onKeyImported(key)
                    count++
                }
                is Short -> {
                    mmkv.encode(key, value.toInt())
                    onKeyImported(key)
                    count++
                }
                is BigDecimal -> {
                    mmkv.encode(key, value.toDouble())
                    onKeyImported(key)
                    count++
                }
                is BigInteger -> {
                    try {
                        val longVal = value.longValueExact()
                        mmkv.encode(key, longVal)
                        onKeyImported(key)
                        count++
                    } catch (_: ArithmeticException) {
                        skipped++
                        AwStoreLogger.w(
                            "importFromMap: skip key=\"$key\", BigInteger out of Long range: $value"
                        )
                    }
                }
                null -> {
                    mmkv.removeValueForKey(key)
                    onKeyImported(key)
                    count++
                }
                else -> {
                    skipped++
                    AwStoreLogger.w("importFromMap: skip key=\"$key\", unsupported value type: ${value.javaClass.name}")
                }
            }
        }
        if (!notifyKeyChanges) {
            deferredNotify!!.forEach { notifyKeyChanged(it) }
        }
        if (skipped > 0) {
            AwStoreLogger.w("importFromMap: skipped $skipped of ${map.size} entries (see per-key messages above)")
        }
        return count
    }

    @PublishedApi
    internal fun <T : Any> getJsonInternal(key: String, clazz: KClass<T>): T? {
        val str = mmkv.decodeString(key, null) ?: return null
        return AwStoreJsonAdapter.fromJson(str, clazz)
    }

    @PublishedApi
    internal fun <T : Any> putJsonInternal(key: String, value: T, clazz: KClass<T>) {
        mmkv.encode(key, AwStoreJsonAdapter.toJson(value, clazz))
        notifyKeyChanged(key)
    }

    /**
     * 读取 JSON 对象。
     *
     * 使用前必须先通过 [AwStoreJsonAdapter.setAdapter] 注册 JSON 适配器。
     *
     * @param key MMKV 键名
     * @return 反序列化后的对象，若 key 不存在或解析失败则返回 `null`
     * @throws IllegalStateException 未设置 JSON 适配器时抛出
     */
    inline fun <reified T : Any> getJson(key: String): T? = getJsonInternal(key, T::class)

    /**
     * 写入 JSON 对象。
     *
     * 使用前必须先通过 [AwStoreJsonAdapter.setAdapter] 注册 JSON 适配器。
     * 对象将被序列化为 JSON 字符串存储。
     *
     * @param key MMKV 键名
     * @param value 要存储的对象
     * @throws IllegalStateException 未设置 JSON 适配器时抛出
     */
    inline fun <reified T : Any> putJson(key: String, value: T) = putJsonInternal(key, value, T::class)

    private val contentChangeListeners = ConcurrentHashMap<String, CopyOnWriteArrayList<(String) -> Unit>>()

    private fun pruneEmptyListenerBuckets(map: ConcurrentHashMap<String, CopyOnWriteArrayList<(String) -> Unit>>) {
        val iter = map.entries.iterator()
        while (iter.hasNext()) {
            val e = iter.next()
            if (e.value.isEmpty()) iter.remove()
        }
    }

    companion object {
        /** 空字节数组单例，供 [getBytes] / [bytes] 默认值等复用，避免每个 [MmkvDelegate] 实例各持一份。 */
        @PublishedApi
        internal val EMPTY_BYTE_ARRAY: ByteArray = byteArrayOf()

        @Volatile
        private var globalNotificationRegistered = false
        private val globalLock = Any()
        private val allListeners = ConcurrentHashMap<String, CopyOnWriteArrayList<(String) -> Unit>>()

        private val globalNotification = object : MMKVContentChangeNotification {
            override fun onContentChangedByOuterProcess(mmapID: String) {
                allListeners[mmapID]?.forEach { it(mmapID) }
            }
        }

        internal fun registerGlobalNotification(targetMmapId: String, listener: (String) -> Unit) {
            allListeners.getOrPut(targetMmapId) { CopyOnWriteArrayList() }.add(listener)
            if (!globalNotificationRegistered) {
                synchronized(globalLock) {
                    if (!globalNotificationRegistered) {
                        MMKV.registerContentChangeNotify(globalNotification)
                        globalNotificationRegistered = true
                    }
                }
            }
        }

        internal fun unregisterGlobalNotification(listener: (String) -> Unit) {
            allListeners.values.forEach { it.remove(listener) }
            pruneAllListenersEmptyBuckets()
            maybeUnregisterGlobalNotification()
        }

        internal fun unregisterAllGlobalNotification(listeners: ConcurrentHashMap<String, CopyOnWriteArrayList<(String) -> Unit>>) {
            listeners.keys.forEach { key ->
                allListeners[key]?.let { globalList ->
                    listeners[key]?.forEach { globalList.remove(it) }
                }
            }
            listeners.clear()
            pruneAllListenersEmptyBuckets()
            maybeUnregisterGlobalNotification()
        }

        private fun pruneAllListenersEmptyBuckets() {
            val iter = allListeners.entries.iterator()
            while (iter.hasNext()) {
                val e = iter.next()
                if (e.value.isEmpty()) iter.remove()
            }
        }

        private fun maybeUnregisterGlobalNotification() {
            if (globalNotificationRegistered && allListeners.values.all { it.isEmpty() }) {
                synchronized(globalLock) {
                    if (globalNotificationRegistered && allListeners.values.all { it.isEmpty() }) {
                        MMKV.unregisterContentChangeNotify()
                        globalNotificationRegistered = false
                    }
                }
            }
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
        val id = targetMmapId ?: (mmapId ?: "DefaultMMKV")
        contentChangeListeners.getOrPut(id) { CopyOnWriteArrayList() }.add(listener)
        registerGlobalNotification(id, listener)
    }

    /** 取消指定的跨进程数据变化监听 */
    fun unregisterContentChange(listener: (String) -> Unit) {
        contentChangeListeners.values.forEach { it.remove(listener) }
        pruneEmptyListenerBuckets(contentChangeListeners)
        unregisterGlobalNotification(listener)
    }

    /** 取消当前实例的所有跨进程数据变化监听 */
    fun unregisterAllContentChange() {
        unregisterAllGlobalNotification(contentChangeListeners)
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
            notifyKeyChanged(k)
        }
    }

    /**
     * Nullable String 类型属性委托，赋值 null 时删除对应键。
     *
     * @param key MMKV 键名，为 null 时自动使用属性名
     * @param default key 不存在时的默认值，默认 null
     */
    fun nullableString(key: String? = null, default: String? = null) = object : ReadWriteProperty<Any?, String?> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): String? {
            val k = key ?: property.name
            return if (mmkv.containsKey(k)) mmkv.decodeString(k, default) else default
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: String?) {
            val k = key ?: property.name
            if (value != null) {
                mmkv.encode(k, value)
            } else {
                mmkv.removeValueForKey(k)
            }
            notifyKeyChanged(k)
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
            notifyKeyChanged(k)
        }
    }

    /**
     * Nullable Int 类型属性委托。
     *
     * key 不存在时返回 null，赋值 null 时删除对应键。
     * 可区分"key 不存在"和"值为 0"的场景。
     *
     * @param key MMKV 键名，为 null 时自动使用属性名
     * @param default key 不存在时的默认值，默认 null
     */
    fun nullableInt(key: String? = null, default: Int? = null) = object : ReadWriteProperty<Any?, Int?> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): Int? {
            val k = key ?: property.name
            return if (mmkv.containsKey(k)) mmkv.decodeInt(k) else default
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Int?) {
            val k = key ?: property.name
            if (value != null) mmkv.encode(k, value) else mmkv.removeValueForKey(k)
            notifyKeyChanged(k)
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
            notifyKeyChanged(k)
        }
    }

    /**
     * Nullable Long 类型属性委托。
     *
     * key 不存在时返回 null，赋值 null 时删除对应键。
     *
     * @param key MMKV 键名，为 null 时自动使用属性名
     * @param default key 不存在时的默认值，默认 null
     */
    fun nullableLong(key: String? = null, default: Long? = null) = object : ReadWriteProperty<Any?, Long?> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): Long? {
            val k = key ?: property.name
            return if (mmkv.containsKey(k)) mmkv.decodeLong(k) else default
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Long?) {
            val k = key ?: property.name
            if (value != null) mmkv.encode(k, value) else mmkv.removeValueForKey(k)
            notifyKeyChanged(k)
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
            notifyKeyChanged(k)
        }
    }

    /**
     * Nullable Float 类型属性委托。
     *
     * key 不存在时返回 null，赋值 null 时删除对应键。
     *
     * @param key MMKV 键名，为 null 时自动使用属性名
     * @param default key 不存在时的默认值，默认 null
     */
    fun nullableFloat(key: String? = null, default: Float? = null) = object : ReadWriteProperty<Any?, Float?> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): Float? {
            val k = key ?: property.name
            return if (mmkv.containsKey(k)) mmkv.decodeFloat(k) else default
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Float?) {
            val k = key ?: property.name
            if (value != null) mmkv.encode(k, value) else mmkv.removeValueForKey(k)
            notifyKeyChanged(k)
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
            notifyKeyChanged(k)
        }
    }

    /**
     * Nullable Double 类型属性委托。
     *
     * key 不存在时返回 null，赋值 null 时删除对应键。
     *
     * @param key MMKV 键名，为 null 时自动使用属性名
     * @param default key 不存在时的默认值，默认 null
     */
    fun nullableDouble(key: String? = null, default: Double? = null) = object : ReadWriteProperty<Any?, Double?> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): Double? {
            val k = key ?: property.name
            return if (mmkv.containsKey(k)) mmkv.decodeDouble(k) else default
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Double?) {
            val k = key ?: property.name
            if (value != null) mmkv.encode(k, value) else mmkv.removeValueForKey(k)
            notifyKeyChanged(k)
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
            notifyKeyChanged(k)
        }
    }

    /**
     * Nullable Boolean 类型属性委托。
     *
     * key 不存在时返回 null，赋值 null 时删除对应键。
     * 可区分"key 不存在"和"值为 false"的场景。
     *
     * @param key MMKV 键名，为 null 时自动使用属性名
     * @param default key 不存在时的默认值，默认 null
     */
    fun nullableBoolean(key: String? = null, default: Boolean? = null) = object : ReadWriteProperty<Any?, Boolean?> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): Boolean? {
            val k = key ?: property.name
            return if (mmkv.containsKey(k)) mmkv.decodeBool(k) else default
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean?) {
            val k = key ?: property.name
            if (value != null) mmkv.encode(k, value) else mmkv.removeValueForKey(k)
            notifyKeyChanged(k)
        }
    }

    /**
     * ByteArray 类型属性委托。
     *
     * @param key MMKV 键名，为 null 时自动使用属性名
     * @param default 默认值
     */
    fun bytes(key: String? = null, default: ByteArray = EMPTY_BYTE_ARRAY) = object : ReadWriteProperty<Any?, ByteArray> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): ByteArray {
            val k = key ?: property.name
            return mmkv.decodeBytes(k, default) ?: default
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: ByteArray) {
            val k = key ?: property.name
            mmkv.encode(k, value)
            notifyKeyChanged(k)
        }
    }

    /**
     * Nullable ByteArray 类型属性委托。
     *
     * key 不存在时返回 null，赋值 null 时删除对应键。
     *
     * @param key MMKV 键名，为 null 时自动使用属性名
     * @param default key 不存在时的默认值，默认 null
     */
    fun nullableBytes(key: String? = null, default: ByteArray? = null) = object : ReadWriteProperty<Any?, ByteArray?> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): ByteArray? {
            val k = key ?: property.name
            return if (mmkv.containsKey(k)) mmkv.decodeBytes(k) else default
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: ByteArray?) {
            val k = key ?: property.name
            if (value != null) mmkv.encode(k, value) else mmkv.removeValueForKey(k)
            notifyKeyChanged(k)
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
            notifyKeyChanged(k)
        }
    }

    /**
     * Nullable Set\<String\> 类型属性委托。
     *
     * key 不存在时返回 null，赋值 null 时删除对应键。
     * 可区分"key 不存在"和"值为空集合"的场景。
     *
     * @param key MMKV 键名，为 null 时自动使用属性名
     * @param default key 不存在时的默认值，默认 null
     */
    fun nullableStringSet(key: String? = null, default: Set<String>? = null) = object : ReadWriteProperty<Any?, Set<String>?> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): Set<String>? {
            val k = key ?: property.name
            return if (mmkv.containsKey(k)) mmkv.decodeStringSet(k) else default
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Set<String>?) {
            val k = key ?: property.name
            if (value != null) mmkv.encode(k, value) else mmkv.removeValueForKey(k)
            notifyKeyChanged(k)
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
    @PublishedApi
    internal fun <T : Parcelable> parcelableDelegate(
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
            notifyKeyChanged(k)
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
    ) = parcelableDelegate(key, T::class.java, default)

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
    @Deprecated(
        message = "Java 序列化性能较差且存在兼容性风险，推荐使用 parcelable() 或 json() 替代",
        level = DeprecationLevel.WARNING
    )
    inline fun <reified T : java.io.Serializable> serializable(
        key: String? = null,
        default: T? = null
    ) = object : ReadWriteProperty<Any?, T?> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): T? {
            val k = key ?: property.name
            val bytes = mmkv.decodeBytes(k) ?: return default
            return try {
                java.io.ByteArrayInputStream(bytes).use { bis ->
                    java.io.ObjectInputStream(bis).use { ois ->
                        ois.readObject() as? T
                    }
                }
            } catch (e: Exception) {
                AwStoreLogger.w("serializable read failed for key=$k", e)
                default
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
            notifyKeyChanged(k)
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
            notifyKeyChanged(k)
        }
    }
}

/**
 * [MmkvDelegate.edit] 的接收者：封装常用 [MMKV.encode] 与删除操作，并跟踪变更的 key 以触发单进程回调。
 * 对「带过期时间」等重载，请通过 [mmkv] 执行并在写入后 [markKeyChanged]。
 */
class MmkvEditScope @PublishedApi internal constructor(
    /** 底层 MMKV，用于 [MMKV.encode] 带过期时间等重载；写入非常规 [encode] 后请 [markKeyChanged]。 */
    val mmkv: MMKV,
    private val changedKeys: MutableSet<String>
) {

    fun markKeyChanged(key: String) {
        changedKeys.add(key)
    }

    fun encode(key: String, value: Boolean): Boolean {
        val r = mmkv.encode(key, value)
        changedKeys.add(key)
        return r
    }

    fun encode(key: String, value: Int): Boolean {
        val r = mmkv.encode(key, value)
        changedKeys.add(key)
        return r
    }

    fun encode(key: String, value: Long): Boolean {
        val r = mmkv.encode(key, value)
        changedKeys.add(key)
        return r
    }

    fun encode(key: String, value: Float): Boolean {
        val r = mmkv.encode(key, value)
        changedKeys.add(key)
        return r
    }

    fun encode(key: String, value: Double): Boolean {
        val r = mmkv.encode(key, value)
        changedKeys.add(key)
        return r
    }

    fun encode(key: String, value: String?): Boolean {
        val r = mmkv.encode(key, value)
        changedKeys.add(key)
        return r
    }

    fun encode(key: String, value: ByteArray?): Boolean {
        val r = mmkv.encode(key, value)
        changedKeys.add(key)
        return r
    }

    fun encode(key: String, value: Set<String>?): Boolean {
        val r = mmkv.encode(key, value)
        changedKeys.add(key)
        return r
    }

    fun encode(key: String, value: Parcelable?): Boolean {
        val r = mmkv.encode(key, value)
        changedKeys.add(key)
        return r
    }

    fun removeValueForKey(key: String) {
        mmkv.removeValueForKey(key)
        changedKeys.add(key)
    }

    fun removeValuesForKeys(keys: Array<out String>) {
        mmkv.removeValuesForKeys(keys)
        changedKeys.addAll(keys)
    }

    fun clearAll() {
        val snapshot = mmkv.allKeys()
        mmkv.clearAll()
        if (snapshot != null) {
            changedKeys.addAll(listOf(*snapshot))
        }
    }
}

/**
 * 尽力按「每个 key 一种 MMKV 类型」还原值；与 [MmkvDelegate.exportToMap] 的说明一致。
 * 顺序：StringSet → String → ByteArray → Int / Long / Float / Double（以哨兵排除「不存在的默认值」；存储 [Int] 的 [Int.MAX_VALUE] 会落入后续分支）。
 * 与 `0` 与 `false`、`1` 与 `true` 等歧义在 MMKV 中无法无 API 地消除，导出的数可能为 [Int] 或 [Double] 等，仅适合调试与迁移，勿依赖精确类型。
 */
private fun decodeValueForExport(m: MMKV, key: String): Any? {
    if (!m.containsKey(key)) return null
    m.decodeStringSet(key, null as Set<String>?)?.let { return it }
    m.decodeString(key, null)?.let { return it }
    m.decodeBytes(key)?.let { return it }
    m.decodeInt(key, Int.MAX_VALUE).takeIf { it != Int.MAX_VALUE }?.let { return it }
    m.decodeLong(key, Long.MAX_VALUE).takeIf { it != Long.MAX_VALUE }?.let { return it }
    m.decodeFloat(key, Float.NaN).takeIf { !it.isNaN() }?.let { return it }
    m.decodeDouble(key, Double.NaN).takeIf { !it.isNaN() }?.let { return it }
    return null
}
