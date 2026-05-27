package com.answufeng.store

import com.tencent.mmkv.MMKV
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * [MmkvDelegate] 属性委托的内部工厂，合并 nullable / non-nullable 的重复实现。
 */
internal object MmkvDelegateFactories {
    fun resolveKey(
        key: String?,
        property: KProperty<*>,
    ): String = key ?: property.name

    fun stringProperty(
        mmkv: MMKV,
        onChanged: (String) -> Unit,
        key: String?,
        default: String,
    ): ReadWriteProperty<Any?, String> =
        object : ReadWriteProperty<Any?, String> {
            override fun getValue(
                thisRef: Any?,
                property: KProperty<*>,
            ): String {
                val k = resolveKey(key, property)
                return mmkv.decodeString(k, default) ?: default
            }

            override fun setValue(
                thisRef: Any?,
                property: KProperty<*>,
                value: String,
            ) {
                val k = resolveKey(key, property)
                mmkv.encode(k, value)
                onChanged(k)
            }
        }

    fun nullableStringProperty(
        mmkv: MMKV,
        onChanged: (String) -> Unit,
        key: String?,
        default: String?,
    ): ReadWriteProperty<Any?, String?> =
        nullableProperty(
            mmkv,
            onChanged,
            key,
            default,
            read = { m, k -> m.decodeString(k, default) },
            write = { m, k, v -> m.encode(k, v) },
        )

    fun intProperty(
        mmkv: MMKV,
        onChanged: (String) -> Unit,
        key: String?,
        default: Int,
    ): ReadWriteProperty<Any?, Int> =
        primitiveProperty(
            mmkv,
            onChanged,
            key,
            default,
            read = { m, k, d -> m.decodeInt(k, d) },
            write = { m, k, v -> m.encode(k, v) },
        )

    fun nullableIntProperty(
        mmkv: MMKV,
        onChanged: (String) -> Unit,
        key: String?,
        default: Int?,
    ): ReadWriteProperty<Any?, Int?> =
        nullableProperty(
            mmkv,
            onChanged,
            key,
            default,
            read = { m, k -> m.decodeInt(k) },
            write = { m, k, v -> m.encode(k, v) },
        )

    fun longProperty(
        mmkv: MMKV,
        onChanged: (String) -> Unit,
        key: String?,
        default: Long,
    ): ReadWriteProperty<Any?, Long> =
        primitiveProperty(
            mmkv,
            onChanged,
            key,
            default,
            read = { m, k, d -> m.decodeLong(k, d) },
            write = { m, k, v -> m.encode(k, v) },
        )

    fun nullableLongProperty(
        mmkv: MMKV,
        onChanged: (String) -> Unit,
        key: String?,
        default: Long?,
    ): ReadWriteProperty<Any?, Long?> =
        nullableProperty(
            mmkv,
            onChanged,
            key,
            default,
            read = { m, k -> m.decodeLong(k) },
            write = { m, k, v -> m.encode(k, v) },
        )

    fun floatProperty(
        mmkv: MMKV,
        onChanged: (String) -> Unit,
        key: String?,
        default: Float,
    ): ReadWriteProperty<Any?, Float> =
        primitiveProperty(
            mmkv,
            onChanged,
            key,
            default,
            read = { m, k, d -> m.decodeFloat(k, d) },
            write = { m, k, v -> m.encode(k, v) },
        )

    fun nullableFloatProperty(
        mmkv: MMKV,
        onChanged: (String) -> Unit,
        key: String?,
        default: Float?,
    ): ReadWriteProperty<Any?, Float?> =
        nullableProperty(
            mmkv,
            onChanged,
            key,
            default,
            read = { m, k -> m.decodeFloat(k) },
            write = { m, k, v -> m.encode(k, v) },
        )

    fun doubleProperty(
        mmkv: MMKV,
        onChanged: (String) -> Unit,
        key: String?,
        default: Double,
    ): ReadWriteProperty<Any?, Double> =
        primitiveProperty(
            mmkv,
            onChanged,
            key,
            default,
            read = { m, k, d -> m.decodeDouble(k, d) },
            write = { m, k, v -> m.encode(k, v) },
        )

    fun nullableDoubleProperty(
        mmkv: MMKV,
        onChanged: (String) -> Unit,
        key: String?,
        default: Double?,
    ): ReadWriteProperty<Any?, Double?> =
        nullableProperty(
            mmkv,
            onChanged,
            key,
            default,
            read = { m, k -> m.decodeDouble(k) },
            write = { m, k, v -> m.encode(k, v) },
        )

    fun booleanProperty(
        mmkv: MMKV,
        onChanged: (String) -> Unit,
        key: String?,
        default: Boolean,
    ): ReadWriteProperty<Any?, Boolean> =
        primitiveProperty(
            mmkv,
            onChanged,
            key,
            default,
            read = { m, k, d -> m.decodeBool(k, d) },
            write = { m, k, v -> m.encode(k, v) },
        )

    fun nullableBooleanProperty(
        mmkv: MMKV,
        onChanged: (String) -> Unit,
        key: String?,
        default: Boolean?,
    ): ReadWriteProperty<Any?, Boolean?> =
        nullableProperty(
            mmkv,
            onChanged,
            key,
            default,
            read = { m, k -> m.decodeBool(k) },
            write = { m, k, v -> m.encode(k, v) },
        )

    fun bytesProperty(
        mmkv: MMKV,
        onChanged: (String) -> Unit,
        key: String?,
        default: ByteArray,
    ): ReadWriteProperty<Any?, ByteArray> =
        object : ReadWriteProperty<Any?, ByteArray> {
            override fun getValue(
                thisRef: Any?,
                property: KProperty<*>,
            ): ByteArray {
                val k = resolveKey(key, property)
                return mmkv.decodeBytes(k, default) ?: default
            }

            override fun setValue(
                thisRef: Any?,
                property: KProperty<*>,
                value: ByteArray,
            ) {
                val k = resolveKey(key, property)
                mmkv.encode(k, value)
                onChanged(k)
            }
        }

    fun nullableBytesProperty(
        mmkv: MMKV,
        onChanged: (String) -> Unit,
        key: String?,
        default: ByteArray?,
    ): ReadWriteProperty<Any?, ByteArray?> =
        nullableProperty(
            mmkv,
            onChanged,
            key,
            default,
            read = { m, k -> m.decodeBytes(k) },
            write = { m, k, v -> m.encode(k, v) },
        )

    fun stringSetProperty(
        mmkv: MMKV,
        onChanged: (String) -> Unit,
        key: String?,
        default: Set<String>,
    ): ReadWriteProperty<Any?, Set<String>> =
        object : ReadWriteProperty<Any?, Set<String>> {
            override fun getValue(
                thisRef: Any?,
                property: KProperty<*>,
            ): Set<String> {
                val k = resolveKey(key, property)
                return mmkv.decodeStringSet(k, default) ?: default
            }

            override fun setValue(
                thisRef: Any?,
                property: KProperty<*>,
                value: Set<String>,
            ) {
                val k = resolveKey(key, property)
                mmkv.encode(k, value)
                onChanged(k)
            }
        }

    fun nullableStringSetProperty(
        mmkv: MMKV,
        onChanged: (String) -> Unit,
        key: String?,
        default: Set<String>?,
    ): ReadWriteProperty<Any?, Set<String>?> =
        nullableProperty(
            mmkv,
            onChanged,
            key,
            default,
            read = { m, k -> m.decodeStringSet(k) },
            write = { m, k, v -> m.encode(k, v) },
        )

    private inline fun <T> primitiveProperty(
        mmkv: MMKV,
        crossinline onChanged: (String) -> Unit,
        key: String?,
        default: T,
        crossinline read: (MMKV, String, T) -> T,
        crossinline write: (MMKV, String, T) -> Unit,
    ): ReadWriteProperty<Any?, T> =
        object : ReadWriteProperty<Any?, T> {
            override fun getValue(
                thisRef: Any?,
                property: KProperty<*>,
            ): T {
                val k = resolveKey(key, property)
                return read(mmkv, k, default)
            }

            override fun setValue(
                thisRef: Any?,
                property: KProperty<*>,
                value: T,
            ) {
                val k = resolveKey(key, property)
                write(mmkv, k, value)
                onChanged(k)
            }
        }

    private inline fun <T> nullableProperty(
        mmkv: MMKV,
        crossinline onChanged: (String) -> Unit,
        key: String?,
        default: T?,
        crossinline read: (MMKV, String) -> T,
        crossinline write: (MMKV, String, T) -> Unit,
    ): ReadWriteProperty<Any?, T?> =
        object : ReadWriteProperty<Any?, T?> {
            override fun getValue(
                thisRef: Any?,
                property: KProperty<*>,
            ): T? {
                val k = resolveKey(key, property)
                return if (mmkv.containsKey(k)) read(mmkv, k) else default
            }

            override fun setValue(
                thisRef: Any?,
                property: KProperty<*>,
                value: T?,
            ) {
                val k = resolveKey(key, property)
                if (value != null) {
                    write(mmkv, k, value)
                } else {
                    mmkv.removeValueForKey(k)
                }
                onChanged(k)
            }
        }
}
