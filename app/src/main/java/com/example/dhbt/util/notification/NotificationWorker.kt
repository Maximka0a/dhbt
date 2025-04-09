package com.example.dhbt.utils.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
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
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@HiltWorker
class NotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val taskDao: TaskDao,
    private val habitDao: HabitDao,
    private val notificationDao: NotificationDao,
    private val dataStore: DHbtDataStore
) : CoroutineWorker(context, params) {

    companion object {
        private const val CHANNEL_ID = "dhbt_notifications"
        private const val CHANNEL_NAME = "Напоминания"
    }

    override suspend fun doWork(): Result {
        val notificationId = inputData.getString("notificationId") ?: return Result.failure()
        val targetId = inputData.getString("targetId") ?: return Result.failure()
        val targetType = inputData.getInt("targetType", 0)
        val message = inputData.getString("message") ?: "Напоминание"
        val dayOfWeek = inputData.getInt("dayOfWeek", -1)

        // Проверка тихих часов
        if (!shouldShowNotification()) {
            return Result.success()
        }

        // Проверяем, существует ли целевой объект
        val shouldShow = when (NotificationTarget.fromInt(targetType)) {
            NotificationTarget.TASK -> {
                val task = taskDao.getTaskById(targetId)
                // Показываем только для активных задач
                task != null && task.status == 0
            }
            NotificationTarget.HABIT -> {
                val habit = habitDao.getHabitById(targetId)
                // Показываем только для активных привычек
                habit != null && habit.status == 0 && (dayOfWeek == -1 || LocalDateTime.now().dayOfWeek.value == dayOfWeek)
            }
            NotificationTarget.SYSTEM -> true // Системные уведомления всегда показываем
        }

        if (shouldShow) {
            showNotification(targetId, targetType, message)
        }

        // Для одноразовых уведомлений задач удаляем из базы после отправки
        if (targetType == NotificationTarget.TASK.value) {
            notificationDao.deleteNotificationById(notificationId)
        }

        return Result.success()
    }

    private suspend fun shouldShowNotification(): Boolean {
        val preferences = dataStore.userPreferences.first()
        val userData = dataStore.userData.first()

        // Проверка тихих часов
        if (preferences.quietHoursEnabled) {
            val now = LocalTime.now()
            val quietStart = preferences.quietHoursStart?.let {
                LocalTime.parse(it, DateTimeFormatter.ofPattern("HH:mm"))
            }
            val quietEnd = preferences.quietHoursEnd?.let {
                LocalTime.parse(it, DateTimeFormatter.ofPattern("HH:mm"))
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
            LocalTime.parse(it, DateTimeFormatter.ofPattern("HH:mm"))
        }
        val sleepTime = userData.sleepTime?.let {
            LocalTime.parse(it, DateTimeFormatter.ofPattern("HH:mm"))
        }

        if (wakeUpTime != null && sleepTime != null) {
            val now = LocalTime.now()

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

    private fun showNotification(targetId: String, targetType: Int, message: String) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

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
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("targetId", targetId)
            putExtra("targetType", targetType)
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Определяем иконку и заголовок в зависимости от типа уведомления
        val icon = R.drawable.ic_notification // Замените на вашу иконку
        val title = when (NotificationTarget.fromInt(targetType)) {
            NotificationTarget.TASK -> "Задача"
            NotificationTarget.HABIT -> "Привычка"
            NotificationTarget.SYSTEM -> "DHbt"
        }

        // Создаем уведомление
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .build()

        // Показываем уведомление
        val uniqueId = System.currentTimeMillis().toInt()
        notificationManager.notify(uniqueId, notification)
    }
}