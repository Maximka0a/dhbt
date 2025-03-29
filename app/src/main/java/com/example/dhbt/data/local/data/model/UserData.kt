package com.example.dhbt.data.local.data.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class UserData(
    val userId: String = UUID.randomUUID().toString(),
    val name: String = "Пользователь", // Устанавливаем дефолтное имя
    val email: String? = null,
    val avatarUrl: String? = null,
    val wakeUpTime: String? = null, // "HH:MM"
    val sleepTime: String? = null,  // "HH:MM"
    val usageGoal: String? = null,
    val isPremium: Boolean = false,
    val premiumExpiryDate: Long? = null,
    val customEmoji: String? = null, // Выбранный пользователем смайлик
    val registrationDate: Long = System.currentTimeMillis(),
    val lastLoginDate: Long = System.currentTimeMillis()
)