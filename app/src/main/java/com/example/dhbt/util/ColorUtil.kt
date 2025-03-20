package com.example.dhbt.presentation.util

import androidx.compose.ui.graphics.Color

/**
 * Преобразует строку с HEX кодом цвета в объект Color
 * @param defaultColor цвет по умолчанию, если преобразование не удалось
 */
fun String?.toColor(defaultColor: Color = Color.Gray): Color {
    if (this == null) return defaultColor

    return try {
        val colorString = this.trim()
            .removePrefix("#")
            .padStart(6, '0')

        when (colorString.length) {
            6 -> Color(
                red = colorString.substring(0, 2).toInt(16) / 255f,
                green = colorString.substring(2, 4).toInt(16) / 255f,
                blue = colorString.substring(4, 6).toInt(16) / 255f
            )
            8 -> Color(
                red = colorString.substring(0, 2).toInt(16) / 255f,
                green = colorString.substring(2, 4).toInt(16) / 255f,
                blue = colorString.substring(4, 6).toInt(16) / 255f,
                alpha = colorString.substring(6, 8).toInt(16) / 255f
            )
            else -> defaultColor
        }
    } catch (e: Exception) {
        defaultColor
    }
}