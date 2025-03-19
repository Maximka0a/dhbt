package com.example.dhbt.domain.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class UserData(
    val userId: String = UUID.randomUUID().toString(),
    val name: String = "",
    val email: String? = null,
    val avatarUrl: String? = null,
    val wakeUpTime: String? = null,
    val sleepTime: String? = null,
    val usageGoal: String? = null,
    val isPremium: Boolean = false,
    val premiumExpiryDate: Long? = null,
    val customEmoji: String? = null,
    val registrationDate: Long = System.currentTimeMillis(),
    val lastLoginDate: Long = System.currentTimeMillis()
)