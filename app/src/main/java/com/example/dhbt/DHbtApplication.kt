package com.example.dhbt

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.dhbt.domain.repository.NotificationRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class DHbtApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var notificationRepository: NotificationRepository

    override fun onCreate() {
        super.onCreate()

        // Инициализация уведомлений
        CoroutineScope(Dispatchers.IO).launch {
            // Планирование системных уведомлений
            notificationRepository.scheduleSystemDailyNotifications()

            // Перепланирование существующих уведомлений
            notificationRepository.rescheduleAllNotifications()
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}