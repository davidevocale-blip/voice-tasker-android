package com.gentlefit.app.domain.model

data class ProgressEntry(
    val id: Long = 0,
    val date: String,
    val weight: Float? = null,
    val energyLevel: Int = 3,       // 1-5
    val sleepQuality: Int = 3,      // 1-5
    val mood: Mood = Mood.NEUTRAL,
    val note: String? = null
)

enum class Mood(val emoji: String, val label: String) {
    GREAT("😊", "Benissimo"),
    GOOD("🙂", "Bene"),
    NEUTRAL("😐", "Così così"),
    LOW("😔", "Non al meglio"),
    STRONG("💪", "Forte"),
    PEACEFUL("🌸", "In pace")
}
