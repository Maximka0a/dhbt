package com.example.dhbt.data.local.data.model

import kotlinx.serialization.Serializable

@Serializable
data class UserPreferences(
    // Основные настройки интерфейса
    val startScreenType: Int = 0, // 0-дашборд, 1-задачи, 2-привычки
    val defaultTaskView: Int = 0, // 0-список, 1-канбан, 2-эйзенхауэр
    val defaultTaskSort: Int = 0, // 0-по дате, 1-по приоритету, 2-по алфавиту
    val defaultHabitSort: Int = 0, // 0-по алфавиту, 1-по стрику

    // Внешний вид
    val theme: Int = 2, // 0-светлая, 1-темная, 2-системная
    val language: String = "ru",

    // Тихие часы
    val quietHoursEnabled: Boolean = false,
    val quietHoursStart: String? = null, // "HH:MM"
    val quietHoursEnd: String? = null, // "HH:MM"

    // Уведомления
    val reminderTimeBeforeTask: Int = 15, // В минутах
    val defaultSoundEnabled: Boolean = true,
    val defaultVibrationEnabled: Boolean = true,

    // Другие настройки
    val cloudSyncEnabled: Boolean = false,
    val hiddenCategories: List<String> = emptyList(),

    // Онбординг
    val hasCompletedOnboarding: Boolean = false
)