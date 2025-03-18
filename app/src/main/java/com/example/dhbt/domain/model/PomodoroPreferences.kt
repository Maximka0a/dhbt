package com.example.dhbt.domain.model

data class PomodoroPreferences(
    val workDuration: Int = 25,
    val shortBreakDuration: Int = 5,
    val longBreakDuration: Int = 15,
    val cyclesBeforeLongBreak: Int = 4,

    val autoStartNextSession: Boolean = false,
    val keepScreenOn: Boolean = true,

    val soundEnabled: Boolean = true,
    val soundId: String = "default",
    val vibrationEnabled: Boolean = true,

    val showRemainingTime: Boolean = true,
    val showNotification: Boolean = true
)