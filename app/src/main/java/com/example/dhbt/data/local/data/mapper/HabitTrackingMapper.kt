package com.example.dhbt.data.mapper

import com.example.dhbt.data.local.entity.HabitTrackingEntity
import com.example.dhbt.domain.model.HabitTracking
import javax.inject.Inject

class HabitTrackingMapper @Inject constructor() {

    fun mapFromEntity(entity: HabitTrackingEntity): HabitTracking {
        return HabitTracking(
            id = entity.trackingId,
            habitId = entity.habitId,
            date = entity.date,
            isCompleted = entity.isCompleted,
            value = entity.value,
            duration = entity.duration,
            notes = entity.notes
        )
    }

    fun mapToEntity(domain: HabitTracking): HabitTrackingEntity {
        return HabitTrackingEntity(
            trackingId = domain.id,
            habitId = domain.habitId,
            date = domain.date,
            isCompleted = domain.isCompleted,
            value = domain.value,
            duration = domain.duration,
            notes = domain.notes
        )
    }
}