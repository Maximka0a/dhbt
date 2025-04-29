package com.example.dhbt.utils.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.dhbt.R
import com.example.dhbt.data.local.dao.HabitDao
import com.example.dhbt.data.local.dao.NotificationDao
import com.example.dhbt.data.local.dao.TaskDao
import com.example.dhbt.data.local.datastore.DHbtDataStore
import com.example.dhbt.domain.model.NotificationTarget
import com.example.dhbt.presentation.MainActivity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDateTime

@HiltWorker
class NotificationWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted private val workerParams: WorkerParameters,
    private val taskDao: TaskDao,
    private val habitDao: HabitDao,
    private val notificationDao: NotificationDao,
    private val dataStore: DHbtDataStore
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val CHANNEL_ID = "dhbt_notifications"
        private const val CHANNEL_NAME = "Напоминания"
    }

    override suspend fun doWork(): Result {
        try {
            val notificationId = inputData.getString("notificationId") ?: return Result.failure()
            val targetId = inputData.getString("targetId") ?: return Result.failure()
            val targetType = inputData.getInt("targetType", 0)
            val message = inputData.getString("message") ?: "Напоминание"
            val dayOfWeek = inputData.getInt("dayOfWeek", -1)

            val shouldShow = shouldShowNotification(targetId, targetType, dayOfWeek)

            if (shouldShow) {
                showNotification(targetId, targetType, message)
            }

            if (targetType == NotificationTarget.TASK.value) {
                notificationDao.deleteNotificationById(notificationId)
            }

            return Result.success()
        } catch (e: Exception) {
            return Result.failure()
        }
    }

    private suspend fun shouldShowNotification(targetId: String, targetType: Int, dayOfWeek: Int): Boolean {
        return when (NotificationTarget.fromInt(targetType)) {
            NotificationTarget.TASK -> {
                val task = taskDao.getTaskById(targetId)
                task != null && task.status == 0
            }
            NotificationTarget.HABIT -> {
                val habit = habitDao.getHabitById(targetId)
                val currentDayOfWeek = LocalDateTime.now().dayOfWeek.value
                habit != null && habit.status == 0 &&
                        (dayOfWeek == -1 || currentDayOfWeek == dayOfWeek)
            }
            NotificationTarget.SYSTEM -> true
        }
    }

    private fun showNotification(targetId: String, targetType: Int, message: String) {
        try {
            val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            createNotificationChannel(notificationManager)

            val pendingIntent = createPendingIntent(targetId, targetType)

            val title = getTitleForNotificationType(targetType)

            val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setVibrate(longArrayOf(0, 250, 250, 250))
                .build()

            val uniqueId = generateUniqueId(targetId, targetType)
            notificationManager.notify(uniqueId, notification)
        } catch (e: Exception) {
            // Сбои при показе уведомлений не должны приводить к сбоям в работе
        }
    }

    private fun createNotificationChannel(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Уведомления приложения DHbt"
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createPendingIntent(targetId: String, targetType: Int): PendingIntent {
        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("targetId", targetId)
            putExtra("targetType", targetType)
        }

        return PendingIntent.getActivity(
            appContext,
            targetId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun getTitleForNotificationType(targetType: Int): String {
        return when (NotificationTarget.fromInt(targetType)) {
            NotificationTarget.TASK -> "Задача"
            NotificationTarget.HABIT -> "Привычка"
            NotificationTarget.SYSTEM -> "DHbt"
        }
    }

    private fun generateUniqueId(targetId: String, targetType: Int): Int {
        return when (NotificationTarget.fromInt(targetType)) {
            NotificationTarget.TASK -> targetId.hashCode()
            NotificationTarget.HABIT -> "$targetId${System.currentTimeMillis()}".hashCode()
            NotificationTarget.SYSTEM -> targetId.hashCode()
        }
    }
}