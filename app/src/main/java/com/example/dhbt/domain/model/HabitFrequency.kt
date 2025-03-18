package com.example.dhbt.domain.model

data class HabitFrequency(
    val id: String,
    val habitId: String,
    val type: FrequencyType,
    val daysOfWeek: List<Int>? = null,
    val timesPerPeriod: Int? = null,
    val periodType: PeriodType? = null
)

enum class FrequencyType(val value: Int) {
    DAILY(0),
    SPECIFIC_DAYS(1),
    TIMES_PER_WEEK(2),
    TIMES_PER_MONTH(3);

    companion object {
        fun fromInt(value: Int): FrequencyType = values().firstOrNull { it.value == value } ?: DAILY
    }
}

enum class PeriodType(val value: Int) {
    WEEK(0),
    MONTH(1);

    companion object {
        fun fromInt(value: Int): PeriodType = values().firstOrNull { it.value == value } ?: WEEK
    }
}