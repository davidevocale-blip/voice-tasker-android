package com.voicetasker.app.domain.model

enum class ReminderType(val label: String, val offsetMs: Long) {
    ONE_DAY("1 giorno prima", 86_400_000L),
    TWELVE_HOURS("12 ore prima", 43_200_000L),
    TWO_HOURS("2 ore prima", 7_200_000L)
}

data class Reminder(
    val id: Long = 0,
    val noteId: Long = 0,
    val triggerAt: Long = 0,
    val type: ReminderType = ReminderType.TWO_HOURS,
    val isTriggered: Boolean = false,
    val workRequestId: String = ""
)
