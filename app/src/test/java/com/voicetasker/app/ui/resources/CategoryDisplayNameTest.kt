package com.voicetasker.app.ui.resources

import com.voicetasker.app.R
import com.voicetasker.app.domain.model.Category
import com.voicetasker.app.domain.model.DefaultCategoryKey
import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryDisplayNameTest {
    @Test
    fun `historical work category uses localized resource`() {
        val category = Category(
            name = "Lavoro",
            canonicalKey = DefaultCategoryKey.WORK
        )

        assertEquals(
            UiText.Resource(R.string.category_default_work),
            category.displayName()
        )
    }

    @Test
    fun `historical family names use localized resource`() {
        listOf("Famiglia", "Personale").forEach { persistedName ->
            assertEquals(
                UiText.Resource(R.string.category_default_family),
                category(persistedName, DefaultCategoryKey.FAMILY).displayName()
            )
        }
    }

    @Test
    fun `historical health category uses localized resource`() {
        assertEquals(
            UiText.Resource(R.string.category_default_health),
            category("Salute", DefaultCategoryKey.HEALTH).displayName()
        )
    }

    @Test
    fun `renamed defaults keep their persisted names`() {
        listOf(
            category("Ufficio", DefaultCategoryKey.WORK),
            category("Casa", DefaultCategoryKey.FAMILY),
            category("Benessere", DefaultCategoryKey.HEALTH)
        ).forEach { category ->
            assertEquals(UiText.Dynamic(category.name), category.displayName())
        }
    }

    @Test
    fun `custom category keeps its persisted name even when it looks translated`() {
        assertEquals(
            UiText.Dynamic("Work"),
            category("Work", canonicalKey = null).displayName()
        )
    }

    @Test
    fun `resolving a display name does not mutate the category`() {
        val category = category("Lavoro", DefaultCategoryKey.WORK)

        category.displayName()

        assertEquals("Lavoro", category.name)
    }

    @Test
    fun `color-only edit preserves original persisted name`() {
        assertEquals(
            "Lavoro",
            persistedCategoryName(
                originalPersistedName = "Lavoro",
                editedName = "Work",
                hasNameChanged = false
            )
        )
    }

    @Test
    fun `explicit name edit persists new name`() {
        assertEquals(
            "Office",
            persistedCategoryName(
                originalPersistedName = "Lavoro",
                editedName = "Office",
                hasNameChanged = true
            )
        )
    }

    @Test
    fun `explicit edit back to localized text persists that text`() {
        assertEquals(
            "Work",
            persistedCategoryName(
                originalPersistedName = "Lavoro",
                editedName = "Work",
                hasNameChanged = true
            )
        )
    }

    private fun category(
        name: String,
        canonicalKey: DefaultCategoryKey?
    ) = Category(name = name, canonicalKey = canonicalKey)
}
