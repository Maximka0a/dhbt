package com.example.dhbt.utils

import android.content.Context
import com.example.dhbt.data.local.dao.NotificationDao
import com.example.dhbt.data.mapper.NotificationMapper
import com.example.dhbt.domain.repository.NotificationRepository
import com.example.dhbt.utils.notification.NotificationHandler
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Вспомогательный класс для отладки уведомлений.
 * В продакшн версии не используется.
 */
@Singleton
class NotificationDebugHelper @Inject constructor(
    private val notificationDao: NotificationDao,
    private val notificationRepository: NotificationRepository,
    private val notificationHandler: NotificationHandler,
    private val notificationMapper: NotificationMapper,
    @ApplicationContext private val context: Context
) {
    // Пустой класс для инъекции зависимостей
    // Реальная отладка выполняется через ADB или логирование в продакшне
}