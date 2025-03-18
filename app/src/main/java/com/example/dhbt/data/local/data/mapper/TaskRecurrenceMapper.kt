package com.example.dhbt.data.mapper

import com.example.dhbt.data.local.entity.TaskRecurrenceEntity
import com.example.dhbt.domain.model.RecurrenceType
import com.example.dhbt.domain.model.TaskRecurrence
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

class TaskRecurrenceMapper @Inject constructor() {

    fun mapFromEntity(entity: TaskRecurrenceEntity): TaskRecurrence {
        val daysOfWeek = entity.daysOfWeek?.let { daysJson ->
            Json.decodeFromString<List<Int>>(daysJson)
        }

        return TaskRecurrence(
            id = entity.recurrenceId,
            taskId = entity.taskId,
            type = RecurrenceType.fromInt(entity.recurrenceType),
            daysOfWeek = daysOfWeek,
            monthDay = entity.monthDay,
            customInterval = entity.customInterval,
            startDate = entity.startDate,
            endDate = entity.endDate
        )
    }

    fun mapToEntity(domain: TaskRecurrence): TaskRecurrenceEntity {
        val daysOfWeekJson = domain.daysOfWeek?.let { days ->
            Json.encodeToString(days)
        }

        return TaskRecurrenceEntity(
            recurrenceId = domain.id,
            taskId = domain.taskId,
            recurrenceType = domain.type.value,
            daysOfWeek = daysOfWeekJson,
            monthDay = domain.monthDay,
            customInterval = domain.customInterval,
            startDate = domain.startDate,
            endDate = domain.endDate
        )
    }
}