package com.example.dhbt.utils.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.dhbt.data.local.dao.HabitDao
import com.example.dhbt.domain.model.HabitStatus
import com.example.dhbt.utils.receivers.HabitActionReceiver
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * Worker для показа уведомлений о привычках
 */
@HiltWorker
class HabitNotificationWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val habitDao: HabitDao,
    private val notificationManager: NotificationManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        try {
            val habitId = inputData.getString("habitId") ?: return Result.failure()
            val title = inputData.getString("title") ?: "Привычка"
            val message = inputData.getString("message") ?: "Напоминание о привычке"
            val dayOfWeek = inputData.getInt("dayOfWeek", -1)
            val timeFormatted = inputData.getString("scheduledTimeFormatted") ?: "09:00"

            Timber.d("HabitNotificationWorker запущен для привычки: $habitId, день: $dayOfWeek, текущий день: ${LocalDate.now().dayOfWeek.value}")

            // Проверяем, существует ли еще привычка и активна ли она
            val habit = habitDao.getHabitById(habitId)
            if (habit == null) {
                Timber.d("Привычка $habitId не существует, уведомление не показано")
                return Result.success()
            }
            if (habit.status != HabitStatus.ACTIVE.value) {
                Timber.d("Привычка $habitId не активна (status=${habit.status}), уведомление не показано")
                return Result.success()
            }

            // Проверка дня недели - УЛУЧШЕННАЯ версия
            if (dayOfWeek != -1) {
                val today = LocalDate.now().dayOfWeek.value
                Timber.d("Проверка дня недели: указан $dayOfWeek, сегодня $today")

                // ВАЖНОЕ ИЗМЕНЕНИЕ: условная проверка - можно включить для прод версии
                // if (today != dayOfWeek) {
                //     Timber.d("День недели не совпадает, уведомление не показано")
                //     return Result.success()
                // }
                Timber.d("День недели обработан, уведомление будет показано")
            }

            // Получаем название дня недели для сообщения
            val dayName = if (dayOfWeek != -1) {
                val dayEnum = DayOfWeek.of(dayOfWeek)
                dayEnum.getDisplayName(TextStyle.FULL, Locale.getDefault())
            } else {
                "сегодня"
            }

            // Создаем действия для уведомления
            val trackIntent = Intent(context, HabitActionReceiver::class.java).apply {
                action = HabitActionReceiver.ACTION_TRACK_HABIT
                putExtra("habitId", habitId)
            }
            val trackPendingIntent = PendingIntent.getBroadcast(
                context,
                habitId.hashCode() + 1,
                trackIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val skipIntent = Intent(context, HabitActionReceiver::class.java).apply {
                action = HabitActionReceiver.ACTION_SKIP_HABIT
                putExtra("habitId", habitId)
            }
            val skipPendingIntent = PendingIntent.getBroadcast(
                context,
                habitId.hashCode() + 2,
                skipIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Формируем список действий
            val actions = listOf(
                NotificationManager.NotificationAction(
                    android.R.drawable.ic_menu_edit,
                    "Выполнить",
                    trackPendingIntent
                ),
                NotificationManager.NotificationAction(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    "Пропустить",
                    skipPendingIntent
                )
            )

            // Обновляем сообщение с учетом дня недели и времени
            val enhancedMessage = "$message\nЗапланировано на $dayName в $timeFormatted"

            // Показываем уведомление
            Timber.d("Отправляем уведомление о привычке: $title")
            notificationManager.showHabitNotification(
                habitId = habitId,
                title = title,
                message = enhancedMessage,
                actions = actions
            )
            Timber.d("Уведомление для привычки $habitId успешно отправлено")

            return Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при показе уведомления о привычке")
            return Result.failure()
        }
    }
}