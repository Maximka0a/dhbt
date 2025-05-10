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
                emptyList<Int>() // Возвращаем пустой список вместо null
            }
        } ?: emptyList() // Если daysOfWeek равен null, возвращаем пустой список

        return Notification(
            id = entity.notificationId,
            targetId = entity.targetId,
            targetType = NotificationTarget.fromInt(entity.targetType),
            time = entity.time ?: "09:00", // Используем дефолтное значение, если null
            daysOfWeek = daysOfWeek, // Теперь точно не null
            isEnabled = entity.isEnabled,
            message = entity.message,
            workId = entity.workId,
            repeatInterval = entity.repeatInterval,
            scheduledDate = entity.scheduledDate // Новое поле с датой
        )
    }

    fun mapToEntity(domain: Notification): NotificationEntity {
        val daysOfWeekJson = if (domain.daysOfWeek.isNotEmpty()) {
            Json.encodeToString(domain.daysOfWeek)
        } else {
            null
        }

        val entity = NotificationEntity(
            notificationId = domain.id,
            targetId = domain.targetId,
            targetType = domain.targetType.value,
            time = domain.time,
            daysOfWeek = daysOfWeekJson,
            isEnabled = domain.isEnabled,
            message = domain.message,
            workId = domain.workId,
            repeatInterval = domain.repeatInterval,
            scheduledDate = domain.scheduledDate // Добавляем дату
        )
        Log.d("NotificationMapper", "Mapped to entity: ${entity.notificationId}, workId=${entity.workId}")
        return entity
    }
}