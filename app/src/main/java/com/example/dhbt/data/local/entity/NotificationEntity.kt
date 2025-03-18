package com.example.dhbt.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey
    val notificationId: String = UUID.randomUUID().toString(),
    val targetId: String, // Идентификатор цели уведомления (задачи или привычки)
    val targetType: Int, // 0-задача, 1-привычка
    val time: String, // Время уведомления в формате "HH:MM"
    val daysOfWeek: String? = null, // JSON-массив с днями недели для уведомления
    val isEnabled: Boolean = true,
    val message: String? = null, // Пользовательский текст уведомления
    val repeatInterval: Int? = null // Интервал повтора уведомления в минутах
)