package com.example.dhbt.data.repository

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.dhbt.data.NotificationReceiver
import com.example.dhbt.data.local.dao.NotificationDao
import com.example.dhbt.data.mapper.NotificationMapper
import com.example.dhbt.domain.model.Notification
import com.example.dhbt.domain.model.NotificationTarget
import com.example.dhbt.domain.repository.NotificationRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import javax.inject.Inject

class NotificationRepositoryImpl @Inject constructor(
    private val notificationDao: NotificationDao,
    private val notificationMapper: NotificationMapper,
    @ApplicationContext private val context: Context
) : NotificationRepository {

    override fun getAllNotifications(): Flow<List<Notification>> {
        return notificationDao.getAllNotifications().map { entities ->
            entities.map { notificationMapper.mapFromEntity(it) }
        }
    }

    override fun getNotificationsForTarget(targetId: String, targetType: NotificationTarget): Flow<List<Notification>> {
        return notificationDao.getNotificationsForTarget(targetId, targetType.value).map { entities ->
            entities.map { notificationMapper.mapFromEntity(it) }
        }
    }

    override suspend fun addNotification(notification: Notification): String {
        val entity = notificationMapper.mapToEntity(notification)
        notificationDao.insertNotification(entity)

        if (notification.isEnabled) {
            scheduleNotification(notification)
        }

        return notification.id
    }

    override suspend fun updateNotification(notification: Notification) {
        val entity = notificationMapper.mapToEntity(notification)
        notificationDao.updateNotification(entity)

        // Отменяем и перепланируем уведомление
        cancelScheduledNotification(notification.id)
        if (notification.isEnabled) {
            scheduleNotification(notification)
        }
    }

    override suspend fun deleteNotification(notificationId: String) {
        cancelScheduledNotification(notificationId)
        notificationDao.deleteNotificationById(notificationId)
    }

    override suspend fun toggleNotification(notificationId: String, isEnabled: Boolean) {
        notificationDao.updateNotificationStatus(notificationId, isEnabled)

        val notification = notificationDao.getAllNotifications().first()
            .find { it.notificationId == notificationId }
            ?.let { notificationMapper.mapFromEntity(it) }

        notification?.let {
            if (isEnabled) {
                scheduleNotification(it)
            } else {
                cancelScheduledNotification(notificationId)
            }
        }
    }

    override suspend fun deleteNotificationsForTarget(targetId: String, targetType: NotificationTarget) {
        // Отменяем запланированные уведомления
        val notifications = notificationDao.getNotificationsForTarget(targetId, targetType.value).first()
        notifications.forEach {
            cancelScheduledNotification(it.notificationId)
        }

        notificationDao.deleteNotificationsForTarget(targetId, targetType.value)
    }

    override suspend fun scheduleNotification(notification: Notification) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Создаем Intent для BroadcastReceiver
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("notificationId", notification.id)
            putExtra("targetId", notification.targetId)
            putExtra("targetType", notification.targetType.value)
            putExtra("message", notification.message)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notification.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Получаем время для уведомления
        val notificationTime = LocalTime.parse(notification.time, DateTimeFormatter.ofPattern("HH:mm"))
        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, notificationTime.hour)
            set(Calendar.MINUTE, notificationTime.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // Если время уже прошло, ставим на завтра
            if (timeInMillis < System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        // Устанавливаем сигнал
        if (notification.repeatInterval != null) {
            // Повторяющееся уведомление
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                notification.repeatInterval * 60 * 1000L, // переводим минуты в миллисекунды
                pendingIntent
            )
        } else {
            // Одноразовое уведомление
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    override suspend fun cancelScheduledNotification(notificationId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }
}