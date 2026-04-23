package com.answufeng.store

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.math.BigInteger
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.KClass
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MmkvDelegateRobolectricTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        if (!AwStore.isInitialized) {
            AwStore.init(context)
        }
    }

    @After
    fun tearDown() {
        AwStoreJsonAdapter.clearAdapter()
    }

    @Test
    fun getOrPutInt_defaultInvokedOnce_underConcurrency() {
        val store = object : MmkvDelegate(mmapId = "t_getorput_${UUID.randomUUID()}") {}
        store.remove("race_key")
        val calls = AtomicInteger(0)
        val threads =
            List(12) {
                Thread {
                    store.getOrPutInt("race_key") {
                        calls.incrementAndGet()
                        42
                    }
                }
            }
        threads.forEach { it.start() }
        threads.forEach { it.join() }
        assertEquals(1, calls.get())
        assertEquals(42, store.getInt("race_key"))
    }

    @Test
    fun importFromMap_skipsBigInteger_outOfLongRange() {
        val store = object : MmkvDelegate(mmapId = "t_bi_${UUID.randomUUID()}") {}
        val huge = BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE)
        val written = store.importFromMap(mapOf("overflow_bi" to huge))
        assertEquals(0, written)
        assertFalse(store.contains("overflow_bi"))
    }

    @Test
    fun importFromMap_importsBigInteger_inLongRange() {
        val store = object : MmkvDelegate(mmapId = "t_bi_ok_${UUID.randomUUID()}") {}
        val written = store.importFromMap(mapOf("ok_bi" to BigInteger.valueOf(99L)))
        assertEquals(1, written)
        assertEquals(99L, store.getLong("ok_bi"))
    }

    @Test
    fun exportImport_roundTrip_primitives() {
        val store = object : MmkvDelegate(mmapId = "t_export_${UUID.randomUUID()}") {}
        store.putString("s", "v")
        store.putInt("i", 7)
        val map = store.exportToMap()
        store.clear()
        assertFalse(store.contains("s"))
        store.importFromMap(map)
        assertEquals("v", store.getString("s"))
        assertEquals(7, store.getInt("i"))
    }

    @Test
    fun importFromMap_deferredNotify_writesSameValues() {
        val store = object : MmkvDelegate(mmapId = "t_defimp_${UUID.randomUUID()}") {}
        val map = mapOf("ia" to 1, "sb" to "x")
        val n = store.importFromMap(map, notifyKeyChanges = false)
        assertEquals(2, n)
        assertEquals(1, store.getInt("ia"))
        assertEquals("x", store.getString("sb"))
    }

    @Test
    fun getOrPutJson_invokesDefaultOnce() {
        AwStoreJsonAdapter.setAdapter(TestDtoJsonAdapter)
        val store = object : MmkvDelegate(mmapId = "t_goj_${UUID.randomUUID()}") {}
        val calls = AtomicInteger(0)
        val first = store.getOrPutJson("jk") {
            calls.incrementAndGet()
            TestDto(1)
        }
        assertEquals(1, first.v)
        assertEquals(1, calls.get())
        val second = store.getOrPutJson("jk") {
            calls.incrementAndGet()
            TestDto(99)
        }
        assertEquals(1, second.v)
        assertEquals(1, calls.get())
    }

    @Test
    fun registerOnKeyChanged_unregisterStopsCallbacks() {
        val store = object : MmkvDelegate(mmapId = "t_lis_${UUID.randomUUID()}") {}
        val seen = mutableListOf<String>()
        val listener: (String) -> Unit = { seen.add(it) }
        store.registerOnKeyChanged(listener)
        store.putString("a", "1")
        store.unregisterOnKeyChanged(listener)
        store.putString("b", "2")
        assertTrue(seen.contains("a"))
        assertFalse(seen.contains("b"))
    }

    @Test
    fun spMigration_withMmapId_readsBack() {
        val spName = "sp_${UUID.randomUUID()}"
        val mmapId = "mm_${UUID.randomUUID()}"
        context.getSharedPreferences(spName, Context.MODE_PRIVATE).edit().putString("sk", "sv").commit()
        SpMigration.migrate(context, spName, mmapId = mmapId, deleteAfterMigration = true)
        val reader = object : MmkvDelegate(mmapId = mmapId) {}
        assertEquals("sv", reader.getString("sk"))
    }

    private data class TestDto(val v: Int)

    private object TestDtoJsonAdapter : StoreJsonAdapter {
        override fun <T : Any> toJson(value: T, clazz: KClass<T>): String {
            require(clazz == TestDto::class)
            val t = value as TestDto
            return JSONObject().put("v", t.v).toString()
        }

        override fun <T : Any> fromJson(json: String, clazz: KClass<T>): T {
            require(clazz == TestDto::class)
            val o = JSONObject(json)
            @Suppress("UNCHECKED_CAST")
            return TestDto(o.getInt("v")) as T
        }
    }
}
