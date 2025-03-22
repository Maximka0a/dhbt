package com.example.dhbt.data.mapper

import com.example.dhbt.data.local.entity.TaskRecurrenceEntity
import com.example.dhbt.domain.model.RecurrenceType
import com.example.dhbt.domain.model.TaskRecurrence
import kotlinx.serialization.json.Json
import javax.inject.Inject

class TaskRecurrenceMapper @Inject constructor() {

    fun mapToEntity(domainModel: TaskRecurrence): TaskRecurrenceEntity {
        return TaskRecurrenceEntity(
            recurrenceId = domainModel.id,
            taskId = domainModel.taskId,
            recurrenceType = domainModel.type.value,
            daysOfWeek = domainModel.daysOfWeek?.joinToString(","),
            monthDay = domainModel.monthDay,
            customInterval = domainModel.customInterval,
            startDate = domainModel.startDate,
            endDate = domainModel.endDate
        )
    }

    fun mapFromEntity(entity: TaskRecurrenceEntity): TaskRecurrence {
        val recurrenceType = when (entity.recurrenceType) {
            0 -> RecurrenceType.DAILY
            1 -> RecurrenceType.WEEKLY
            2 -> RecurrenceType.MONTHLY
            3 -> RecurrenceType.CUSTOM
            4 -> RecurrenceType.YEARLY
            else -> RecurrenceType.DAILY // Дефолтное значение
        }

        // Безопасное преобразование строки с днями недели в список
        val daysOfWeek: List<Int>? = entity.daysOfWeek?.let { daysString ->
            if (daysString.isEmpty()) {
                null
            } else {
                try {
                    // Напрямую разбираем строку, а не используем Json.decodeFromString
                    daysString.split(",").map { it.trim().toInt() }
                } catch (e: Exception) {
                    null
                }
            }
        }

        return TaskRecurrence(
            id = entity.recurrenceId,
            taskId = entity.taskId,
            type = recurrenceType,
            daysOfWeek = daysOfWeek,
            monthDay = entity.monthDay,
            customInterval = entity.customInterval,
            startDate = entity.startDate,
            endDate = entity.endDate
        )
    }
}