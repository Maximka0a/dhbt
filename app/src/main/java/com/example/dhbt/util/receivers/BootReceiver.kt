package com.example.dhbt.utils.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Приемник для восстановления уведомлений после перезагрузки устройства
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var notificationManager: com.example.dhbt.utils.notification.NotificationManager

    @Inject
    lateinit var notificationRepository: com.example.dhbt.domain.repository.NotificationRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON" ||
            intent.action == "com.htc.intent.action.QUICKBOOT_POWERON") {

            Timber.d("Устройство перезагружено, восстанавливаем уведомления")

            // Запускаем корутину для восстановления уведомлений
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    notificationRepository.rescheduleAllNotifications()
                    Timber.d("Уведомления успешно восстановлены")
                } catch (e: Exception) {
                    Timber.e(e, "Ошибка восстановления уведомлений")
                }
            }
        }
    }
}