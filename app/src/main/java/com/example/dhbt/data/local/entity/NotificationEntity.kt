package com.example.dhbt.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import java.util.UUID

@Entity(
    tableName = "notifications",
    indices = [Index("targetId")]
)
data class NotificationEntity(
    @PrimaryKey
    val notificationId: String = UUID.randomUUID().toString(),
    val targetId: String, // ID задачи или привычки
    val targetType: Int, // 0-задача, 1-привычка, 2-системное
    val time: String, // Время уведомления в формате "HH:MM"
    val daysOfWeek: String? = null, // JSON-массив с днями недели [1-7]
    val isEnabled: Boolean = true,
    val message: String? = null, // Пользовательский текст уведомления
    val workId: String? = null, // ID задачи WorkManager
    val repeatInterval: Int? = null // Интервал повтора в минутах (для системных)
)