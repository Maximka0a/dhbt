package com.example.dhbt.utils

import com.example.dhbt.domain.model.Notification
import com.example.dhbt.domain.model.NotificationTarget
import com.example.dhbt.domain.repository.NotificationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

/**
 * Вспомогательный класс для отладки уведомлений
 */
class NotificationDebugHelper @Inject constructor(
    private val notificationRepository: NotificationRepository
) {
    /**
     * Проверяет работоспособность репозитория уведомлений
     */
    suspend fun testNotificationRepository() = withContext(Dispatchers.IO) {
        try {
            Timber.d("Начало тестирования NotificationRepository")

            // Создаем тестовое уведомление
            val testNotificationId = UUID.randomUUID().toString()
            val targetId = "test_target_${System.currentTimeMillis()}"

            val notification = Notification(
                id = testNotificationId,
                targetId = targetId,
                targetType = NotificationTarget.HABIT,
                time = "09:00",
                daysOfWeek = listOf(1, 2, 3, 4, 5),
                isEnabled = true,
                message = "Тестовое уведомление"
            )

            Timber.d("Попытка сохранения тестового уведомления: id=$testNotificationId, targetId=$targetId")
            val savedId = notificationRepository.addNotification(notification)
            Timber.d("Тестовое уведомление сохранено с ID: $savedId")

            // Проверяем, что уведомление сохранилось
            val savedNotifications = notificationRepository.getNotificationsForTarget(targetId, NotificationTarget.HABIT).first()
            Timber.d("Найдено ${savedNotifications.size} уведомлений для цели $targetId")

            for (n in savedNotifications) {
                Timber.d("Уведомление: id=${n.id}, targetId=${n.targetId}, time=${n.time}, enabled=${n.isEnabled}")
            }

            // Удаляем тестовое уведомление
            Timber.d("Удаление тестовых уведомлений для цели $targetId")
            notificationRepository.deleteNotificationsForTarget(targetId, NotificationTarget.HABIT)

            // Проверяем, что уведомление удалилось
            val afterDeleteNotifications = notificationRepository.getNotificationsForTarget(targetId, NotificationTarget.HABIT).first()
            Timber.d("После удаления найдено ${afterDeleteNotifications.size} уведомлений")

            Timber.d("Тестирование NotificationRepository завершено успешно")
            return@withContext true
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при тестировании NotificationRepository")
            return@withContext false
        }
    }
}