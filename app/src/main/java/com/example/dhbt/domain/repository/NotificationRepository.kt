package com.example.dhbt.domain.repository

import com.example.dhbt.domain.model.Notification
import com.example.dhbt.domain.model.NotificationTarget
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    fun getAllNotifications(): Flow<List<Notification>>
    fun getNotificationsForTarget(targetId: String, targetType: NotificationTarget): Flow<List<Notification>>
    suspend fun addNotification(notification: Notification): String
    suspend fun updateNotification(notification: Notification)
    suspend fun deleteNotification(notificationId: String)
    suspend fun toggleNotification(notificationId: String, isEnabled: Boolean)
    suspend fun deleteNotificationsForTarget(targetId: String, targetType: NotificationTarget)
    suspend fun scheduleNotification(notification: Notification)
    suspend fun cancelScheduledNotification(notificationId: String)
}