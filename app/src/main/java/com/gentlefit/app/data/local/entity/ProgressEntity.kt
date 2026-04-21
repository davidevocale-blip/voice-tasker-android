package com.gentlefit.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "progress")
data class ProgressEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val weight: Float? = null,
    val energyLevel: Int = 3,
    val sleepQuality: Int = 3,
    val mood: String = "NEUTRAL",
    val note: String? = null
)
