package com.voicetasker.app.notification

import androidx.core.os.LocaleListCompat
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationLocaleTest {

    @Test
    fun `system language uses the system locale when app locales are empty`() {
        assertEquals(
            Locale.ITALIAN,
            notificationLocale(
                applicationLocales = LocaleListCompat.getEmptyLocaleList(),
                systemLocale = Locale.ITALIAN
            )
        )
    }

    @Test
    fun `italian app language overrides the system locale`() {
        assertEquals(
            "it",
            notificationLocale(
                applicationLocales = LocaleListCompat.forLanguageTags("it"),
                systemLocale = Locale.ENGLISH
            ).language
        )
    }

    @Test
    fun `english app language overrides the system locale`() {
        assertEquals(
            "en",
            notificationLocale(
                applicationLocales = LocaleListCompat.forLanguageTags("en"),
                systemLocale = Locale.ITALIAN
            ).language
        )
    }
}
