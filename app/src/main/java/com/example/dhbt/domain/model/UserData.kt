package com.example.dhbt.domain.model

data class UserData(
    val userId: String,
    val name: String = "",
    val email: String? = null,
    val avatarUrl: String? = null,
    val wakeUpTime: String? = null,
    val sleepTime: String? = null,
    val usageGoal: String? = null,
    val isPremium: Boolean = false,
    val premiumExpiryDate: Long? = null,
    val customEmoji: String? = null,
    val registrationDate: Long,
    val lastLoginDate: Long
)