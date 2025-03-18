package com.example.dhbt.data.mapper

import com.example.dhbt.data.local.entity.NotificationEntity
import com.example.dhbt.domain.model.Notification
import com.example.dhbt.domain.model.NotificationTarget
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

class NotificationMapper @Inject constructor() {

    fun mapFromEntity(entity: NotificationEntity): Notification {
        val daysOfWeek = entity.daysOfWeek?.let { daysJson ->
            Json.decodeFromString<List<Int>>(daysJson)
        }

        return Notification(
            id = entity.notificationId,
            targetId = entity.targetId,
            targetType = NotificationTarget.fromInt(entity.targetType),
            time = entity.time,
            daysOfWeek = daysOfWeek,
            isEnabled = entity.isEnabled,
            message = entity.message,
            repeatInterval = entity.repeatInterval
        )
    }

    fun mapToEntity(domain: Notification): NotificationEntity {
        val daysOfWeekJson = domain.daysOfWeek?.let { days ->
            Json.encodeToString(days)
        }

        return NotificationEntity(
            notificationId = domain.id,
            targetId = domain.targetId,
            targetType = domain.targetType.value,
            time = domain.time,
            daysOfWeek = daysOfWeekJson,
            isEnabled = domain.isEnabled,
            message = domain.message,
            repeatInterval = domain.repeatInterval
        )
    }
}