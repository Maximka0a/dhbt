package com.example.dhbt.utils.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.dhbt.R
import com.example.dhbt.data.local.datastore.DHbtDataStore
import com.example.dhbt.domain.model.NotificationTarget
import com.example.dhbt.presentation.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DHbtDataStore
) {
    companion object {
        private const val CHANNEL_ID = "dhbt_notifications"
        private const val CHANNEL_NAME = "Напоминания"
    }

    suspend fun showNotification(
        targetId: String,
        targetType: Int,
        title: String,
        message: String
    ) {
        // Проверяем настройки пользователя перед отправкой
        if (!shouldShowNotification()) {
            return
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Создаем канал для уведомлений (требуется для Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        // Создаем интент для открытия приложения при нажатии
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("targetId", targetId)
            putExtra("targetType", targetType)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Определяем иконку в зависимости от типа уведомления
        val icon = R.drawable.ic_notification // Замените на вашу иконку

        // Создаем уведомление
        val userPreferences = dataStore.userPreferences.first()
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        // Добавляем звук в зависимости от настроек
        if (userPreferences.defaultSoundEnabled) {
            notification.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
        }

        // Добавляем вибрацию в зависимости от настроек
        if (userPreferences.defaultVibrationEnabled) {
            notification.setVibrate(longArrayOf(0, 250, 250, 250))
        }

        // Показываем уведомление
        val uniqueId = when (NotificationTarget.fromInt(targetType)) {
            NotificationTarget.TASK -> targetId.hashCode()
            NotificationTarget.HABIT -> (targetId + System.currentTimeMillis()).hashCode()
            NotificationTarget.SYSTEM -> targetId.hashCode()
        }

        notificationManager.notify(uniqueId, notification.build())
    }

    private suspend fun shouldShowNotification(): Boolean {
        val preferences = dataStore.userPreferences.first()
        val userData = dataStore.userData.first()

        // Проверка тихих часов
        if (preferences.quietHoursEnabled) {
            val now = java.time.LocalTime.now()
            val quietStart = preferences.quietHoursStart?.let {
                java.time.LocalTime.parse(it, java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
            }
            val quietEnd = preferences.quietHoursEnd?.let {
                java.time.LocalTime.parse(it, java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
            }

            if (quietStart != null && quietEnd != null) {
                if (quietStart.isBefore(quietEnd)) {
                    // Если период тихих часов в пределах одного дня
                    if (now.isAfter(quietStart) && now.isBefore(quietEnd)) {
                        return false
                    }
                } else {
                    // Если период тихих часов переходит на следующий день
                    if (now.isAfter(quietStart) || now.isBefore(quietEnd)) {
                        return false
                    }
                }
            }
        }

        // Проверка периода бодрствования
        val wakeUpTime = userData.wakeUpTime?.let {
            java.time.LocalTime.parse(it, java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
        }
        val sleepTime = userData.sleepTime?.let {
            java.time.LocalTime.parse(it, java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
        }

        if (wakeUpTime != null && sleepTime != null) {
            val now = java.time.LocalTime.now()

            if (wakeUpTime.isBefore(sleepTime)) {
                // Если период бодрствования в пределах одного дня
                if (now.isBefore(wakeUpTime) || now.isAfter(sleepTime)) {
                    return false
                }
            } else {
                // Если период бодрствования переходит на следующий день
                if (now.isAfter(sleepTime) && now.isBefore(wakeUpTime)) {
                    return false
                }
            }
        }

        return true
    }
}