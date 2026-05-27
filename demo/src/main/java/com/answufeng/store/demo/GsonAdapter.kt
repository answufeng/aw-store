package com.answufeng.store.demo

import com.answufeng.store.StoreJsonAdapter
import com.google.gson.Gson
import kotlin.reflect.KClass

class GsonAdapter : StoreJsonAdapter {
    private val gson = Gson()
    override fun <T : Any> toJson(value: T, clazz: KClass<T>): String = gson.toJson(value)
    override fun <T : Any> fromJson(json: String, clazz: KClass<T>): T = gson.fromJson(json, clazz.java)
}
