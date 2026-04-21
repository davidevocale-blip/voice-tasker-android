package com.gentlefit.app.domain.model

data class NewsArticle(
    val id: Long = 0,
    val title: String,
    val summary: String,
    val content: String,
    val category: NewsCategory,
    val imageResName: String? = null,
    val publishedDate: String,
    val isRead: Boolean = false
)

enum class NewsCategory(val displayName: String, val emoji: String) {
    BENESSERE("Benessere", "🌿"),
    ALIMENTAZIONE("Alimentazione", "🥑"),
    MOVIMENTO("Movimento", "🏃‍♀️"),
    MENTE("Mente", "🧠")
}
