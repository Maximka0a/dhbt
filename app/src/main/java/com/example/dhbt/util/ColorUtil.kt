package com.example.dhbt.presentation.util

import androidx.compose.ui.graphics.Color

/**
 * Преобразует строковое представление цвета в объект Color
 * Поддерживает форматы:
 * - "#RRGGBB"
 * - "#AARRGGBB"
 */
fun String?.toColor(defaultColor: Color = Color.Gray): Color {
    if (this == null || this.isEmpty()) return defaultColor

    return try {
        when {
            startsWith("#") && length == 7 -> {
                Color(
                    red = substring(1, 3).toInt(16) / 255f,
                    green = substring(3, 5).toInt(16) / 255f,
                    blue = substring(5, 7).toInt(16) / 255f
                )
            }
            startsWith("#") && length == 9 -> {
                Color(
                    alpha = substring(1, 3).toInt(16) / 255f,
                    red = substring(3, 5).toInt(16) / 255f,
                    green = substring(5, 7).toInt(16) / 255f,
                    blue = substring(7, 9).toInt(16) / 255f
                )
            }
            else -> defaultColor
        }
    } catch (e: Exception) {
        defaultColor
    }
}