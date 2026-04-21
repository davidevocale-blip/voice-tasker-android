package com.gentlefit.app.domain.model

data class DailyRoutine(
    val id: Long = 0,
    val date: String,
    val exercise: Exercise,
    val foodTip: FoodTip,
    val dailyGoal: DailyGoal,
    val isExerciseCompleted: Boolean = false,
    val isFoodTipFollowed: Boolean = false,
    val isGoalCompleted: Boolean = false
) {
    val completionCount: Int
        get() = listOf(isExerciseCompleted, isFoodTipFollowed, isGoalCompleted).count { it }

    val isFullyCompleted: Boolean
        get() = completionCount == 3

    val completionPercentage: Float
        get() = completionCount / 3f
}

data class Exercise(
    val title: String,
    val description: String,
    val durationMinutes: Int,
    val type: ExerciseType
)

enum class ExerciseType(val displayName: String, val emoji: String) {
    STRETCHING("Stretching", "🧘"),
    MOBILITA("Mobilità", "🤸"),
    CAMMINATA("Camminata", "🚶‍♀️"),
    YOGA("Yoga dolce", "🕊️"),
    RESPIRAZIONE("Respirazione", "🌬️"),
    POSTURA("Postura", "💆‍♀️")
}

data class FoodTip(
    val text: String
)

data class DailyGoal(
    val text: String
)
