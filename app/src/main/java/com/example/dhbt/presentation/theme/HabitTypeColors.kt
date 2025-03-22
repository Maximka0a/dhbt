package com.example.dhbt.presentation.theme

import androidx.compose.ui.graphics.Color
import com.example.dhbt.domain.model.HabitType

// Цвета для разных типов привычек
val habitTypeColors = mapOf(
    HabitType.BINARY to Color(0xFF4CAF50),  // Зеленый для бинарных
    HabitType.QUANTITY to Color(0xFF2196F3), // Синий для количественных
    HabitType.TIME to Color(0xFFFFC107)      // Желтый для временных
)