package com.example.dhbt.data.mapper

import com.example.dhbt.data.local.entity.HabitFrequencyEntity
import com.example.dhbt.domain.model.FrequencyType
import com.example.dhbt.domain.model.HabitFrequency
import com.example.dhbt.domain.model.PeriodType
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

class HabitFrequencyMapper @Inject constructor() {

    fun mapFromEntity(entity: HabitFrequencyEntity): HabitFrequency {
        val daysOfWeek = entity.daysOfWeek?.let { daysJson ->
            try {
                // First try normal JSON parsing
                Json.decodeFromString<List<Int>>(daysJson)
            } catch (e: Exception) {
                // If that fails, try parsing as comma-separated values
                try {
                    daysJson.split(",").map { it.trim().toInt() }
                } catch (e2: Exception) {
                    // If all parsing fails, return null
                    null
                }
            }
        }

        return HabitFrequency(
            id = entity.frequencyId,
            habitId = entity.habitId,
            type = FrequencyType.fromInt(entity.frequencyType),
            daysOfWeek = daysOfWeek,
            timesPerPeriod = entity.timesPerPeriod,
            periodType = entity.periodType?.let { PeriodType.fromInt(it) }
        )
    }

    fun mapToEntity(domain: HabitFrequency): HabitFrequencyEntity {
        val daysOfWeekJson = domain.daysOfWeek?.let { days ->
            // Always ensure proper JSON array format
            Json.encodeToString(days)
        }

        return HabitFrequencyEntity(
            frequencyId = domain.id,
            habitId = domain.habitId,
            frequencyType = domain.type.value,
            daysOfWeek = daysOfWeekJson,
            timesPerPeriod = domain.timesPerPeriod,
            periodType = domain.periodType?.value
        )
    }
}