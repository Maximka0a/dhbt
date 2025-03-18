package com.example.dhbt.domain.model

data class TaskRecurrence(
    val id: String,
    val taskId: String,
    val type: RecurrenceType,
    val daysOfWeek: List<Int>? = null,
    val monthDay: Int? = null,
    val customInterval: Int? = null,
    val startDate: Long,
    val endDate: Long? = null
)

enum class RecurrenceType(val value: Int) {
    DAILY(0),
    WEEKLY(1),
    MONTHLY(2),
    YEARLY(3),
    CUSTOM(4);

    companion object {
        fun fromInt(value: Int): RecurrenceType = values().firstOrNull { it.value == value } ?: DAILY
    }
}