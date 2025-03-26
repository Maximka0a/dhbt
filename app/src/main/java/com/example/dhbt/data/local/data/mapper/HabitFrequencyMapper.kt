package com.example.dhbt.data.mapper

import com.example.dhbt.data.local.entity.HabitFrequencyEntity
import com.example.dhbt.domain.model.FrequencyType
import com.example.dhbt.domain.model.HabitFrequency
import com.example.dhbt.domain.model.PeriodType
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject

class HabitFrequencyMapper @Inject constructor() {

    fun mapFromEntity(entity: HabitFrequencyEntity): HabitFrequency {
        val daysOfWeek = entity.daysOfWeek?.let { daysJson ->
            try {
                // Пробуем разные варианты парсинга
                try {
                    // Стандартное JSON-парсинг
                    Json.decodeFromString<List<Int>>(daysJson)
                } catch (e: Exception) {
                    Timber.w("Не удалось разобрать JSON дней недели, пробуем CSV: $daysJson")
                    try {
                        // Пробуем парсинг как CSV
                        daysJson.split(",").map { it.trim().toInt() }
                    } catch (e2: Exception) {
                        Timber.e(e2, "Все попытки парсинга дней недели не удались: $daysJson")
                        emptyList() // Возвращаем пустой список вместо null
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Критическая ошибка при обработке дней недели: $daysJson")
                emptyList() // В случае ошибки возвращаем пустой список
            }
        } ?: emptyList()

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
        val daysOfWeekJson = try {
            // Проверяем на null или пустой список
            if (domain.daysOfWeek.isNullOrEmpty()) {
                null
            } else {
                // Безопасная сериализация
                Json.encodeToString(domain.daysOfWeek)
            }
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при сериализации дней недели: ${domain.daysOfWeek}")
            null // Если сериализация не удалась, сохраняем null
        }

        Timber.d("Сериализация дней недели: ${domain.daysOfWeek} -> $daysOfWeekJson")

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