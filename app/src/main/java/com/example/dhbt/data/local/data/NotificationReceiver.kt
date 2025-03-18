package com.example.dhbt.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.dhbt.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import org.threeten.bp.LocalTime
import org.threeten.bp.format.DateTimeFormatter
import javax.inject.Inject

@AndroidEntryPoint
class NotificationReceiver : BroadcastReceiver() {

    @Inject
    lateinit var userPreferencesRepository: com.example.dhbt.domain.repository.UserPreferencesRepository

    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getStringExtra("notificationId") ?: return
        val targetId = intent.getStringExtra("targetId") ?: return
        val targetType = intent.getIntExtra("targetType", 0)
        val message = intent.getStringExtra("message") ?: "Напоминание"

        // Проверяем, не находимся ли мы в тихом режиме
        if (isQuietHoursActive()) {
            return
        }

        // Создаём канал уведомлений (для Android 8.0+)
        createNotificationChannel(context)

        // Создаём и отображаем уведомление
        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getTitle(targetType))
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId.hashCode(), notificationBuilder.build())
    }

    private suspend fun isQuietHoursActive(): Boolean {
        val preferences = userPreferencesRepository.getUserPreferences().first()

        if (!preferences.quietHoursEnabled || preferences.quietHoursStart == null || preferences.quietHoursEnd == null) {
            return false
        }

        val currentTime = LocalTime.now()
        val startTime = LocalTime.parse(preferences.quietHoursStart, DateTimeFormatter.ofPattern("HH:mm"))
        val endTime = LocalTime.parse(preferences.quietHoursEnd, DateTimeFormatter.ofPattern("HH:mm"))

        return if (startTime.isBefore(endTime)) {
            currentTime.isAfter(startTime) && currentTime.isBefore(endTime)
        } else {
            // Обрабатываем случай, когда тихие часы переходят через полночь
            currentTime.isAfter(startTime) || currentTime.isBefore(endTime)
        }
    }

    private fun getTitle(targetType: Int): String {
        return when (targetType) {
            0 -> "Задача"
            1 -> "Привычка"
            else -> "DHbt"
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "DHbt Notifications"
            val descriptionText = "Уведомления для задач и привычек"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "dhbt_notifications"
    }
}