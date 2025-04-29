package com.example.dhbt.data.repository

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.dhbt.data.local.dao.NotificationDao
import com.example.dhbt.data.local.dao.TaskDao
import com.example.dhbt.data.local.datastore.DHbtDataStore
import com.example.dhbt.data.mapper.NotificationMapper
import com.example.dhbt.domain.model.Notification
import com.example.dhbt.domain.model.NotificationTarget
import com.example.dhbt.domain.repository.NotificationRepository
import com.example.dhbt.utils.notification.NotificationWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
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

    override suspend fun rescheduleAllNotifications() {
        workManager.cancelAllWorkByTag("notification")
        val notifications = notificationDao.getAllNotifications().first()

        notifications.forEach { entity ->
            if (entity.isEnabled) {
                val notification = notificationMapper.mapFromEntity(entity)
                notificationDao.updateWorkId(notification.id, null)
                scheduleNotification(notification)
            }
        }
    }

    override suspend fun addNotification(notification: Notification): String {
        try {
            val entity = notificationMapper.mapToEntity(notification)
            notificationDao.insertNotification(entity)

            if (notification.isEnabled) {
                val savedEntity = notificationDao.getNotificationById(notification.id)
                if (savedEntity != null) {
                    val savedNotification = notificationMapper.mapFromEntity(savedEntity)
                    scheduleNotification(savedNotification)
                }
            }

            return notification.id
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun updateNotification(notification: Notification) {
        val entity = notificationMapper.mapToEntity(notification)
        notificationDao.updateNotification(entity)

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

    override suspend fun scheduleExistingNotification(notification: Notification) {
        when (notification.targetType) {
            NotificationTarget.TASK -> scheduleTaskNotificationInternal(notification)
            NotificationTarget.HABIT -> {} // scheduleHabitNotificationInternal(notification)
            NotificationTarget.SYSTEM -> scheduleSystemNotificationInternal(notification)
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
            NotificationTarget.HABIT -> {} // scheduleHabitNotificationInternal(notification)
            NotificationTarget.SYSTEM -> scheduleSystemNotificationInternal(notification)
        }
    }

    override suspend fun scheduleTaskNotification(taskId: String, dueDate: Long, dueTime: String?) {
        val taskEntity = taskDao.getTaskById(taskId)
        val taskTitle = taskEntity?.title ?: "Задача"

        val notification = Notification(
            targetId = taskId,
            targetType = NotificationTarget.TASK,
            time = dueTime ?: "09:00",
            message = "Задача: $taskTitle",
            isEnabled = true
        )

        val entity = notificationMapper.mapToEntity(notification)
        notificationDao.insertNotification(entity)

        val data = Data.Builder()
            .putString("notificationId", notification.id)
            .putString("targetId", notification.targetId)
            .putInt("targetType", notification.targetType.value)
            .putString("message", notification.message ?: "Задача: $taskTitle")
            .build()

        val delay = calculateDelayForTask(dueDate, dueTime, System.currentTimeMillis())
        val notificationTag = "notification_task_${notification.id}"

        val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInputData(data)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .addTag("notification")
            .addTag("task_notification")
            .addTag(notificationTag)
            .build()

        try {
            workManager.enqueueUniqueWork(
                notificationTag,
                ExistingWorkPolicy.REPLACE,
                workRequest
            )

            val workId = workRequest.id.toString()
            notificationDao.updateWorkId(notification.id, workId)
        } catch (e: Exception) {
            // Ошибки логируются, но не прерывают выполнение
        }
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

    private suspend fun scheduleTaskNotificationInternal(notification: Notification) {
        try {
            val taskEntity = taskDao.getTaskById(notification.targetId)
            val taskTitle = taskEntity?.title ?: "Задача"
            val taskDueDate = taskEntity?.dueDate
            val taskDueTime = taskEntity?.dueTime

            val data = Data.Builder()
                .putString("notificationId", notification.id)
                .putString("targetId", notification.targetId)
                .putInt("targetType", notification.targetType.value)
                .putString("message", notification.message ?: "Задача: $taskTitle")
                .build()

            val dueTime = taskDueTime ?: notification.time
            val dueDate = taskDueDate
                ?: (System.currentTimeMillis() + 24 * 60 * 60 * 1000) // По умолчанию +1 день
            val delay = calculateDelayForTask(dueDate, dueTime, System.currentTimeMillis())
            val notificationTag = "notification_task_${notification.id}"

            val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
                .setInputData(data)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .addTag("notification")
                .addTag("task_notification")
                .addTag(notificationTag)
                .build()

            workManager.enqueueUniqueWork(
                notificationTag,
                ExistingWorkPolicy.REPLACE,
                workRequest
            )

            val workId = workRequest.id.toString()
            notificationDao.updateWorkId(notification.id, workId)
        } catch (e: Exception) {
            // Ошибки логируются, но не прерывают выполнение
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
            PeriodicWorkRequestBuilder<NotificationWorker>(
                notification.repeatInterval.toLong(), TimeUnit.MINUTES
            )
                .setInputData(data)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .addTag("notification")
                .addTag("system_notification")
                .build()
        } else {
            OneTimeWorkRequestBuilder<NotificationWorker>()
                .setInputData(data)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .addTag("notification")
                .addTag("system_notification")
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

        notificationDao.updateWorkId(notification.id, workRequest.id.toString())
    }

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

    private fun calculateDelayForTask(dueDate: Long?, dueTimeString: String?, currentTime: Long): Long {
        if (dueDate == null) {
            return 60000L // 1 минута по умолчанию
        }

        try {
            val dueDateTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(dueDate),
                ZoneId.systemDefault()
            )

            val timeToSet = if (!dueTimeString.isNullOrEmpty()) {
                try {
                    val time = LocalTime.parse(dueTimeString, DateTimeFormatter.ofPattern("HH:mm"))
                    dueDateTime
                        .withHour(time.hour)
                        .withMinute(time.minute)
                        .withSecond(0)
                } catch (e: Exception) {
                    dueDateTime.withHour(9).withMinute(0).withSecond(0)
                }
            } else {
                dueDateTime.withHour(9).withMinute(0).withSecond(0)
            }

            val dueDateMillis = timeToSet.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            var delay = dueDateMillis - currentTime

            // Минимальная задержка в 1 минуту
            if (delay <= 0) {
                delay = 60000L
            }

            // Максимальная задержка в 7 дней
            if (delay > 7 * 24 * 60 * 60 * 1000L) {
                delay = 7 * 24 * 60 * 60 * 1000L
            }

            return delay
        } catch (e: Exception) {
            return 60000L // 1 минута при ошибке
        }
    }
}