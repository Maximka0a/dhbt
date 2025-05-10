package com.example.dhbt.utils.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.dhbt.domain.repository.TaskRepository
import com.example.dhbt.utils.notification.NotificationManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import javax.inject.Inject

/**
 * BroadcastReceiver для обработки действий с задачами из уведомлений
 */
@AndroidEntryPoint
class TaskActionReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_COMPLETE_TASK = "com.example.dhbt.ACTION_COMPLETE_TASK"
        const val ACTION_POSTPONE_TASK = "com.example.dhbt.ACTION_POSTPONE_TASK"
    }

    @Inject
    lateinit var taskRepository: TaskRepository

    @Inject
    lateinit var notificationManager: NotificationManager

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra("taskId") ?: return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ACTION_COMPLETE_TASK -> handleCompleteTask(taskId)
                    ACTION_POSTPONE_TASK -> handlePostponeTask(taskId)
                }
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при обработке действия для задачи $taskId")
            }
        }
    }

    private suspend fun handleCompleteTask(taskId: String) {
        Timber.d("Выполнение задачи $taskId из уведомления")
        val task = taskRepository.getTaskById(taskId) ?: return
        taskRepository.completeTask(taskId, System.currentTimeMillis())

        // Отменяем все уведомления для задачи
        notificationManager.cancelTaskNotifications(taskId)
    }

    private suspend fun handlePostponeTask(taskId: String) {
        Timber.d("Откладывание задачи $taskId из уведомления")
        val task = taskRepository.getTaskById(taskId) ?: return

        // Откладываем задачу на следующий день
        val currentMillis = System.currentTimeMillis()
        val tomorrow = LocalDate.now().plusDays(1)
        val tomorrowMillis = tomorrow.atStartOfDay().toEpochSecond(java.time.ZoneOffset.UTC) * 1000

        // Обновляем задачу
        val updatedTask = task.copy(dueDate = tomorrowMillis)
        taskRepository.updateTask(updatedTask)

        // Отменяем текущие уведомления
        notificationManager.cancelTaskNotifications(taskId)

        // Создаем новое уведомление
        notificationManager.scheduleTaskNotification(
            taskId = taskId,
            taskTitle = task.title,
            dueDate = tomorrowMillis,
            dueTime = task.dueTime,
            description = task.description
        )
    }
}