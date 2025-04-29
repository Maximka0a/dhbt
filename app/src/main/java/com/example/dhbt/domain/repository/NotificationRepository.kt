package com.example.dhbt.domain.repository

import com.example.dhbt.domain.model.Notification
import com.example.dhbt.domain.model.NotificationTarget
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    fun getAllNotifications(): Flow<List<Notification>>

    fun getNotificationsForTarget(targetId: String, targetType: NotificationTarget): Flow<List<Notification>>

    fun getNotificationsByType(targetType: NotificationTarget): Flow<List<Notification>>
    suspend fun scheduleExistingNotification(notification: Notification)
    suspend fun getNotificationById(notificationId: String): Notification?

    suspend fun addNotification(notification: Notification): String

    suspend fun updateNotification(notification: Notification)

    suspend fun deleteNotification(notificationId: String)

    suspend fun toggleNotificationStatus(notificationId: String, isEnabled: Boolean)

    suspend fun deleteNotificationsForTarget(targetId: String, targetType: NotificationTarget)

    suspend fun scheduleNotification(notification: Notification)

    suspend fun scheduleTaskNotification(taskId: String, dueDate: Long, dueTime: String?)

    suspend fun scheduleHabitNotification(habitId: String, time: String, daysOfWeek: List<Int>)

    suspend fun scheduleSystemDailyNotifications()
    suspend fun rescheduleAllNotifications()

    suspend fun cancelScheduledNotification(notificationId: String)
}