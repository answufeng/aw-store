package com.answufeng.store

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SpMigrationInstrumentedTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        if (!AwStore.isInitialized) {
            AwStore.init(context)
        }
    }

    @Test
    fun migrateEmptySPReturnsEmptyResult() {
        val result = SpMigration.migrate(context, "empty_prefs_${System.nanoTime()}")
        assertEquals(0, result.totalKeys)
        assertEquals(0, result.successCount)
        assertTrue(result.isSuccess)
    }

    @Test
    fun migrateStringValue() {
        val name = "migrate_str_${System.nanoTime()}"
        val sp = context.getSharedPreferences(name, Context.MODE_PRIVATE)
        sp.edit().putString("name", "hello").commit()

        val result = SpMigration.migrate(context, name, mmapId = "migrate_str_target_${System.nanoTime()}")
        assertEquals(1, result.totalKeys)
        assertEquals(1, result.successCount)
        assertEquals(0, result.failedCount)
        assertTrue(result.isSuccess)
    }

    @Test
    fun migrateMixedTypes() {
        val name = "migrate_mixed_${System.nanoTime()}"
        val sp = context.getSharedPreferences(name, Context.MODE_PRIVATE)
        sp.edit()
            .putString("name", "test")
            .putInt("count", 10)
            .putLong("ts", 100L)
            .putFloat("score", 1.0f)
            .putBoolean("flag", true)
            .putStringSet("tags", setOf("a", "b"))
            .commit()

        val result = SpMigration.migrate(context, name, mmapId = "migrate_mixed_target_${System.nanoTime()}")
        assertEquals(6, result.totalKeys)
        assertEquals(6, result.successCount)
        assertTrue(result.isSuccess)
    }

    @Test
    fun migrateDeletesSPAfterSuccess() {
        val name = "migrate_delete_${System.nanoTime()}"
        val sp = context.getSharedPreferences(name, Context.MODE_PRIVATE)
        sp.edit().putString("key", "value").commit()

        SpMigration.migrate(context, name, mmapId = "migrate_delete_target_${System.nanoTime()}", deleteAfterMigration = true)

        assertTrue(sp.all.isEmpty())
    }

    @Test
    fun migrateKeepsSPWhenDeleteAfterMigrationIsFalse() {
        val name = "migrate_keep_${System.nanoTime()}"
        val sp = context.getSharedPreferences(name, Context.MODE_PRIVATE)
        sp.edit().putString("key", "value").commit()

        SpMigration.migrate(context, name, mmapId = "migrate_keep_target_${System.nanoTime()}", deleteAfterMigration = false)

        assertFalse(sp.all.isEmpty())
    }

    @Test
    fun migrateNullValueIsSkipped() {
        val name = "migrate_null_${System.nanoTime()}"
        val sp = context.getSharedPreferences(name, Context.MODE_PRIVATE)
        sp.edit().putString("valid_key", "value").commit()
        sp.edit().putString("null_key", null as String?).commit()

        val result = SpMigration.migrate(context, name, mmapId = "migrate_null_target_${System.nanoTime()}")
        assertTrue(result.skippedKeys.contains("null_key"))
    }
}
