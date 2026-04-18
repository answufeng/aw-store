package com.answufeng.store

import org.junit.Assert.*
import org.junit.Test

class MigrationResultTest {

    @Test
    fun `isSuccess when no failures and no skipped keys`() {
        val result = MigrationResult(5, 5, 0, emptyList())
        assertTrue(result.isSuccess)
    }

    @Test
    fun `isNotSuccess when has failures`() {
        val result = MigrationResult(5, 4, 1, emptyList())
        assertFalse(result.isSuccess)
    }

    @Test
    fun `isNotSuccess when has skipped keys`() {
        val result = MigrationResult(5, 4, 0, listOf("null_key"))
        assertFalse(result.isSuccess)
    }

    @Test
    fun `toString format`() {
        val result = MigrationResult(5, 4, 1, listOf("key1"))
        val str = result.toString()
        assertTrue(str.contains("total=5"))
        assertTrue(str.contains("success=4"))
        assertTrue(str.contains("failed=1"))
        assertTrue(str.contains("skipped=1"))
    }

    @Test
    fun `empty result is success`() {
        val result = MigrationResult(0, 0, 0, emptyList())
        assertTrue(result.isSuccess)
    }

    @Test
    fun `failedCount calculated from total success and skipped`() {
        val result = MigrationResult(10, 7, 2, listOf("null_key"))
        assertFalse(result.isSuccess)
        assertEquals(2, result.failedCount)
    }
}
