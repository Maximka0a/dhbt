package com.example.dhbt.domain.model

import java.util.UUID

enum class NotificationTarget(val value: Int) {
    TASK(0),
    HABIT(1),
    SYSTEM(2);

    companion object {
        fun fromInt(value: Int): NotificationTarget = values().first { it.value == value }
    }
}

data class Notification(
    val id: String = UUID.randomUUID().toString(),
    val targetId: String,
    val targetType: NotificationTarget,
    val time: String, // "HH:MM"
    val daysOfWeek: List<Int>? = null, // [1-7] для дней недели
    val isEnabled: Boolean = true,
    val message: String? = null,
    val workId: String? = null,
    val repeatInterval: Int? = null
)