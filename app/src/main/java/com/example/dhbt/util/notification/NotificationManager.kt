package com.example.dhbt.utils.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.dhbt.R
import com.example.dhbt.data.local.datastore.DHbtDataStore
import com.example.dhbt.domain.model.NotificationTarget
import com.example.dhbt.presentation.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Централизованный менеджер уведомлений с поддержкой современных API
 */
@Singleton
class NotificationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DHbtDataStore
) {
    // Группы каналов уведомлений
    companion object {
        const val GROUP_TASKS = "group_tasks"
        const val GROUP_HABITS = "group_habits"
        const val GROUP_SYSTEM = "group_system"

        const val CHANNEL_TASKS_DEFAULT = "channel_tasks_default"
        const val CHANNEL_TASKS_URGENT = "channel_tasks_urgent"
        const val CHANNEL_HABITS_DEFAULT = "channel_habits_default"
        const val CHANNEL_SYSTEM_DEFAULT = "channel_system_default"
        const val CHANNEL_SYSTEM_IMPORTANT = "channel_system_important"
    }

    private val workManager = WorkManager.getInstance(context)
    private val notificationManager = NotificationManagerCompat.from(context)

    /**
     * Инициализация каналов уведомлений
     */
    init {
        createNotificationChannels()
    }

    /**
     * Создает необходимые каналы уведомлений
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Создаем группы каналов
            val taskGroup = NotificationChannelGroup(
                GROUP_TASKS, "Задачи"
            )

            val habitGroup = NotificationChannelGroup(
                GROUP_HABITS, "Привычки"
            )

            val systemGroup = NotificationChannelGroup(
                GROUP_SYSTEM, "Системные"
            )

            // Регистрируем группы
            notificationManager.createNotificationChannelGroups(
                listOf(taskGroup, habitGroup, systemGroup)
            )

            // Звук для уведомлений по умолчанию
            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            // Создаем каналы для задач
            val taskDefaultChannel = NotificationChannel(
                CHANNEL_TASKS_DEFAULT,
                "Задачи",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Уведомления о задачах"
                enableLights(true)
                lightColor = Color.BLUE
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 250, 250)
                setSound(defaultSoundUri, audioAttributes)
                group = GROUP_TASKS
            }

            val taskUrgentChannel = NotificationChannel(
                CHANNEL_TASKS_URGENT,
                "Срочные задачи",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Уведомления о срочных задачах"
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 250, 250)
                setSound(defaultSoundUri, audioAttributes)
                group = GROUP_TASKS
            }

            // Канал для привычек
            val habitDefaultChannel = NotificationChannel(
                CHANNEL_HABITS_DEFAULT,
                "Привычки",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Уведомления о привычках"
                enableLights(true)
                lightColor = Color.GREEN
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 250, 250)
                setSound(defaultSoundUri, audioAttributes)
                group = GROUP_HABITS
            }

            // Каналы для системных уведомлений
            val systemDefaultChannel = NotificationChannel(
                CHANNEL_SYSTEM_DEFAULT,
                "Системные",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Общие системные уведомления"
                enableLights(false)
                group = GROUP_SYSTEM
            }

            val systemImportantChannel = NotificationChannel(
                CHANNEL_SYSTEM_IMPORTANT,
                "Важные системные",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Важные системные уведомления"
                enableLights(true)
                lightColor = Color.YELLOW
                enableVibration(true)
                group = GROUP_SYSTEM
            }

            // Регистрируем все каналы
            notificationManager.createNotificationChannels(
                listOf(
                    taskDefaultChannel,
                    taskUrgentChannel,
                    habitDefaultChannel,
                    systemDefaultChannel,
                    systemImportantChannel
                )
            )
        }
    }

    /**
     * Планирует уведомление для задачи
     */
    suspend fun scheduleTaskNotification(
        taskId: String,
        taskTitle: String,
        dueDate: Long,
        dueTime: String?,
        description: String? = null,
        priority: Int = 0 // 0 - обычная, 1 - высокая
    ) {
        try {
            // Проверяем, есть ли у пользователя разрешения на показ уведомлений
            if (!hasNotificationPermission()) {
                Timber.w("Нет разрешения на показ уведомлений")
                return
            }

            // Определяем когда показывать уведомление
            val scheduledDateTime = calculateTaskNotificationTime(dueDate, dueTime)
            val now = Instant.now().toEpochMilli()

            // Если дата в прошлом, не планируем
            if (scheduledDateTime < now) {
                Timber.d("Не планируем уведомление для задачи с прошедшей датой: $taskId")
                return
            }

            // Создаем данные для Worker
            val data = Data.Builder()
                .putString("taskId", taskId)
                .putString("title", taskTitle)
                .putString("message", description ?: "Напоминание о задаче: $taskTitle")
                .putInt("targetType", NotificationTarget.TASK.value)
                .putInt("priority", priority)
                .putLong("scheduledTime", scheduledDateTime)
                .putLong("createdTime", now) // Момент создания уведомления
                .build()

            // Время до показа уведомления в миллисекундах
            val delayMs = scheduledDateTime - now

            // Создаем ограничения для работы
            val constraints = Constraints.Builder()
                .setRequiresDeviceIdle(false)
                .setRequiresBatteryNotLow(false)
                .build()

            // Создаем запрос на выполнение работы
            val notificationWorkRequest = OneTimeWorkRequestBuilder<TaskNotificationWorker>()
                .setInputData(data)
                .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    Duration.ofMinutes(5)
                )
                .addTag("notification")
                .addTag("task")
                .addTag("task_$taskId")
                .build()

            // Запланировать работу с заменой, если уже существует
            workManager.enqueueUniqueWork(
                "task_notification_$taskId",
                ExistingWorkPolicy.REPLACE,
                notificationWorkRequest
            )

            // Сохраняем ID работы
            val workId = notificationWorkRequest.id
            Timber.d("Запланировано уведомление для задачи $taskId, workId: $workId, дата показа: ${Instant.ofEpochMilli(scheduledDateTime)}")

            // Здесь можно сохранить информацию о запланированном уведомлении в БД
        } catch (e: Exception) {
            Timber.e(e, "Ошибка планирования уведомления для задачи $taskId")
        }
    }

    /**
     * Планирует уведомление для привычки
     */
    suspend fun scheduleHabitNotification(
        habitId: String,
        habitTitle: String,
        time: String,
        daysOfWeek: List<Int>,
        message: String? = null
    ) {
        try {
            // Проверяем правильность входных данных
            if (daysOfWeek.isEmpty()) {
                Timber.w("Не указаны дни недели для привычки $habitId")
                return
            }

            // Парсим время
            val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
            val localTime = try {
                LocalTime.parse(time, timeFormatter)
            } catch (e: Exception) {
                Timber.e(e, "Неверный формат времени: $time")
                LocalTime.of(9, 0) // По умолчанию 9:00
            }

            // Для каждого дня недели создаем отдельную повторяющуюся задачу
            daysOfWeek.forEach { dayOfWeek ->
                // Вычисляем следующий день недели
                val today = LocalDate.now()
                val currentDayOfWeek = today.dayOfWeek.value
                var daysUntil = dayOfWeek - currentDayOfWeek

                // Если текущий день уже прошел, берем следующую неделю
                if (daysUntil <= 0 || (daysUntil == 0 && LocalTime.now().isAfter(localTime))) {
                    daysUntil += 7
                }

                val nextOccurrence = today.plusDays(daysUntil.toLong())
                val scheduledDateTime = LocalDateTime.of(nextOccurrence, localTime)
                val scheduledMillis = scheduledDateTime
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()

                // Создаем данные для Worker
                val data = Data.Builder()
                    .putString("habitId", habitId)
                    .putString("title", habitTitle)
                    .putString("message", message ?: "Напоминание о привычке: $habitTitle")
                    .putInt("targetType", NotificationTarget.HABIT.value)
                    .putInt("dayOfWeek", dayOfWeek)
                    .putLong("scheduledTime", scheduledMillis)
                    .putString("scheduledTimeFormatted", localTime.format(timeFormatter))
                    .build()

                // Интервал повторения - 7 дней (еженедельно)
                val repeatInterval = 7L * 24 * 60

                // Вычисляем начальную задержку
                val now = Instant.now().toEpochMilli()
                val initialDelay = scheduledMillis - now

                // Создаем периодический запрос для привычки
                val habitWorkRequest = PeriodicWorkRequestBuilder<HabitNotificationWorker>(
                    repeatInterval, TimeUnit.MINUTES
                )
                    .setInputData(data)
                    .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                    .addTag("notification")
                    .addTag("habit")
                    .addTag("habit_$habitId")
                    .addTag("habit_day_$dayOfWeek")
                    .build()

                // Запланировать работу
                workManager.enqueueUniquePeriodicWork(
                    "habit_notification_${habitId}_day_$dayOfWeek",
                    ExistingPeriodicWorkPolicy.REPLACE,
                    habitWorkRequest
                )

                Timber.d(
                    "Запланировано уведомление для привычки $habitId на день $dayOfWeek, " +
                            "время: $localTime, первый показ: $scheduledDateTime"
                )

                // ВАЖНОЕ ДОПОЛНЕНИЕ: Если сегодня день привычки и время еще не прошло, запускаем немедленное уведомление
                if (dayOfWeek == currentDayOfWeek && initialDelay < 24 * 60 * 60 * 1000) {
                    // Создаем запрос на немедленное выполнение
                    val immediateData = Data.Builder()
                        .putString("habitId", habitId)
                        .putString("title", habitTitle)
                        .putString("message", message ?: "Напоминание о привычке на сегодня: $habitTitle")
                        .putInt("targetType", NotificationTarget.HABIT.value)
                        .putInt("dayOfWeek", dayOfWeek)
                        .putLong("scheduledTime", System.currentTimeMillis() + 60 * 1000) // Через минуту
                        .putString("scheduledTimeFormatted", localTime.format(timeFormatter))
                        .build()

                    val immediateWorkRequest = OneTimeWorkRequestBuilder<HabitNotificationWorker>()
                        .setInputData(immediateData)
                        .setInitialDelay(60, TimeUnit.SECONDS) // Задержка 60 секунд
                        .addTag("notification")
                        .addTag("habit_immediate")
                        .build()

                    workManager.enqueueUniqueWork(
                        "immediate_habit_${habitId}_${System.currentTimeMillis()}",
                        ExistingWorkPolicy.REPLACE,
                        immediateWorkRequest
                    )

                    Timber.d("Запланировано дополнительное немедленное уведомление для привычки $habitId через 60 сек")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Ошибка планирования уведомления для привычки $habitId")
        }
    }

    /**
     * Отменяет все уведомления для задачи
     */
    fun cancelTaskNotifications(taskId: String) {
        workManager.cancelAllWorkByTag("task_$taskId")
        Timber.d("Отменены все уведомления для задачи $taskId")
    }

    /**
     * Отменяет все уведомления для привычки
     */
    fun cancelHabitNotifications(habitId: String) {
        workManager.cancelAllWorkByTag("habit_$habitId")
        Timber.d("Отменены все уведомления для привычки $habitId")
    }

    /**
     * Показывает уведомление для задачи с учетом настроек пользователя
     */
    suspend fun showTaskNotification(
        taskId: String,
        title: String,
        message: String,
        priority: Int = 0,
        actions: List<NotificationAction> = emptyList()
    ) {
        try {
            // Проверяем разрешения
            if (!hasNotificationPermission()) {
                Timber.w("Нет разрешения на показ уведомлений")
                return
            }

            // Получаем настройки пользователя
            val preferences = dataStore.userPreferences.first()
            val soundEnabled = preferences.defaultSoundEnabled ?: true
            val vibrationEnabled = preferences.defaultVibrationEnabled ?: true

            // Создаем интент для открытия задачи
            val intent = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                putExtra("openTaskId", taskId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            // Создаем PendingIntent
            val pendingIntent = PendingIntent.getActivity(
                context,
                taskId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Выбираем канал в зависимости от приоритета
            val channelId = if (priority > 0) CHANNEL_TASKS_URGENT else CHANNEL_TASKS_DEFAULT

            // Создаем билдер уведомления
            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(if (priority > 0) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

            // Применяем настройки звука и вибрации в зависимости от предпочтений пользователя
            // Настраиваем звук
            if (soundEnabled) {
                val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                builder.setSound(soundUri)
            } else {
                builder.setSound(null)
            }

            // Настраиваем вибрацию
            if (vibrationEnabled) {
                builder.setVibrate(longArrayOf(0, 250, 250, 250)) // Шаблон вибрации
            } else {
                builder.setVibrate(null)
            }

            // Добавляем действия (если есть)
            actions.forEach { action ->
                builder.addAction(
                    NotificationCompat.Action.Builder(
                        action.icon,
                        action.title,
                        action.pendingIntent
                    ).build()
                )
            }

            // Показываем уведомление с безопасной обработкой
            try {
                Timber.d("Отправляем уведомление для задачи $taskId (звук: $soundEnabled, вибрация: $vibrationEnabled)")
                if (hasNotificationPermission()) {
                    notificationManager.notify(taskId.hashCode(), builder.build())
                    Timber.d("Уведомление для задачи успешно отправлено")
                } else {
                    Timber.w("Пропуск уведомления из-за отсутствия разрешения")
                }
            } catch (se: SecurityException) {
                Timber.e(se, "SecurityException при показе уведомления (отсутствует разрешение)")
            }
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при показе уведомления для задачи $taskId")
        }
    }

    /**
     * Показывает уведомление для привычки с учетом настроек пользователя
     */
    suspend fun showHabitNotification(
        habitId: String,
        title: String,
        message: String,
        actions: List<NotificationAction> = emptyList()
    ) {
        try {
            // Проверка разрешения с логированием
            val hasPermission = hasNotificationPermission()
            Timber.d("showHabitNotification: habitId=$habitId, разрешение=$hasPermission")

            if (!hasPermission) {
                Timber.w("Нет разрешения на показ уведомлений")
                return
            }

            // Обновляем настройки каналов перед показом уведомления
            updateNotificationChannels()

            // Получаем настройки пользователя
            val preferences = dataStore.userPreferences.first()
            val soundEnabled = preferences.defaultSoundEnabled ?: true
            val vibrationEnabled = preferences.defaultVibrationEnabled ?: true

            // Создаем интент для открытия привычки
            val intent = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                putExtra("openHabitId", habitId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            // Создаем PendingIntent
            val pendingIntent = PendingIntent.getActivity(
                context,
                habitId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Создаем билдер уведомления
            val builder = NotificationCompat.Builder(context, CHANNEL_HABITS_DEFAULT)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

            // Настраиваем звук (только для старых версий Android)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                if (soundEnabled) {
                    val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    builder.setSound(soundUri)
                } else {
                    builder.setSound(null)
                }

                // Настраиваем вибрацию (только для старых версий Android)
                if (vibrationEnabled) {
                    builder.setVibrate(longArrayOf(0, 250, 250, 250)) // Шаблон вибрации
                } else {
                    builder.setVibrate(null)
                }
            }

            // Добавляем действия
            actions.forEach { action ->
                builder.addAction(
                    NotificationCompat.Action.Builder(
                        action.icon,
                        action.title,
                        action.pendingIntent
                    ).build()
                )
            }

            // Показываем уведомление с безопасной обработкой
            val notificationId = (habitId + System.currentTimeMillis()).hashCode()
            try {
                Timber.d("Отправляем уведомление с ID $notificationId (звук: $soundEnabled, вибрация: $vibrationEnabled)")
                if (hasNotificationPermission()) {
                    notificationManager.notify(notificationId, builder.build())
                    logNotificationChannelSettings() // Для отладки
                    Timber.d("Уведомление успешно отправлено")
                } else {
                    Timber.w("Пропуск уведомления из-за отсутствия разрешения")
                }
            } catch (se: SecurityException) {
                Timber.e(se, "SecurityException при показе уведомления (отсутствует разрешение)")
            }
        } catch (e: Exception) {
            Timber.e(e, "Общая ошибка при показе уведомления для привычки $habitId")
        }
    }
    /**
     * Проверяет и логирует текущее состояние каналов уведомлений для отладки
     */
    fun logNotificationChannelSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val systemNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                val channels = listOf(
                    CHANNEL_TASKS_DEFAULT,
                    CHANNEL_TASKS_URGENT,
                    CHANNEL_HABITS_DEFAULT
                )

                channels.forEach { channelId ->
                    val channel = systemNotificationManager.getNotificationChannel(channelId)
                    if (channel != null) {
                        Timber.d("""
                            📢 Канал $channelId:
                            - Звук: ${channel.sound != null}
                            - Вибрация: ${channel.vibrationPattern != null}
                            - Включена вибрация: ${channel.shouldVibrate()}
                            - Важность: ${channel.importance}
                            - Имя: ${channel.name}
                        """.trimIndent())
                    } else {
                        Timber.e("Канал $channelId не найден!")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при логировании настроек каналов")
            }
        }
    }

    /**
     * Обновляет настройки каналов уведомлений в соответствии с пользовательскими предпочтениями
     * Эта функция должна вызываться каждый раз при изменении настроек уведомлений
     */
    suspend fun updateNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val preferences = dataStore.userPreferences.first()
                val soundEnabled = preferences.defaultSoundEnabled ?: true
                val vibrationEnabled = preferences.defaultVibrationEnabled ?: true

                // Получаем системный NotificationManager
                val systemNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                // Звук для уведомлений по умолчанию
                val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()

                // Обновляем каналы для задач
                systemNotificationManager.getNotificationChannel(CHANNEL_TASKS_DEFAULT)?.let { channel ->
                    channel.enableLights(true)
                    channel.lightColor = Color.BLUE
                    channel.enableVibration(vibrationEnabled)
                    if (vibrationEnabled) {
                        channel.vibrationPattern = longArrayOf(0, 250, 250, 250)
                    } else {
                        channel.vibrationPattern = null
                    }

                    if (soundEnabled) {
                        channel.setSound(defaultSoundUri, audioAttributes)
                    } else {
                        channel.setSound(null, null)
                    }
                    systemNotificationManager.createNotificationChannel(channel)
                    Timber.d("Обновлен канал уведомлений задач: звук=$soundEnabled, вибрация=$vibrationEnabled")
                }

                systemNotificationManager.getNotificationChannel(CHANNEL_TASKS_URGENT)?.let { channel ->
                    channel.enableLights(true)
                    channel.lightColor = Color.RED
                    channel.enableVibration(vibrationEnabled)
                    if (vibrationEnabled) {
                        channel.vibrationPattern = longArrayOf(0, 250, 250, 250)
                    } else {
                        channel.vibrationPattern = null
                    }

                    if (soundEnabled) {
                        channel.setSound(defaultSoundUri, audioAttributes)
                    } else {
                        channel.setSound(null, null)
                    }
                    systemNotificationManager.createNotificationChannel(channel)
                    Timber.d("Обновлен канал срочных уведомлений: звук=$soundEnabled, вибрация=$vibrationEnabled")
                }

                // Обновляем канал для привычек
                systemNotificationManager.getNotificationChannel(CHANNEL_HABITS_DEFAULT)?.let { channel ->
                    channel.enableLights(true)
                    channel.lightColor = Color.GREEN
                    channel.enableVibration(vibrationEnabled)
                    if (vibrationEnabled) {
                        channel.vibrationPattern = longArrayOf(0, 250, 250, 250)
                    } else {
                        channel.vibrationPattern = null
                    }

                    if (soundEnabled) {
                        channel.setSound(defaultSoundUri, audioAttributes)
                    } else {
                        channel.setSound(null, null)
                    }
                    systemNotificationManager.createNotificationChannel(channel)
                    Timber.d("Обновлен канал уведомлений привычек: звук=$soundEnabled, вибрация=$vibrationEnabled")
                }

            } catch (e: Exception) {
                Timber.e(e, "Ошибка при обновлении каналов уведомлений")
            }
        }
    }


    /**
     * Показывает системное уведомление
     */
    fun showSystemNotification(
        id: String,
        title: String,
        message: String,
        isImportant: Boolean = false,
        actions: List<NotificationAction> = emptyList()
    ) {
        try {
            // Проверяем разрешения
            if (!hasNotificationPermission()) {
                Timber.w("Нет разрешения на показ уведомлений")
                return
            }

            // Создаем интент для открытия приложения
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            // Создаем PendingIntent
            val pendingIntent = PendingIntent.getActivity(
                context,
                id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Выбираем канал в зависимости от важности
            val channelId = if (isImportant) CHANNEL_SYSTEM_IMPORTANT else CHANNEL_SYSTEM_DEFAULT

            // Создаем билдер уведомления
            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(if (isImportant) NotificationCompat.PRIORITY_DEFAULT else NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

            // Добавляем действия (если есть)
            actions.forEach { action ->
                builder.addAction(
                    NotificationCompat.Action.Builder(
                        action.icon,
                        action.title,
                        action.pendingIntent
                    ).build()
                )
            }

            // Показываем уведомление с безопасной обработкой
            try {
                Timber.d("Показ системного уведомления: $title")
                if (hasNotificationPermission()) {
                    notificationManager.notify(id.hashCode(), builder.build())
                    Timber.d("Системное уведомление успешно отправлено")
                } else {
                    Timber.w("Пропуск системного уведомления из-за отсутствия разрешения")
                }
            } catch (se: SecurityException) {
                Timber.e(se, "SecurityException при показе системного уведомления (отсутствует разрешение)")
            }
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при показе системного уведомления: $title")
        }
    }

    /**
     * Вычисляет оптимальное время для показа уведомления о задаче
     */
    private suspend fun calculateTaskNotificationTime(dueDate: Long, dueTime: String?): Long {
        val userPreferences = dataStore.userPreferences.first()
        val reminderMinutes = userPreferences.reminderTimeBeforeTask ?: 30

        // Определяем дату и время задачи
        val dueDateInstant = Instant.ofEpochMilli(dueDate)
        val dueDateTime = try {
            val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
            val localTime = if (!dueTime.isNullOrEmpty()) {
                LocalTime.parse(dueTime, timeFormatter)
            } else {
                LocalTime.of(23, 59) // Конец дня, если время не указано
            }

            LocalDateTime.ofInstant(dueDateInstant, ZoneId.systemDefault())
                .withHour(localTime.hour)
                .withMinute(localTime.minute)
                .withSecond(0)
                .withNano(0)
        } catch (e: Exception) {
            // При ошибке парсинга используем дату с дефолтным временем
            LocalDateTime.ofInstant(dueDateInstant, ZoneId.systemDefault())
                .withHour(23)
                .withMinute(59)
                .withSecond(0)
                .withNano(0)
        }

        // Вычисляем время напоминания (за reminderMinutes минут до срока)
        val notifyDateTime = dueDateTime.minus(reminderMinutes.toLong(), ChronoUnit.MINUTES)

        // Проверяем, если время напоминания уже прошло, используем текущее время + 1 минута
        val now = LocalDateTime.now()
        val finalNotifyDateTime = if (notifyDateTime.isBefore(now)) {
            now.plusMinutes(1)
        } else {
            notifyDateTime
        }

        // Преобразуем в миллисекунды
        return finalNotifyDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    /**
     * Проверяет разрешение на показ уведомлений
     */
    private fun hasNotificationPermission(): Boolean {
        // На API 33+ требуется POST_NOTIFICATIONS permission
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // До API 33 разрешение не требуется
        }
    }

    /**
     * Класс для представления действия в уведомлении
     */
    data class NotificationAction(
        @DrawableRes val icon: Int,
        val title: String,
        val pendingIntent: PendingIntent
    )
}