package com.answufeng.store

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SpMigrationStableIdTest {

    @Test
    fun stableIdForCryptKey_isDeterministic() {
        val a = SpMigration.stableIdForCryptKey("same-key")
        val b = SpMigration.stableIdForCryptKey("same-key")
        assertEquals(a, b)
    }

    @Test
    fun stableIdForCryptKey_differsForDifferentKeys() {
        val a = SpMigration.stableIdForCryptKey("a")
        val b = SpMigration.stableIdForCryptKey("b")
        assertNotEquals(a, b)
    }
}
