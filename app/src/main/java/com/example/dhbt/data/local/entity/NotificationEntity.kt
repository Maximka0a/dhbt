package com.example.dhbt.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Сущность для хранения данных уведомления
 */
@Entity(
    tableName = "notifications",
    indices = [
        Index(value = ["targetId", "targetType"], unique = true)
    ]
)
data class NotificationEntity(
    @PrimaryKey
    val notificationId: String = UUID.randomUUID().toString(),
    val targetId: String,
    val targetType: Int,
    val title: String? = null,
    val message: String? = null,
    val time: String? = null,
    val scheduledDate: Long? = null,  // Добавлено поле даты
    val daysOfWeek: String? = null,   // Дни недели для повторяющихся уведомлений
    val repeatInterval: Int? = null,  // Интервал повторения в минутах
    val workId: String? = null,
    val isEnabled: Boolean = true,
    val priority: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)