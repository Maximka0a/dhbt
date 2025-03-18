package com.example.dhbt.domain.model

data class Notification(
    val id: String,
    val targetId: String,
    val targetType: NotificationTarget,
    val time: String,
    val daysOfWeek: List<Int>? = null,
    val isEnabled: Boolean = true,
    val message: String? = null,
    val repeatInterval: Int? = null
)

enum class NotificationTarget(val value: Int) {
    TASK(0),
    HABIT(1);

    companion object {
        fun fromInt(value: Int): NotificationTarget = values().firstOrNull { it.value == value } ?: TASK
    }
}