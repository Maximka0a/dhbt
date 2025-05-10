package com.example.dhbt.data.repository

import android.content.Context
import androidx.work.WorkManager
import com.example.dhbt.data.local.dao.NotificationDao
import com.example.dhbt.data.local.dao.TaskDao
import com.example.dhbt.data.local.datastore.DHbtDataStore
import com.example.dhbt.data.mapper.NotificationMapper
import com.example.dhbt.domain.model.Notification
import com.example.dhbt.domain.model.NotificationTarget
import com.example.dhbt.domain.repository.NotificationRepository
import com.example.dhbt.utils.notification.NotificationManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject

class NotificationRepositoryImpl @Inject constructor(
    private val notificationDao: NotificationDao,
    private val taskDao: TaskDao,
    private val notificationMapper: NotificationMapper,
    private val dataStore: DHbtDataStore,
    private val notificationManager: NotificationManager,
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
        // Отменяем все существующие запланированные уведомления
        workManager.cancelAllWorkByTag("notification")

        // Получаем все уведомления из базы данных
        val notifications = notificationDao.getAllNotifications().first()

        Timber.d("Переназначение всех ${notifications.size} уведомлений")

        // Перепланируем уведомления с использованием нового NotificationManager
        notifications.forEach { entity ->
            if (entity.isEnabled) {
                val notification = notificationMapper.mapFromEntity(entity)
                notificationDao.updateWorkId(notification.id, null)

                // Используем новую систему уведомлений для планирования
                when (notification.targetType) {
                    NotificationTarget.TASK -> {
                        // Получаем информацию о задаче
                        val task = taskDao.getTaskById(notification.targetId)
                        if (task != null) {
                            notificationManager.scheduleTaskNotification(
                                taskId = notification.targetId,
                                taskTitle = task.title,
                                dueDate = task.dueDate ?: System.currentTimeMillis() + 86400000, // +1 день по умолчанию
                                dueTime = notification.time,
                                description = notification.message,
                                priority = task.priority ?: 0
                            )
                        }
                    }
                    NotificationTarget.HABIT -> {
                        // Привычки обрабатываются отдельно с новым NotificationManager
                        if (notification.daysOfWeek.isNotEmpty()) {
                            notificationManager.scheduleHabitNotification(
                                habitId = notification.targetId,
                                habitTitle = "Привычка", // Здесь можно получить имя привычки из БД
                                time = notification.time,
                                daysOfWeek = notification.daysOfWeek,
                                message = notification.message ?: "Напоминание о привычке"
                            )
                        }
                    }
                    NotificationTarget.SYSTEM -> {
                        // Системные уведомления
                        notificationManager.showSystemNotification(
                            id = notification.targetId,
                            title = "Уведомление системы",
                            message = notification.message ?: "Системное уведомление",
                            isImportant = false
                        )
                    }
                }
            }
        }
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
            Timber.e(e, "Ошибка при добавлении уведомления")
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

        // Отменяем запланированные уведомления в зависимости от типа
        notification?.let {
            when (it.targetType) {
                NotificationTarget.TASK -> notificationManager.cancelTaskNotifications(it.targetId)
                NotificationTarget.HABIT -> notificationManager.cancelHabitNotifications(it.targetId)
                NotificationTarget.SYSTEM -> { /* Для системных не требуется отдельная отмена */ }
            }
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
                when (it.targetType) {
                    NotificationTarget.TASK -> notificationManager.cancelTaskNotifications(it.targetId)
                    NotificationTarget.HABIT -> notificationManager.cancelHabitNotifications(it.targetId)
                    NotificationTarget.SYSTEM -> { /* Для системных не требуется отдельная отмена */ }
                }
            }
        }
    }

    override suspend fun scheduleExistingNotification(notification: Notification) {
        scheduleNotification(notification)
    }

    override suspend fun deleteNotificationsForTarget(targetId: String, targetType: NotificationTarget) {
        val notifications = notificationDao.getNotificationsForTarget(targetId, targetType.value).first()

        // Отменяем запланированные уведомления
        when (targetType) {
            NotificationTarget.TASK -> notificationManager.cancelTaskNotifications(targetId)
            NotificationTarget.HABIT -> notificationManager.cancelHabitNotifications(targetId)
            NotificationTarget.SYSTEM -> { /* Для системных не требуется отдельная отмена */ }
        }

        notificationDao.deleteNotificationsForTarget(targetId, targetType.value)
    }

    override suspend fun scheduleNotification(notification: Notification) {
        when (notification.targetType) {
            NotificationTarget.TASK -> {
                val task = taskDao.getTaskById(notification.targetId)
                if (task != null) {
                    notificationManager.scheduleTaskNotification(
                        taskId = notification.targetId,
                        taskTitle = task.title,
                        dueDate = task.dueDate ?: System.currentTimeMillis() + 86400000, // +1 день по умолчанию
                        dueTime = notification.time,
                        description = notification.message,
                        priority = task.priority ?: 0
                    )
                }
            }
            NotificationTarget.HABIT -> {
                if (notification.daysOfWeek.isNotEmpty()) {
                    notificationManager.scheduleHabitNotification(
                        habitId = notification.targetId,
                        habitTitle = "Привычка", // Здесь можно получить имя привычки из БД
                        time = notification.time,
                        daysOfWeek = notification.daysOfWeek,
                        message = notification.message ?: "Напоминание о привычке"
                    )
                }
            }
            NotificationTarget.SYSTEM -> {
                // Для системных уведомлений используем разное поведение в зависимости от ID
                if (notification.targetId.startsWith("system_")) {
                    if (notification.repeatInterval != null && notification.repeatInterval > 0) {
                        // Для повторяющихся системных уведомлений (утро/вечер)
                        setupSystemRecurringNotification(notification)
                    } else {
                        notificationManager.showSystemNotification(
                            id = notification.targetId,
                            title = "Уведомление системы",
                            message = notification.message ?: "Системное уведомление",
                            isImportant = notification.targetId.contains("important")
                        )
                    }
                }
            }
        }
    }

    override suspend fun scheduleTaskNotification(taskId: String, dueDate: Long, dueTime: String?) {
        val taskEntity = taskDao.getTaskById(taskId)
        val taskTitle = taskEntity?.title ?: "Задача"
        val priority = taskEntity?.priority ?: 0

        // Создаем запись уведомления в БД
        val notification = Notification(
            targetId = taskId,
            targetType = NotificationTarget.TASK,
            time = dueTime ?: "09:00",
            message = "Задача: $taskTitle",
            isEnabled = true,
            scheduledDate = dueDate // Сохраняем дату
        )

        val entity = notificationMapper.mapToEntity(notification)
        notificationDao.insertNotification(entity)

        // Планируем уведомление через новый NotificationManager
        notificationManager.scheduleTaskNotification(
            taskId = taskId,
            taskTitle = taskTitle,
            dueDate = dueDate,
            dueTime = dueTime,
            description = notification.message,
            priority = priority
        )
    }

    override suspend fun scheduleHabitNotification(habitId: String, time: String, daysOfWeek: List<Int>) {
        // Создаем запись уведомления в БД
        val notification = Notification(
            targetId = habitId,
            targetType = NotificationTarget.HABIT,
            time = time,
            daysOfWeek = daysOfWeek,
            message = "Напоминание о привычке"
        )

        val entity = notificationMapper.mapToEntity(notification)
        notificationDao.insertNotification(entity)

        // Планируем уведомление через новый NotificationManager
        notificationManager.scheduleHabitNotification(
            habitId = habitId,
            habitTitle = "Привычка", // Здесь можно получить имя привычки из БД
            time = time,
            daysOfWeek = daysOfWeek,
            message = "Напоминание о привычке"
        )
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
            setupSystemRecurringNotification(wakeUpNotification)

            // Сохраняем в БД
            val entity = notificationMapper.mapToEntity(wakeUpNotification)
            notificationDao.insertNotification(entity)
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
            setupSystemRecurringNotification(sleepNotification)

            // Сохраняем в БД
            val entity = notificationMapper.mapToEntity(sleepNotification)
            notificationDao.insertNotification(entity)
        }
    }

    override suspend fun cancelScheduledNotification(notificationId: String) {
        val notification = getNotificationById(notificationId)
        notification?.let {
            when (it.targetType) {
                NotificationTarget.TASK -> notificationManager.cancelTaskNotifications(it.targetId)
                NotificationTarget.HABIT -> notificationManager.cancelHabitNotifications(it.targetId)
                NotificationTarget.SYSTEM -> { /* Для системных не требуется отдельная отмена */ }
            }
        }

        notificationDao.updateWorkId(notificationId, null)
    }

    /**
     * Вспомогательный метод для настройки системных повторяющихся уведомлений
     */
    private fun setupSystemRecurringNotification(notification: Notification) {
        try {
            val notificationTime = LocalTime.parse(notification.time, DateTimeFormatter.ofPattern("HH:mm"))
            val now = LocalDateTime.now()
            val targetTime = now.toLocalDate().atTime(notificationTime)

            // Если время уже прошло, планируем на следующий день
            val showTime = if (now.toLocalTime().isAfter(notificationTime)) {
                targetTime.plusDays(1)
            } else {
                targetTime
            }

            val title = when {
                notification.targetId.contains("wakeup") -> "Доброе утро"
                notification.targetId.contains("sleep") -> "Добрый вечер"
                else -> "Уведомление системы"
            }

            // Планируем первое уведомление через новый NotificationManager
            notificationManager.showSystemNotification(
                id = notification.targetId,
                title = title,
                message = notification.message ?: "Системное уведомление",
                isImportant = false
            )

            // Для повторяющихся уведомлений можно использовать специальный метод в NotificationManager
            // Этот блок добавить, если ваш NotificationManager поддерживает такое планирование
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при настройке системного уведомления")
        }
    }
}