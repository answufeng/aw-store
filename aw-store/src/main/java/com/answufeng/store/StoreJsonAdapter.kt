package com.answufeng.store

import kotlin.reflect.KClass

/**
 * JSON 序列化适配器接口。
 *
 * 用于 [MmkvDelegate.json] 委托的序列化/反序列化。
 * 用户需实现此接口并注册到 [AwStoreJsonAdapter]，以支持 Gson、Moshi、Kotlin Serialization 等库。
 *
 * ```kotlin
 * // Gson 实现
 * class GsonAdapter : StoreJsonAdapter {
 *     private val gson = Gson()
 *     override fun <T : Any> toJson(value: T, clazz: KClass<T>): String = gson.toJson(value)
 *     override fun <T : Any> fromJson(json: String, clazz: KClass<T>): T = gson.fromJson(json, clazz.java)
 * }
 *
 * // 注册
 * AwStoreJsonAdapter.setAdapter(GsonAdapter())
 * ```
 */
interface StoreJsonAdapter {
    /** 将 [value] 序列化为 JSON 字符串 */
    fun <T : Any> toJson(value: T, clazz: KClass<T>): String
    /** 将 JSON [json] 反序列化为 [clazz] 类型的对象 */
    fun <T : Any> fromJson(json: String, clazz: KClass<T>): T
}

/**
 * 全局 JSON 适配器管理器。
 *
 * 在使用 [MmkvDelegate.json] 委托前，必须先通过 [setAdapter] 注册一个 JSON 适配器实现。
 * 库本身不强制依赖任何 JSON 库，用户可自由选择。
 *
 * ```kotlin
 * AwStoreJsonAdapter.setAdapter(GsonAdapter())
 * ```
 */
object AwStoreJsonAdapter : StoreJsonAdapter {

    private var impl: StoreJsonAdapter? = null

    /**
     * 设置 JSON 适配器实现。
     *
     * 建议在 [android.app.Application.onCreate] 中调用。
     */
    fun setAdapter(adapter: StoreJsonAdapter) {
        impl = adapter
    }

    override fun <T : Any> toJson(value: T, clazz: KClass<T>): String {
        return impl?.toJson(value, clazz)
            ?: throw IllegalStateException(
                "请先调用 AwStoreJsonAdapter.setAdapter() 设置 JSON 适配器"
            )
    }

    override fun <T : Any> fromJson(json: String, clazz: KClass<T>): T {
        return impl?.fromJson(json, clazz)
            ?: throw IllegalStateException(
                "请先调用 AwStoreJsonAdapter.setAdapter() 设置 JSON 适配器"
            )
    }
}
