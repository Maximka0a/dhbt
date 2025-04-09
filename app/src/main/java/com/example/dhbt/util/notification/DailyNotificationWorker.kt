package com.example.dhbt.utils.notification

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.dhbt.data.local.dao.TaskDao
import com.example.dhbt.domain.model.NotificationTarget
import com.example.dhbt.domain.model.TaskStatus
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class DailyNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val taskDao: TaskDao,
    private val notificationHandler: NotificationHandler
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val notificationType = inputData.getString("type") ?: return Result.failure()

        try {
            when (notificationType) {
                "wakeup" -> sendWakeUpNotification()
                "sleep" -> sendSleepNotification()
            }
            return Result.success()
        } catch (e: Exception) {
            return Result.failure()
        }
    }

    private suspend fun sendWakeUpNotification() {
        // Подсчитываем количество активных задач
        val activeTasks = taskDao.getTasksByStatus(TaskStatus.ACTIVE.value).first()
        val taskCount = activeTasks.size

        val message = if (taskCount > 0) {
            "У вас $taskCount невыполненных задач. Просыпайтесь и выполняйте их!"
        } else {
            "Доброе утро! У вас нет невыполненных задач на сегодня."
        }

        notificationHandler.showNotification(
            targetId = "system_wakeup",
            targetType = NotificationTarget.SYSTEM.value,
            title = "Доброе утро",
            message = message
        )
    }

    private suspend fun sendSleepNotification() {
        notificationHandler.showNotification(
            targetId = "system_sleep",
            targetType = NotificationTarget.SYSTEM.value,
            title = "Добрый вечер",
            message = "Добавьте новые задачи на завтра!"
        )
    }
}