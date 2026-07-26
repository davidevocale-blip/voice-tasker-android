package com.voicetasker.app.ui.localization

import androidx.core.os.LocaleListCompat

enum class AppLanguage(val languageTags: String) {
    SYSTEM(""),
    ITALIAN("it"),
    ENGLISH("en");

    fun toLocaleList(): LocaleListCompat =
        if (this == SYSTEM) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(languageTags)
        }

    fun matches(locales: LocaleListCompat): Boolean = this == fromLocaleList(locales)

    companion object {
        fun fromLocaleList(locales: LocaleListCompat): AppLanguage =
            when (locales[0]?.language) {
                ITALIAN.languageTags -> ITALIAN
                ENGLISH.languageTags -> ENGLISH
                else -> SYSTEM
            }
    }
}
