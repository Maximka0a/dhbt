package com.example.dhbt.data.mapper

import com.example.dhbt.data.local.entity.HabitEntity
import com.example.dhbt.domain.model.Habit
import com.example.dhbt.domain.model.HabitStatus
import com.example.dhbt.domain.model.HabitType
import javax.inject.Inject

class HabitMapper @Inject constructor() {

    fun mapFromEntity(entity: HabitEntity): Habit {
        return Habit(
            id = entity.habitId,
            title = entity.title,
            description = entity.description,
            iconEmoji = entity.iconEmoji,
            color = entity.color,
            creationDate = entity.creationDate,
            type = HabitType.fromInt(entity.habitType),
            targetValue = entity.targetValue,
            unitOfMeasurement = entity.unitOfMeasurement,
            targetStreak = entity.targetStreak,
            currentStreak = entity.currentStreak,
            bestStreak = entity.bestStreak,
            status = HabitStatus.fromInt(entity.status),
            pausedDate = entity.pausedDate,
            categoryId = entity.categoryId,
            frequency = null // Загружается отдельно
        )
    }

    fun mapToEntity(domain: Habit): HabitEntity {
        return HabitEntity(
            habitId = domain.id,
            title = domain.title,
            description = domain.description,
            iconEmoji = domain.iconEmoji,
            color = domain.color,
            creationDate = domain.creationDate,
            habitType = domain.type.value,
            targetValue = domain.targetValue,
            unitOfMeasurement = domain.unitOfMeasurement,
            targetStreak = domain.targetStreak,
            currentStreak = domain.currentStreak,
            bestStreak = domain.bestStreak,
            status = domain.status.value,
            pausedDate = domain.pausedDate,
            categoryId = domain.categoryId
        )
    }
}