package com.example.dhbt.presentation.util

import org.threeten.bp.Instant
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalTime
import org.threeten.bp.ZoneId

fun Long.toLocalDate(): LocalDate {
    return Instant
        .ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
}

fun String.toLocalTime(): LocalTime? {
    return try {
        val parts = this.split(":")
        if (parts.size >= 2) {
            LocalTime.of(parts[0].toInt(), parts[1].toInt())
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }
}