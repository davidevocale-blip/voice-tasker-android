package com.gentlefit.app.domain.model

data class MicroGoal(
    val id: Long = 0,
    val title: String,
    val description: String,
    val category: GoalCategory,
    val isCompleted: Boolean = false,
    val createdDate: String,
    val completedDate: String? = null,
    val streakDays: Int = 0
)

enum class GoalCategory(val displayName: String, val emoji: String) {
    ACQUA("Acqua", "💧"),
    MOVIMENTO("Movimento", "🚶"),
    RELAX("Relax", "🧘"),
    ALIMENTAZIONE("Alimentazione", "🥗")
}
