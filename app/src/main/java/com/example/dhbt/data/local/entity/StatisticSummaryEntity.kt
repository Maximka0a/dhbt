package com.example.dhbt.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "statistic_summaries")
data class StatisticSummaryEntity(
    @PrimaryKey
    val summaryId: String = UUID.randomUUID().toString(),
    val date: Long,
    val periodType: Int, // 0-день, 1-неделя, 2-месяц, 3-год
    val taskCompletionPercentage: Float? = null,
    val habitCompletionPercentage: Float? = null,
    val totalPomodoroMinutes: Int? = null,
    val productiveStreak: Int? = null,
    val tasksCategorySummary: String? = null, // JSON с распределением по категориям
    val tasksPrioritySummary: String? = null, // JSON с распределением по приоритетам
    val habitsSuccessRate: String? = null, // JSON с данными успешности привычек
    val pomodoroDistribution: String? = null // JSON с распределением Pomodoro-сессий
)