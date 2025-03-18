package com.example.dhbt.data.mapper

import com.example.dhbt.data.local.entity.StatisticSummaryEntity
import com.example.dhbt.domain.model.StatisticPeriod
import com.example.dhbt.domain.model.StatisticSummary
import com.example.dhbt.domain.model.TaskPriority
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

class StatisticSummaryMapper @Inject constructor() {

    fun mapFromEntity(entity: StatisticSummaryEntity): StatisticSummary {
        val tasksCategorySummary = entity.tasksCategorySummary?.let { json ->
            Json.decodeFromString<Map<String, Int>>(json)
        }

        val tasksPrioritySummary = entity.tasksPrioritySummary?.let { json ->
            val map = Json.decodeFromString<Map<Int, Int>>(json)
            map.mapKeys { TaskPriority.fromInt(it.key) }
        }

        val habitsSuccessRate = entity.habitsSuccessRate?.let { json ->
            Json.decodeFromString<Map<String, Float>>(json)
        }

        val pomodoroDistribution = entity.pomodoroDistribution?.let { json ->
            Json.decodeFromString<Map<String, Int>>(json)
        }

        return StatisticSummary(
            id = entity.summaryId,
            date = entity.date,
            periodType = StatisticPeriod.fromInt(entity.periodType),
            taskCompletionPercentage = entity.taskCompletionPercentage,
            habitCompletionPercentage = entity.habitCompletionPercentage,
            totalPomodoroMinutes = entity.totalPomodoroMinutes,
            productiveStreak = entity.productiveStreak,
            tasksCategorySummary = tasksCategorySummary,
            tasksPrioritySummary = tasksPrioritySummary,
            habitsSuccessRate = habitsSuccessRate,
            pomodoroDistribution = pomodoroDistribution
        )
    }

    fun mapToEntity(domain: StatisticSummary): StatisticSummaryEntity {
        val tasksCategorySummaryJson = domain.tasksCategorySummary?.let { map ->
            Json.encodeToString(map)
        }

        val tasksPrioritySummaryJson = domain.tasksPrioritySummary?.let { map ->
            val intMap = map.mapKeys { it.key.value }
            Json.encodeToString(intMap)
        }

        val habitsSuccessRateJson = domain.habitsSuccessRate?.let { map ->
            Json.encodeToString(map)
        }

        val pomodoroDistributionJson = domain.pomodoroDistribution?.let { map ->
            Json.encodeToString(map)
        }

        return StatisticSummaryEntity(
            summaryId = domain.id,
            date = domain.date,
            periodType = domain.periodType.value,
            taskCompletionPercentage = domain.taskCompletionPercentage,
            habitCompletionPercentage = domain.habitCompletionPercentage,
            totalPomodoroMinutes = domain.totalPomodoroMinutes,
            productiveStreak = domain.productiveStreak,
            tasksCategorySummary = tasksCategorySummaryJson,
            tasksPrioritySummary = tasksPrioritySummaryJson,
            habitsSuccessRate = habitsSuccessRateJson,
            pomodoroDistribution = pomodoroDistributionJson
        )
    }
}