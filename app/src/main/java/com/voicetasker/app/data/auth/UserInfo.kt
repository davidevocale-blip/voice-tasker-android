package com.voicetasker.app.data.auth

data class UserInfo(
    val id: String,
    val email: String,
    val displayName: String?,
    val avatarUrl: String?,
    val isPremium: Boolean = false,
    val subscriptionType: String? = null // "monthly", "yearly", "lifetime"
)
