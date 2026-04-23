package com.answufeng.store

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MigrationResultTest {

    @Test
    fun isSuccess_whenNoFailuresAndNoSkips() {
        assertTrue(MigrationResult(2, 2, 0, emptyList()).isSuccess)
    }

    @Test
    fun isSuccess_falseWhenFailed() {
        assertFalse(MigrationResult(2, 1, 1, emptyList()).isSuccess)
    }

    @Test
    fun isSuccess_falseWhenSkipped() {
        assertFalse(MigrationResult(2, 1, 0, listOf("k")).isSuccess)
    }
}
