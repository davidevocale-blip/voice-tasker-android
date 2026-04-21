package com.gentlefit.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "routines")
data class RoutineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val exerciseTitle: String,
    val exerciseDescription: String,
    val exerciseDurationMin: Int,
    val exerciseType: String,
    val foodTip: String,
    val dailyGoal: String,
    val isExerciseCompleted: Boolean = false,
    val isFoodTipFollowed: Boolean = false,
    val isGoalCompleted: Boolean = false
)
