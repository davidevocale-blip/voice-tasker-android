package com.voicetasker.app.ui.localization

import androidx.core.os.LocaleListCompat
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class AppLanguageTest {

    @Test
    fun `system generates an empty locale list`() {
        assertTrue(AppLanguage.SYSTEM.toLocaleList().isEmpty)
    }

    @Test
    fun `explicit languages generate their canonical language tags`() {
        assertEquals("it", AppLanguage.ITALIAN.toLocaleList().toLanguageTags())
        assertEquals("en", AppLanguage.ENGLISH.toLocaleList().toLanguageTags())
    }

    @Test
    fun `empty and supported locale lists resolve to an app language`() {
        assertEquals(
            AppLanguage.SYSTEM,
            AppLanguage.fromLocaleList(LocaleListCompat.getEmptyLocaleList())
        )
        assertEquals(
            AppLanguage.ITALIAN,
            AppLanguage.fromLocaleList(LocaleListCompat.forLanguageTags("it"))
        )
        assertEquals(
            AppLanguage.ITALIAN,
            AppLanguage.fromLocaleList(LocaleListCompat.forLanguageTags("it-IT"))
        )
        assertEquals(
            AppLanguage.ENGLISH,
            AppLanguage.fromLocaleList(LocaleListCompat.forLanguageTags("en"))
        )
        assertEquals(
            AppLanguage.ENGLISH,
            AppLanguage.fromLocaleList(LocaleListCompat.forLanguageTags("en-US"))
        )
        assertEquals(
            AppLanguage.ENGLISH,
            AppLanguage.fromLocaleList(LocaleListCompat.forLanguageTags("en-GB"))
        )
    }

    @Test
    fun `unsupported locale safely falls back to system`() {
        assertEquals(
            AppLanguage.SYSTEM,
            AppLanguage.fromLocaleList(LocaleListCompat.forLanguageTags("fr-FR"))
        )
    }

    @Test
    fun `language choice detects whether it matches the active locales`() {
        assertTrue(
            AppLanguage.ITALIAN.matches(LocaleListCompat.forLanguageTags("it-IT"))
        )
        assertFalse(
            AppLanguage.ENGLISH.matches(LocaleListCompat.forLanguageTags("it-IT"))
        )
    }
}
