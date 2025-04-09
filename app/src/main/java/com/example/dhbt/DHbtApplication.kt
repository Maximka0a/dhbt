package com.example.dhbt

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
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
    lateinit var notificationRepository: com.example.dhbt.domain.repository.NotificationRepository

    override fun onCreate() {
        super.onCreate()

        // Инициализация ежедневных системных уведомлений
        CoroutineScope(Dispatchers.IO).launch {
            notificationRepository.scheduleSystemDailyNotifications()
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}