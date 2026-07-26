package com.voicetasker.app.data.recorder

import androidx.core.os.LocaleListCompat
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class SpeechRecognitionLocaleResolverTest {

    @Test
    fun `italian application locale resolves to italian Italy`() {
        assertEquals(
            "it-IT",
            resolve(
                applicationLanguageTags = "it",
                systemLocale = Locale.US
            )
        )
    }

    @Test
    fun `english application locale resolves to english US`() {
        assertEquals(
            "en-US",
            resolve(
                applicationLanguageTags = "en-GB",
                systemLocale = Locale.ITALY
            )
        )
    }

    @Test
    fun `system language with italian system locale resolves to italian Italy`() {
        assertEquals("it-IT", resolve(systemLocale = Locale.ITALY))
    }

    @Test
    fun `system language preserves british English`() {
        assertEquals("en-GB", resolve(systemLocale = Locale.UK))
    }

    @Test
    fun `system language preserves american English`() {
        assertEquals("en-US", resolve(systemLocale = Locale.US))
    }

    @Test
    fun `empty application locales use the system locale`() {
        assertEquals(
            "fr-FR",
            resolve(
                applicationLanguageTags = "",
                systemLocale = Locale.FRANCE
            )
        )
    }

    @Test
    fun `unsupported application locale uses the system locale`() {
        assertEquals(
            "en-GB",
            resolve(
                applicationLanguageTags = "de-DE",
                systemLocale = Locale.UK
            )
        )
    }

    @Test
    fun `missing valid locale uses stable fallback`() {
        assertEquals(
            "en-US",
            SpeechRecognitionLocaleResolver.resolve(
                applicationLocales = LocaleListCompat.getEmptyLocaleList(),
                systemLocale = null
            )
        )
        assertEquals(
            "en-US",
            SpeechRecognitionLocaleResolver.resolve(
                applicationLocales = LocaleListCompat.getEmptyLocaleList(),
                systemLocale = Locale.ROOT
            )
        )
    }

    @Test
    fun `new sessions can resolve a changed application locale`() {
        var applicationLocales = LocaleListCompat.forLanguageTags("it")
        val sessionResolver = SpeechRecognitionLocaleResolver(
            applicationLocalesProvider = { applicationLocales },
            systemLocaleProvider = { Locale.US }
        )

        assertEquals("it-IT", sessionResolver.resolveForNewSession())

        applicationLocales = LocaleListCompat.forLanguageTags("en")

        assertEquals("en-US", sessionResolver.resolveForNewSession())
    }

    @Test
    fun `resolving a locale does not mutate the global default`() {
        val originalDefault = Locale.getDefault()

        resolve(
            applicationLanguageTags = "it",
            systemLocale = Locale.UK
        )
        resolve(
            applicationLanguageTags = "en",
            systemLocale = Locale.ITALY
        )

        assertEquals(originalDefault, Locale.getDefault())
    }

    private fun resolve(
        applicationLanguageTags: String = "",
        systemLocale: Locale?
    ): String = SpeechRecognitionLocaleResolver.resolve(
        applicationLocales = LocaleListCompat.forLanguageTags(
            applicationLanguageTags
        ),
        systemLocale = systemLocale
    )
}
