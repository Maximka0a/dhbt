package com.example.dhbt.domain.model

data class Habit(
    val id: String,
    val title: String,
    val description: String? = null,
    val iconEmoji: String? = null,
    val color: String? = null,
    val creationDate: Long,
    val type: HabitType,
    val targetValue: Float? = null,
    val unitOfMeasurement: String? = null,
    val targetStreak: Int? = null,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val status: HabitStatus = HabitStatus.ACTIVE,
    val pausedDate: Long? = null,
    val categoryId: String? = null,
    val frequency: HabitFrequency? = null
)

enum class HabitType(val value: Int) {
    BINARY(0),
    QUANTITY(1),
    TIME(2);

    companion object {
        fun fromInt(value: Int): HabitType = values().firstOrNull { it.value == value } ?: BINARY
    }
}

enum class HabitStatus(val value: Int) {
    ACTIVE(0),
    PAUSED(1),
    ARCHIVED(2);

    companion object {
        fun fromInt(value: Int): HabitStatus = values().firstOrNull { it.value == value } ?: ACTIVE
    }
}