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
        return try {
            val notificationType = inputData.getString("type") ?: return Result.failure()

            when (notificationType) {
                "wakeup" -> sendWakeUpNotification()
                "sleep" -> sendSleepNotification()
                else -> return Result.failure()
            }

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    private suspend fun sendWakeUpNotification() {
        val activeTasks = taskDao.getTasksByStatus(TaskStatus.ACTIVE.value).first()
        val message = createWakeUpMessage(activeTasks.size)

        notificationHandler.showNotification(
            targetId = "system_wakeup",
            targetType = NotificationTarget.SYSTEM.value,
            title = "Доброе утро",
            message = message
        )
    }

    private fun createWakeUpMessage(taskCount: Int): String {
        return if (taskCount > 0) {
            "У вас $taskCount невыполненных задач. Просыпайтесь и выполняйте их!"
        } else {
            "Доброе утро! У вас нет невыполненных задач на сегодня."
        }
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