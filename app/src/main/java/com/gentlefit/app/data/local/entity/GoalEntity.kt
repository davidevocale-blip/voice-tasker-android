package com.gentlefit.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val category: String,
    val isCompleted: Boolean = false,
    val createdDate: String,
    val completedDate: String? = null,
    val streakDays: Int = 0
)
