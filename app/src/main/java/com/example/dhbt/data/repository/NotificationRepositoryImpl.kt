package com.example.dhbt.data.repository

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.*
import com.example.dhbt.data.local.dao.NotificationDao
import com.example.dhbt.data.local.dao.TaskDao
import com.example.dhbt.data.local.datastore.DHbtDataStore
import com.example.dhbt.data.mapper.NotificationMapper
import com.example.dhbt.domain.model.Notification
import com.example.dhbt.domain.model.NotificationTarget
import com.example.dhbt.domain.repository.NotificationRepository
import com.example.dhbt.utils.notification.NotificationReceiver
import com.example.dhbt.utils.notification.NotificationWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class NotificationRepositoryImpl @Inject constructor(
    private val notificationDao: NotificationDao,
    private val taskDao: TaskDao,
    private val notificationMapper: NotificationMapper,
    private val dataStore: DHbtDataStore,
    @ApplicationContext private val context: Context
) : NotificationRepository {

    private val workManager = WorkManager.getInstance(context)

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

    override fun getNotificationsByType(targetType: NotificationTarget): Flow<List<Notification>> {
        return notificationDao.getNotificationsByType(targetType.value).map { entities ->
            entities.map { notificationMapper.mapFromEntity(it) }
        }
    }

    override suspend fun getNotificationById(notificationId: String): Notification? {
        val entity = notificationDao.getNotificationById(notificationId) ?: return null
        return notificationMapper.mapFromEntity(entity)
    }

    override suspend fun addNotification(notification: Notification): String {
        try {
            val entity = notificationMapper.mapToEntity(notification)
            notificationDao.insertNotification(entity)

            if (notification.isEnabled) {
                scheduleNotification(notification)
            }

            return notification.id
        } catch (e: Exception) {
            Log.e("NotificationRepository", "Error adding notification: ${e.message}")
            // Consider whether to rethrow or handle gracefully
            throw e
        }
    }

    override suspend fun updateNotification(notification: Notification) {
        val entity = notificationMapper.mapToEntity(notification)
        notificationDao.updateNotification(entity)

        // Отмена и перепланирование уведомления
        notification.workId?.let {
            workManager.cancelWorkById(UUID.fromString(it))
        }

        if (notification.isEnabled) {
            scheduleNotification(notification)
        }
    }

    override suspend fun deleteNotification(notificationId: String) {
        val notification = getNotificationById(notificationId)
        notification?.workId?.let {
            workManager.cancelWorkById(UUID.fromString(it))
        }

        notificationDao.deleteNotificationById(notificationId)
    }

    override suspend fun toggleNotificationStatus(notificationId: String, isEnabled: Boolean) {
        notificationDao.updateNotificationStatus(notificationId, isEnabled)

        val notification = getNotificationById(notificationId)
        notification?.let {
            if (isEnabled) {
                scheduleNotification(it)
            } else {
                notification.workId?.let { workId ->
                    workManager.cancelWorkById(UUID.fromString(workId))
                }
            }
        }
    }

    override suspend fun deleteNotificationsForTarget(targetId: String, targetType: NotificationTarget) {
        val notifications = notificationDao.getNotificationsForTarget(targetId, targetType.value).first()
        notifications.forEach {
            it.workId?.let { workId ->
                workManager.cancelWorkById(UUID.fromString(workId))
            }
        }

        notificationDao.deleteNotificationsForTarget(targetId, targetType.value)
    }

    override suspend fun scheduleNotification(notification: Notification) {
        when (notification.targetType) {
            NotificationTarget.TASK -> scheduleTaskNotificationInternal(notification)
            NotificationTarget.HABIT -> scheduleHabitNotificationInternal(notification)
            NotificationTarget.SYSTEM -> scheduleSystemNotificationInternal(notification)
        }
    }

    override suspend fun scheduleTaskNotification(taskId: String, dueDate: Long, dueTime: String?) {
        val userPreferences = dataStore.userPreferences.first()
        val reminderMinutes = userPreferences.reminderTimeBeforeTask

        // Try to get the task, but don't exit if not found
        val taskEntity = taskDao.getTaskById(taskId)
        val taskTitle = taskEntity?.title ?: "Задача"

        val notification = Notification(
            targetId = taskId,
            targetType = NotificationTarget.TASK,
            time = dueTime ?: "09:00", // Default time if not specified
            message = "Задача: $taskTitle"
        )

        addNotification(notification)
    }

    override suspend fun scheduleHabitNotification(habitId: String, time: String, daysOfWeek: List<Int>) {
        val notification = Notification(
            targetId = habitId,
            targetType = NotificationTarget.HABIT,
            time = time,
            daysOfWeek = daysOfWeek,
            message = "Напоминание о привычке"
        )

        addNotification(notification)
    }

    override suspend fun scheduleSystemDailyNotifications() {
        val userData = dataStore.userData.first()

        // Утреннее уведомление
        if (!userData.wakeUpTime.isNullOrEmpty()) {
            val wakeUpNotification = Notification(
                targetId = "system_wakeup",
                targetType = NotificationTarget.SYSTEM,
                time = userData.wakeUpTime,
                message = "У вас невыполненные задачи. Просыпайтесь и выполняйте их!",
                repeatInterval = 24 * 60 // Повтор каждые 24 часа
            )
            addNotification(wakeUpNotification)
        }

        // Вечернее уведомление
        if (!userData.sleepTime.isNullOrEmpty()) {
            val sleepNotification = Notification(
                targetId = "system_sleep",
                targetType = NotificationTarget.SYSTEM,
                time = userData.sleepTime,
                message = "Добавьте новые задачи на завтра!",
                repeatInterval = 24 * 60 // Повтор каждые 24 часа
            )
            addNotification(sleepNotification)
        }
    }

    override suspend fun cancelScheduledNotification(notificationId: String) {
        val notification = getNotificationById(notificationId)
        notification?.workId?.let {
            workManager.cancelWorkById(UUID.fromString(it))
        }

        notificationDao.updateWorkId(notificationId, null)
    }

    // Внутренние методы для планирования уведомлений

    private suspend fun scheduleTaskNotificationInternal(notification: Notification) {
        val taskEntity = taskDao.getTaskById(notification.targetId) ?: return
        val dueDate = taskEntity.dueDate ?: return

        val notificationTime = if (taskEntity.dueTime != null) {
            LocalTime.parse(taskEntity.dueTime, DateTimeFormatter.ofPattern("HH:mm"))
        } else {
            LocalTime.parse(notification.time, DateTimeFormatter.ofPattern("HH:mm"))
        }

        val dueDateTime = LocalDate.ofEpochDay(dueDate / (24 * 60 * 60 * 1000))
            .atTime(notificationTime)
            .atZone(ZoneId.systemDefault())

        val currentTime = System.currentTimeMillis()
        val notificationTimeMillis = dueDateTime.toInstant().toEpochMilli()

        // Если время уведомления уже прошло, не планируем
        if (notificationTimeMillis <= currentTime) {
            return
        }

        val delay = notificationTimeMillis - currentTime

        val data = Data.Builder()
            .putString("notificationId", notification.id)
            .putString("targetId", notification.targetId)
            .putInt("targetType", notification.targetType.value)
            .putString("message", notification.message ?: "Задача: ${taskEntity.title}")
            .build()

        val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInputData(data)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()

        workManager.enqueue(workRequest)

        // Сохраняем идентификатор работы
        notificationDao.updateWorkId(notification.id, workRequest.id.toString())
    }

    private suspend fun scheduleHabitNotificationInternal(notification: Notification) {
        val daysOfWeek = notification.daysOfWeek ?: listOf()
        val notificationTime = LocalTime.parse(notification.time, DateTimeFormatter.ofPattern("HH:mm"))

        // Создаем отдельный WorkRequest для каждого дня недели
        for (dayOfWeek in daysOfWeek) {
            val data = Data.Builder()
                .putString("notificationId", notification.id)
                .putString("targetId", notification.targetId)
                .putInt("targetType", notification.targetType.value)
                .putString("message", notification.message ?: "Напоминание о привычке")
                .putInt("dayOfWeek", dayOfWeek)
                .build()

            // Создаем периодический запрос работы для каждого дня недели
            val workRequest = PeriodicWorkRequestBuilder<NotificationWorker>(7, TimeUnit.DAYS)
                .setInputData(data)
                .addTag("habit_${notification.targetId}_$dayOfWeek")
                .setInitialDelay(calculateInitialDelayForDay(dayOfWeek, notificationTime), TimeUnit.MILLISECONDS)
                .build()

            workManager.enqueueUniquePeriodicWork(
                "habit_${notification.id}_$dayOfWeek",
                ExistingPeriodicWorkPolicy.REPLACE,
                workRequest
            )
        }

        // Сохраняем идентификатор работы (первый день как основной)
        if (daysOfWeek.isNotEmpty()) {
            notificationDao.updateWorkId(notification.id, "habit_${notification.id}_${daysOfWeek.first()}")
        }
    }

    private suspend fun scheduleSystemNotificationInternal(notification: Notification) {
        val notificationTime = LocalTime.parse(notification.time, DateTimeFormatter.ofPattern("HH:mm"))

        val initialDelay = calculateInitialDelayForTime(notificationTime)

        val data = Data.Builder()
            .putString("notificationId", notification.id)
            .putString("targetId", notification.targetId)
            .putInt("targetType", notification.targetType.value)
            .putString("message", notification.message ?: "Системное уведомление")
            .build()

        val workRequest = if (notification.repeatInterval != null) {
            // Периодическое уведомление
            PeriodicWorkRequestBuilder<NotificationWorker>(
                notification.repeatInterval.toLong(), TimeUnit.MINUTES
            )
                .setInputData(data)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .build()
        } else {
            // Одноразовое уведомление
            OneTimeWorkRequestBuilder<NotificationWorker>()
                .setInputData(data)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .build()
        }

        if (notification.repeatInterval != null) {
            workManager.enqueueUniquePeriodicWork(
                "system_${notification.id}",
                ExistingPeriodicWorkPolicy.REPLACE,
                workRequest as PeriodicWorkRequest
            )
        } else {
            workManager.enqueue(workRequest)
        }

        // Сохраняем идентификатор работы
        notificationDao.updateWorkId(notification.id, workRequest.id.toString())
    }

    // Вспомогательные методы для расчета задержки

    private fun calculateInitialDelayForDay(dayOfWeek: Int, time: LocalTime): Long {
        val now = LocalDate.now().atTime(LocalTime.now())
        val currentDayOfWeek = now.dayOfWeek.value

        var daysToAdd = dayOfWeek - currentDayOfWeek
        if (daysToAdd < 0) {
            daysToAdd += 7
        } else if (daysToAdd == 0 && now.toLocalTime().isAfter(time)) {
            daysToAdd = 7
        }

        val targetDateTime = now.toLocalDate().plusDays(daysToAdd.toLong()).atTime(time)
        return targetDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() -
                now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    private fun calculateInitialDelayForTime(time: LocalTime): Long {
        val now = LocalDate.now().atTime(LocalTime.now())
        val targetTime = LocalDate.now().atTime(time)

        var targetDateTime = targetTime
        if (now.isAfter(targetTime)) {
            targetDateTime = targetTime.plusDays(1)
        }

        return targetDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() -
                now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}