package com.example.dhbt.presentation.navigation

import kotlinx.serialization.Serializable

// Главные маршруты (нижняя навигация)
@Serializable
object Dashboard

@Serializable
object Tasks

@Serializable
object Habits

@Serializable
object Statistics

@Serializable
object More

// Маршруты для задач
@Serializable
data class TaskDetail(val taskId: String)

@Serializable
data class TaskEdit(val taskId: String = "")

// Маршруты для привычек
@Serializable
data class HabitDetail(val habitId: String)

@Serializable
data class HabitEdit(val habitId: String = "")

// Маршруты для Pomodoro
@Serializable
data class Pomodoro(
    val taskId: String = ""
)

// Маршруты для Матрицы Эйзенхауэра
@Serializable
object EisenhowerMatrix

// Маршруты для настроек и дополнительных экранов
@Serializable
object Settings

@Serializable
object Onboarding

@Serializable
object PremiumSubscription