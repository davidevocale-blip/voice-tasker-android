package com.voicetasker.app.data.recorder

import androidx.core.os.LocaleListCompat
import java.util.Locale

fun interface SpeechRecognitionApplicationLocalesProvider {
    fun getApplicationLocales(): LocaleListCompat
}

class SpeechRecognitionLocaleResolver(
    private val applicationLocalesProvider: () -> LocaleListCompat,
    private val systemLocaleProvider: () -> Locale?
) {
    fun resolveForNewSession(): String = resolve(
        applicationLocales = applicationLocalesProvider(),
        systemLocale = systemLocaleProvider()
    )

    companion object {
        private const val ITALIAN_ITALY = "it-IT"
        private const val ENGLISH_US = "en-US"

        /**
         * Resolves the language tag for a new recognition session.
         *
         * Explicitly supported app languages use stable recognition locales.
         * System mode and unsupported app locales preserve a valid system tag.
         * English US is the stable fallback when no usable locale is available.
         */
        fun resolve(
            applicationLocales: LocaleListCompat,
            systemLocale: Locale?
        ): String = when (applicationLocales[0]?.language) {
            "it" -> ITALIAN_ITALY
            "en" -> ENGLISH_US
            else -> systemLanguageTag(systemLocale)
        }

        private fun systemLanguageTag(locale: Locale?): String {
            val language = locale?.language
                ?.lowercase(Locale.ROOT)
                ?.takeUnless { it.isBlank() || it == "und" }
                ?: return ENGLISH_US

            return when {
                language == "it" && locale.country.isBlank() -> ITALIAN_ITALY
                language == "en" && locale.country.isBlank() -> ENGLISH_US
                else -> locale.toLanguageTag()
                    .takeUnless { it.isBlank() || it == "und" }
                    ?: ENGLISH_US
            }
        }
    }
}
