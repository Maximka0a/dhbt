package com.example.dhbt.domain.model

data class UserPreferences(
    val startScreenType: StartScreen = StartScreen.DASHBOARD,
    val defaultTaskView: TaskView = TaskView.LIST,
    val defaultTaskSort: TaskSort = TaskSort.DATE,
    val defaultHabitSort: HabitSort = HabitSort.ALPHABETICAL,

    val theme: AppTheme = AppTheme.SYSTEM,
    val language: String = "ru",

    val quietHoursEnabled: Boolean = false,
    val quietHoursStart: String? = null,
    val quietHoursEnd: String? = null,

    val reminderTimeBeforeTask: Int = 15,
    val defaultSoundEnabled: Boolean = true,
    val defaultVibrationEnabled: Boolean = true,

    val cloudSyncEnabled: Boolean = false,
    val hiddenCategories: List<String> = emptyList(),

    val hasCompletedOnboarding: Boolean = false
)

enum class StartScreen(val value: Int) {
    DASHBOARD(0),
    TASKS(1),
    HABITS(2);

    companion object {
        fun fromInt(value: Int): StartScreen = values().firstOrNull { it.value == value } ?: DASHBOARD
    }
}

enum class TaskView(val value: Int) {
    LIST(0),
    KANBAN(1),
    EISENHOWER(2);

    companion object {
        fun fromInt(value: Int): TaskView = values().firstOrNull { it.value == value } ?: LIST
    }
}

enum class TaskSort(val value: Int) {
    DATE(0),
    PRIORITY(1),
    ALPHABETICAL(2);

    companion object {
        fun fromInt(value: Int): TaskSort = values().firstOrNull { it.value == value } ?: DATE
    }
}

enum class HabitSort(val value: Int) {
    ALPHABETICAL(0),
    STREAK(1);

    companion object {
        fun fromInt(value: Int): HabitSort = values().firstOrNull { it.value == value } ?: ALPHABETICAL
    }
}

enum class AppTheme(val value: Int) {
    LIGHT(0),
    DARK(1),
    SYSTEM(2);

    companion object {
        fun fromInt(value: Int): AppTheme = values().firstOrNull { it.value == value } ?: SYSTEM
    }
}