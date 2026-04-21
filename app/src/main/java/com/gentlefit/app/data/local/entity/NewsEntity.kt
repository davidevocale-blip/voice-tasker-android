package com.gentlefit.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "news")
data class NewsEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val summary: String,
    val content: String,
    val category: String,
    val imageResName: String? = null,
    val publishedDate: String,
    val isRead: Boolean = false
)
