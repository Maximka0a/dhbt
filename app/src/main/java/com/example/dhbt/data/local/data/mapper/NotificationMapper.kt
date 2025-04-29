package com.example.dhbt.data.mapper

import android.util.Log
import com.example.dhbt.data.local.entity.NotificationEntity
import com.example.dhbt.domain.model.Notification
import com.example.dhbt.domain.model.NotificationTarget
import kotlinx.serialization.json.Json
import javax.inject.Inject

class NotificationMapper @Inject constructor() {

    fun mapFromEntity(entity: NotificationEntity): Notification {
        val daysOfWeek = entity.daysOfWeek?.let { daysJson ->
            try {
                Json.decodeFromString<List<Int>>(daysJson)
            } catch (e: Exception) {
                null
            }
        }

        return Notification(
            id = entity.notificationId,
            targetId = entity.targetId,
            targetType = NotificationTarget.fromInt(entity.targetType),
            time = entity.time,
            daysOfWeek = daysOfWeek,
            isEnabled = entity.isEnabled,
            message = entity.message,
            workId = entity.workId,
            repeatInterval = entity.repeatInterval
        )
    }

    fun mapToEntity(domain: Notification): NotificationEntity {
        val entity = NotificationEntity(
            notificationId = domain.id,
            targetId = domain.targetId,
            targetType = domain.targetType.value,
            time = domain.time,
            daysOfWeek = domain.daysOfWeek?.let { Json.encodeToString(it) },
            isEnabled = domain.isEnabled,
            message = domain.message,
            workId = domain.workId,
            repeatInterval = domain.repeatInterval
        )
        Log.d("NotificationMapper", "Mapped to entity: ${entity.notificationId}, workId=${entity.workId}")
        return entity
    }
}