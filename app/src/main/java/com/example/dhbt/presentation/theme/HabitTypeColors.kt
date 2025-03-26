package com.example.dhbt.presentation.theme

import androidx.compose.ui.graphics.Color
import com.example.dhbt.domain.model.HabitType

// Цвета для разных типов привычек
val habitTypeColors = mapOf(
    HabitType.BINARY to Color(0xFF4CAF50),  // Зеленый для бинарных
    HabitType.QUANTITY to Color(0xFF2196F3), // Синий для количественных
    HabitType.TIME to Color(0xFFFFC107)      // Желтый для временных
)

object TaskPriorityColors {
    val high = Color(0xFFFF5252)      // Красный для высокого приоритета
    val medium = Color(0xFFFFB74D)    // Оранжевый для среднего приоритета
    val low = Color(0xFF4CAF50)       // Зеленый для низкого приоритета
}

// Цвета для обозначения статуса задач
object TaskStatusColors {
    val completed = Color(0xFF4CAF50)         // Зеленый для выполненных
    val inProgress = Color(0xFF2196F3)        // Синий для в процессе
    val pending = Color(0xFFFFC107)          // Желтый для ожидающих
    val overdue = Color(0xFFFF5252)          // Красный для просроченных
}

// Цвета для квадрантов матрицы Эйзенхауэра
object EisenhowerColors {
    val urgentImportant = Color(0xFFF44336).copy(alpha = 0.2f)      // Красный с прозрачностью
    val notUrgentImportant = Color(0xFF4CAF50).copy(alpha = 0.2f)   // Зеленый с прозрачностью
    val urgentNotImportant = Color(0xFFFF9800).copy(alpha = 0.2f)   // Оранжевый с прозрачностью
    val notUrgentNotImportant = Color(0xFF2196F3).copy(alpha = 0.2f) // Синий с прозрачностью
}