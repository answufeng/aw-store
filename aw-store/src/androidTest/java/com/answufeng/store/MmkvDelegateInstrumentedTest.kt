package com.answufeng.store

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MmkvDelegateInstrumentedTest {

    private lateinit var store: MmkvDelegate

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        if (!AwStore.isInitialized) {
            AwStore.init(context)
        }
        store = object : MmkvDelegate(mmapId = "test_store_${System.nanoTime()}") {}
    }

    @After
    fun tearDown() {
        store.clear()
    }

    @Test
    fun stringDelegateReadWrite() {
        val delegate = object : MmkvDelegate(mmapId = "test_str_${System.nanoTime()}") {
            var name by string("name", "default")
        }
        assertEquals("default", delegate.name)
        delegate.name = "hello"
        assertEquals("hello", delegate.name)
        delegate.clear()
    }

    @Test
    fun stringDelegateKeyAutoInference() {
        val delegate = object : MmkvDelegate(mmapId = "test_str_auto_${System.nanoTime()}") {
            var name by string("default")
        }
        assertEquals("default", delegate.name)
        delegate.name = "hello"
        assertEquals("hello", delegate.name)
        assertTrue(delegate.contains("name"))
        delegate.clear()
    }

    @Test
    fun stringDelegateNoArgs() {
        val delegate = object : MmkvDelegate(mmapId = "test_str_noargs_${System.nanoTime()}") {
            var token by string()
        }
        assertEquals("", delegate.token)
        delegate.token = "abc"
        assertEquals("abc", delegate.token)
        assertTrue(delegate.contains("token"))
        delegate.clear()
    }

    @Test
    fun nullableStringDelegateReadWriteNull() {
        val delegate = object : MmkvDelegate(mmapId = "test_nullable_${System.nanoTime()}") {
            var nickname by nullableString("nickname")
        }
        assertNull(delegate.nickname)
        delegate.nickname = "bob"
        assertEquals("bob", delegate.nickname)
        delegate.nickname = null
        assertNull(delegate.nickname)
        delegate.clear()
    }

    @Test
    fun intDelegateReadWrite() {
        val delegate = object : MmkvDelegate(mmapId = "test_int_${System.nanoTime()}") {
            var count by int("count", 0)
        }
        assertEquals(0, delegate.count)
        delegate.count = 42
        assertEquals(42, delegate.count)
        delegate.clear()
    }

    @Test
    fun nullableIntDelegate() {
        val delegate = object : MmkvDelegate(mmapId = "test_nullable_int_${System.nanoTime()}") {
            var age by nullableInt()
        }
        assertNull(delegate.age)
        delegate.age = 25
        assertEquals(25, delegate.age)
        delegate.age = null
        assertNull(delegate.age)
        assertFalse(delegate.contains("age"))
        delegate.clear()
    }

    @Test
    fun nullableLongDelegate() {
        val delegate = object : MmkvDelegate(mmapId = "test_nullable_long_${System.nanoTime()}") {
            var timestamp by nullableLong()
        }
        assertNull(delegate.timestamp)
        delegate.timestamp = 123456789L
        assertEquals(123456789L, delegate.timestamp)
        delegate.timestamp = null
        assertNull(delegate.timestamp)
        delegate.clear()
    }

    @Test
    fun nullableFloatDelegate() {
        val delegate = object : MmkvDelegate(mmapId = "test_nullable_float_${System.nanoTime()}") {
            var score by nullableFloat()
        }
        assertNull(delegate.score)
        delegate.score = 95.5f
        assertEquals(95.5f, delegate.score!!, 0.001f)
        delegate.score = null
        assertNull(delegate.score)
        delegate.clear()
    }

    @Test
    fun nullableDoubleDelegate() {
        val delegate = object : MmkvDelegate(mmapId = "test_nullable_double_${System.nanoTime()}") {
            var ratio by nullableDouble()
        }
        assertNull(delegate.ratio)
        delegate.ratio = 3.14159
        assertEquals(3.14159, delegate.ratio!!, 0.001)
        delegate.ratio = null
        assertNull(delegate.ratio)
        delegate.clear()
    }

    @Test
    fun nullableBooleanDelegate() {
        val delegate = object : MmkvDelegate(mmapId = "test_nullable_bool_${System.nanoTime()}") {
            var enabled by nullableBoolean()
        }
        assertNull(delegate.enabled)
        delegate.enabled = true
        assertTrue(delegate.enabled!!)
        delegate.enabled = null
        assertNull(delegate.enabled)
        delegate.clear()
    }

    @Test
    fun nullableBytesDelegate() {
        val delegate = object : MmkvDelegate(mmapId = "test_nullable_bytes_${System.nanoTime()}") {
            var data by nullableBytes()
        }
        assertNull(delegate.data)
        delegate.data = byteArrayOf(1, 2, 3)
        assertArrayEquals(byteArrayOf(1, 2, 3), delegate.data)
        delegate.data = null
        assertNull(delegate.data)
        delegate.clear()
    }

    @Test
    fun longDelegateReadWrite() {
        val delegate = object : MmkvDelegate(mmapId = "test_long_${System.nanoTime()}") {
            var timestamp by long("timestamp", 0L)
        }
        assertEquals(0L, delegate.timestamp)
        delegate.timestamp = 123456789L
        assertEquals(123456789L, delegate.timestamp)
        delegate.clear()
    }

    @Test
    fun floatDelegateReadWrite() {
        val delegate = object : MmkvDelegate(mmapId = "test_float_${System.nanoTime()}") {
            var score by float("score", 0f)
        }
        assertEquals(0f, delegate.score, 0.001f)
        delegate.score = 95.5f
        assertEquals(95.5f, delegate.score, 0.001f)
        delegate.clear()
    }

    @Test
    fun doubleDelegateReadWrite() {
        val delegate = object : MmkvDelegate(mmapId = "test_double_${System.nanoTime()}") {
            var ratio by double("ratio", 0.0)
        }
        assertEquals(0.0, delegate.ratio, 0.001)
        delegate.ratio = 3.14159
        assertEquals(3.14159, delegate.ratio, 0.001)
        delegate.clear()
    }

    @Test
    fun booleanDelegateReadWrite() {
        val delegate = object : MmkvDelegate(mmapId = "test_bool_${System.nanoTime()}") {
            var enabled by boolean("enabled", false)
        }
        assertFalse(delegate.enabled)
        delegate.enabled = true
        assertTrue(delegate.enabled)
        delegate.clear()
    }

    @Test
    fun bytesDelegateReadWrite() {
        val delegate = object : MmkvDelegate(mmapId = "test_bytes_${System.nanoTime()}") {
            var data by bytes("data")
        }
        assertArrayEquals(byteArrayOf(), delegate.data)
        delegate.data = byteArrayOf(1, 2, 3)
        assertArrayEquals(byteArrayOf(1, 2, 3), delegate.data)
        delegate.clear()
    }

    @Test
    fun stringSetDelegateReadWrite() {
        val delegate = object : MmkvDelegate(mmapId = "test_set_${System.nanoTime()}") {
            var tags by stringSet("tags")
        }
        assertEquals(emptySet<String>(), delegate.tags)
        delegate.tags = setOf("kotlin", "android")
        assertEquals(setOf("kotlin", "android"), delegate.tags)
        delegate.clear()
    }

    @Test
    fun clearRemovesAllKeys() {
        val delegate = object : MmkvDelegate(mmapId = "test_clear_${System.nanoTime()}") {
            var name by string("name", "")
            var age by int("age", 0)
        }
        delegate.name = "test"
        delegate.age = 25
        delegate.clear()
        assertEquals("", delegate.name)
        assertEquals(0, delegate.age)
    }

    @Test
    fun removeDeletesSpecificKey() {
        val delegate = object : MmkvDelegate(mmapId = "test_remove_${System.nanoTime()}") {
            var name by string("name", "")
            var age by int("age", 0)
        }
        delegate.name = "test"
        delegate.age = 25
        delegate.remove("name")
        assertEquals("", delegate.name)
        assertEquals(25, delegate.age)
        delegate.clear()
    }

    @Test
    fun containsChecksKeyExistence() {
        val delegate = object : MmkvDelegate(mmapId = "test_contains_${System.nanoTime()}") {
            var name by string("name", "")
        }
        assertFalse(delegate.contains("name"))
        delegate.name = "test"
        assertTrue(delegate.contains("name"))
        delegate.clear()
    }

    @Test
    fun allKeysReturnsStoredKeys() {
        val delegate = object : MmkvDelegate(mmapId = "test_allkeys_${System.nanoTime()}") {
            var name by string("name", "")
            var age by int("age", 0)
        }
        delegate.name = "test"
        delegate.age = 25
        val keys = delegate.allKeys()
        assertTrue(keys.contains("name"))
        assertTrue(keys.contains("age"))
        delegate.clear()
    }

    @Test
    fun multiInstanceIsolation() {
        val id = System.nanoTime()
        val store1 = object : MmkvDelegate(mmapId = "iso_1_$id") {
            var value by string("key", "")
        }
        val store2 = object : MmkvDelegate(mmapId = "iso_2_$id") {
            var value by string("key", "")
        }
        store1.value = "one"
        store2.value = "two"
        assertEquals("one", store1.value)
        assertEquals("two", store2.value)
        store1.clear()
        store2.clear()
    }

    @Test
    fun encryptedStoreReadWrite() {
        val store = object : MmkvDelegate(cryptKey = "test_key_${System.nanoTime()}") {
            var secret by string("secret", "")
        }
        store.secret = "password123"
        assertEquals("password123", store.secret)
        store.clear()
    }

    @Test
    fun encryptedStoreWithCryptKey() {
        val key = CryptKey.fromSecureRandom()
        val store = object : MmkvDelegate(secureCryptKey = key) {
            var secret by string("secret", "")
        }
        store.secret = "password123"
        assertEquals("password123", store.secret)
        store.clear()
    }

    @Test
    fun multipleEncryptedStoresWithDifferentKeysAreIsolated() {
        val id = System.nanoTime()
        val store1 = object : MmkvDelegate(cryptKey = "key1_$id") {
            var data by string("data", "")
        }
        val store2 = object : MmkvDelegate(cryptKey = "key2_$id") {
            var data by string("data", "")
        }
        store1.data = "from_store1"
        store2.data = "from_store2"
        assertEquals("from_store1", store1.data)
        assertEquals("from_store2", store2.data)
        store1.clear()
        store2.clear()
    }

    @Test
    fun multiProcessStoreReadWrite() {
        val store = object : MmkvDelegate(mmapId = "test_multi_${System.nanoTime()}", multiProcess = true) {
            var counter by int("counter", 0)
        }
        assertEquals(0, store.counter)
        store.counter = 42
        assertEquals(42, store.counter)
        store.clear()
    }

    @Test
    fun contentChangeRegisterAndUnregister() {
        val store = object : MmkvDelegate(mmapId = "test_listener_${System.nanoTime()}") {
            var data by string("data", "")
        }
        var receivedMmapId: String? = null
        val listener: (String) -> Unit = { receivedMmapId = it }
        store.registerContentChange(listener)
        store.unregisterContentChange(listener)
        store.clear()
    }
}
