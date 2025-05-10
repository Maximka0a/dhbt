package com.example.dhbt.domain.model

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

enum class NotificationTarget(val value: Int) {
    TASK(0),
    HABIT(1),
    SYSTEM(2);

    companion object {
        fun fromInt(value: Int): NotificationTarget = values().first { it.value == value }
    }
}


/**
 * Модель уведомления
 */
data class Notification(
    val id: String = UUID.randomUUID().toString(),
    val targetId: String,
    val targetType: NotificationTarget,
    val title: String? = null,
    val message: String? = null,
    val time: String = "09:00",
    val scheduledDate: Long? = null,  // Добавлено поле с датой уведомления
    val daysOfWeek: List<Int> = emptyList(),  // По умолчанию пустой список, не null
    val repeatInterval: Int? = null,
    val workId: String? = null,
    val isEnabled: Boolean = true,
    val priority: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Форматирует время уведомления в читаемый вид
     */
    fun getFormattedTime(): String {
        return try {
            val localTime = LocalTime.parse(time, DateTimeFormatter.ofPattern("HH:mm"))
            localTime.format(DateTimeFormatter.ofPattern("HH:mm"))
        } catch (e: Exception) {
            time
        }
    }

    /**
     * Возвращает дату уведомления в читаемом виде
     */
    fun getFormattedDate(): String? {
        return scheduledDate?.let {
            try {
                val date = LocalDate.ofEpochDay(it / (24 * 60 * 60 * 1000))
                date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * Проверяет, является ли уведомление повторяющимся
     */
    fun isRecurring(): Boolean {
        return repeatInterval != null || daysOfWeek.isNotEmpty()
    }
}