package com.voicetasker.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DefaultCategoryKeyTest {
    @Test
    fun `known persisted values map to domain keys`() {
        assertEquals(DefaultCategoryKey.WORK, DefaultCategoryKey.fromPersistedValue("work"))
        assertEquals(DefaultCategoryKey.FAMILY, DefaultCategoryKey.fromPersistedValue("family"))
        assertEquals(DefaultCategoryKey.HEALTH, DefaultCategoryKey.fromPersistedValue("health"))
    }

    @Test
    fun `null and unknown persisted values map to null`() {
        assertNull(DefaultCategoryKey.fromPersistedValue(null))
        assertNull(DefaultCategoryKey.fromPersistedValue("unknown"))
    }

    @Test
    fun `every key round trips through its persisted value`() {
        DefaultCategoryKey.entries.forEach { key ->
            assertEquals(key, DefaultCategoryKey.fromPersistedValue(key.persistedValue))
        }
    }
}
