package com.example.dhbt.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.example.dhbt.domain.model.HabitType

// Расширение для пользовательских цветовых настроек
data class DHbtExtendedColors(
    val taskPriority: TaskPriorityColorScheme,
    val taskStatus: TaskStatusColorScheme,
    val eisenhower: EisenhowerColorScheme,
    val habitTypes: HabitTypeColorScheme
)

data class TaskPriorityColorScheme(
    val high: Color,
    val medium: Color,
    val low: Color
)

data class TaskStatusColorScheme(
    val completed: Color,
    val inProgress: Color,
    val pending: Color,
    val overdue: Color
)

data class EisenhowerColorScheme(
    val urgentImportant: Color,
    val notUrgentImportant: Color,
    val urgentNotImportant: Color,
    val notUrgentNotImportant: Color
)

data class HabitTypeColorScheme(
    val binary: Color,
    val quantity: Color,
    val time: Color
)

// Локальный композиционный ключ для расширенных цветов
val LocalExtendedColors = staticCompositionLocalOf {
    DHbtExtendedColors(
        taskPriority = TaskPriorityColorScheme(
            high = TaskPriorityColors.high,
            medium = TaskPriorityColors.medium,
            low = TaskPriorityColors.low
        ),
        taskStatus = TaskStatusColorScheme(
            completed = TaskStatusColors.completed,
            inProgress = TaskStatusColors.inProgress,
            pending = TaskStatusColors.pending,
            overdue = TaskStatusColors.overdue
        ),
        eisenhower = EisenhowerColorScheme(
            urgentImportant = EisenhowerColors.urgentImportant,
            notUrgentImportant = EisenhowerColors.notUrgentImportant,
            urgentNotImportant = EisenhowerColors.urgentNotImportant,
            notUrgentNotImportant = EisenhowerColors.notUrgentNotImportant
        ),
        habitTypes = HabitTypeColorScheme(
            binary = habitTypeColors[HabitType.BINARY] ?: Color(0xFF4CAF50),
            quantity = habitTypeColors[HabitType.QUANTITY] ?: Color(0xFF2196F3),
            time = habitTypeColors[HabitType.TIME] ?: Color(0xFFFFC107)
        )
    )
}

// Расширение для MaterialTheme чтобы добавить кастомные цвета
val MaterialTheme.extendedColors: DHbtExtendedColors
    @Composable
    @ReadOnlyComposable
    get() = LocalExtendedColors.current