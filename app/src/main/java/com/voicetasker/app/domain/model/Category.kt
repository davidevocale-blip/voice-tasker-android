package com.voicetasker.app.domain.model

data class Category(
    val id: Long = 0,
    val name: String = "",
    val colorHex: String = "#6C63FF",
    val iconName: String = "Label",
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
