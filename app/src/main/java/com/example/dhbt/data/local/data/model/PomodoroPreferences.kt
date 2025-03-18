package com.example.dhbt.data.local.data.model

import kotlinx.serialization.Serializable

@Serializable
data class PomodoroPreferences(
    // Длительность интервалов
    val workDuration: Int = 25, // В минутах
    val shortBreakDuration: Int = 5, // В минутах
    val longBreakDuration: Int = 15, // В минутах
    val cyclesBeforeLongBreak: Int = 4,

    // Настройки поведения
    val autoStartNextSession: Boolean = false,
    val keepScreenOn: Boolean = true,

    // Уведомления
    val soundEnabled: Boolean = true,
    val soundId: String = "default",
    val vibrationEnabled: Boolean = true,

    // Внешний вид
    val showRemainingTime: Boolean = true,
    val showNotification: Boolean = true
)