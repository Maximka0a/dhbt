package com.example.dhbt.utils.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.dhbt.data.local.dao.TaskDao
import com.example.dhbt.domain.model.TaskStatus
import com.example.dhbt.utils.receivers.TaskActionReceiver
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Worker для показа уведомлений о задачах
 */
@HiltWorker
class TaskNotificationWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val taskDao: TaskDao,
    private val notificationManager: NotificationManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        try {
            val taskId = inputData.getString("taskId") ?: return Result.failure()
            val title = inputData.getString("title") ?: "Задача"
            val message = inputData.getString("message") ?: "Напоминание о задаче"
            val priority = inputData.getInt("priority", 0)
            val scheduledTime = inputData.getLong("scheduledTime", 0)

            // Проверяем, существует ли еще задача и не выполнена ли она
            val task = taskDao.getTaskById(taskId)
            if (task == null || task.status != TaskStatus.ACTIVE.value) {
                Timber.d("Задача $taskId не существует или уже выполнена, уведомление не показано")
                return Result.success()
            }

            // Проверяем, не показываем ли уведомление слишком поздно
            val now = Instant.now()
            val scheduledInstant = Instant.ofEpochMilli(scheduledTime)

            // Если прошло более 1 часа с запланированного времени, не показываем
            if (ChronoUnit.HOURS.between(scheduledInstant, now) > 1) {
                Timber.d("Уведомление о задаче $taskId пропущено (прошло более 1 часа)")
                return Result.success()
            }

            // Создаем действия для уведомления
            val completeIntent = Intent(context, TaskActionReceiver::class.java).apply {
                action = TaskActionReceiver.ACTION_COMPLETE_TASK
                putExtra("taskId", taskId)
            }
            val completePendingIntent = PendingIntent.getBroadcast(
                context,
                taskId.hashCode() + 1,
                completeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val postponeIntent = Intent(context, TaskActionReceiver::class.java).apply {
                action = TaskActionReceiver.ACTION_POSTPONE_TASK
                putExtra("taskId", taskId)
            }
            val postponePendingIntent = PendingIntent.getBroadcast(
                context,
                taskId.hashCode() + 2,
                postponeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Формируем список действий
            val actions = listOf(
                NotificationManager.NotificationAction(
                    android.R.drawable.ic_menu_add, // Иконка выполнения/добавления
                    "Выполнить",
                    completePendingIntent
                ),
                NotificationManager.NotificationAction(
                    android.R.drawable.ic_menu_recent_history, // Иконка отложить/история
                    "Отложить",
                    postponePendingIntent
                )
            )

            // Показываем уведомление
            notificationManager.showTaskNotification(
                taskId = taskId,
                title = title,
                message = message,
                priority = priority,
                actions = actions
            )

            return Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при показе уведомления о задаче")
            return Result.failure()
        }
    }
}