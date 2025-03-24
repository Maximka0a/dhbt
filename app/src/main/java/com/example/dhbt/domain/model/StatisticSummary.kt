package com.example.dhbt.domain.model

data class StatisticSummary(
    val id: String,
    val date: Long,
    val periodType: StatisticPeriod,
    val taskCompletionPercentage: Float? = null,
    val habitCompletionPercentage: Float? = null,
    val totalPomodoroMinutes: Int? = null,
    val productiveStreak: Int? = null,
    val tasksCategorySummary: Map<String, Int>? = null,
    val tasksPrioritySummary: Map<TaskPriority, Int>? = null,
    val habitsSuccessRate: Map<String, Float>? = null,
    val pomodoroDistribution: Map<String, Int>? = null
)

enum class StatisticPeriod(val value: Int, val displayName: String) {
    DAY(0, "День"),
    WEEK(1, "Неделя"),
    MONTH(2, "Месяц"),
    YEAR(3, "Год");

    companion object {
        fun fromInt(value: Int): StatisticPeriod = values().firstOrNull { it.value == value } ?: DAY
    }
}