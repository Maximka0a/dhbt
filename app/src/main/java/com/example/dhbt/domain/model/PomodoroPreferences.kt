package com.example.dhbt.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class PomodoroPreferences(
    // Use these property names to match repository usage
    val workDuration: Int = 25,  // Changed from workDurationMinutes
    val shortBreakDuration: Int = 5,  // Changed from shortBreakMinutes
    val longBreakDuration: Int = 15,  // Changed from longBreakMinutes
    val pomodorosUntilLongBreak: Int = 4,
    val autoStartBreaks: Boolean = true,
    val autoStartPomodoros: Boolean = false,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val customSoundUri: String? = null,
    val soundVolume: Int = 70,
    val keepScreenOn: Boolean = false
)