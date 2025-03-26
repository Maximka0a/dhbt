package com.example.dhbt.presentation.theme

import androidx.compose.ui.graphics.Color

/**
 * Набор цветов для отображения прогресса
 */
object ProgressColors {
    val completed = Color(0xFF4CAF50) // Зеленый - полное выполнение
    val excellent = Color(0xFF8BC34A) // Светло-зеленый - отличный прогресс
    val good = Color(0xFFFFEB3B)      // Желтый - хороший прогресс
    val moderate = Color(0xFFFF9800)  // Оранжевый - средний прогресс
    val poor = Color(0xFFF44336)      // Красный - слабый прогресс
    val neutral = Color(0xFF2196F3)   // Синий - нейтральный (когда всего 0)
}