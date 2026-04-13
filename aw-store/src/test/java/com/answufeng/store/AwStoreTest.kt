package com.answufeng.store

import org.junit.Assert.*
import org.junit.Test

class AwStoreTest {

    @Test
    fun `ensureInitialized message contains guidance`() {
        val message = "AwStore 尚未初始化，请先在 Application.onCreate() 中调用 AwStore.init(context)"
        assertTrue(message.contains("Application.onCreate()"))
        assertTrue(message.contains("AwStore.init"))
    }
}
