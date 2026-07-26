package com.voicetasker.app.domain.model

enum class DefaultCategoryKey(val persistedValue: String) {
    WORK("work"),
    FAMILY("family"),
    HEALTH("health");

    companion object {
        fun fromPersistedValue(value: String?): DefaultCategoryKey? =
            entries.firstOrNull { it.persistedValue == value }
    }
}
