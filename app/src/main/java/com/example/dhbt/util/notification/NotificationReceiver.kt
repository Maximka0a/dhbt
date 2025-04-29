package com.example.dhbt.utils.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.dhbt.R
import com.example.dhbt.domain.model.NotificationTarget
import com.example.dhbt.presentation.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NotificationReceiver : BroadcastReceiver() {

    companion object {
        private const val CHANNEL_ID = "dhbt_notifications"
        private const val CHANNEL_NAME = "Напоминания"
    }

    @Inject
    lateinit var notificationRepository: com.example.dhbt.domain.repository.NotificationRepository

    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getStringExtra("notificationId") ?: return
        val targetId = intent.getStringExtra("targetId") ?: return
        val targetType = intent.getIntExtra("targetType", 0)
        val message = intent.getStringExtra("message") ?: "Напоминание"

        CoroutineScope(Dispatchers.IO).launch {
            val notification = notificationRepository.getNotificationById(notificationId)
            if (notification != null) {
                showNotification(context, targetId, targetType, message)

                if (targetType == NotificationTarget.TASK.value) {
                    notificationRepository.deleteNotification(notificationId)
                }
            }
        }
    }

    private fun showNotification(context: Context, targetId: String, targetType: Int, message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        createNotificationChannelIfNeeded(context, notificationManager)

        val pendingIntent = createPendingIntent(context, targetId, targetType)

        val title = getTitleForNotificationType(targetType)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .build()

        val uniqueId = System.currentTimeMillis().toInt()
        notificationManager.notify(uniqueId, notification)
    }

    private fun createNotificationChannelIfNeeded(context: Context, notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createPendingIntent(context: Context, targetId: String, targetType: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("targetId", targetId)
            putExtra("targetType", targetType)
        }

        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun getTitleForNotificationType(targetType: Int): String {
        return when (NotificationTarget.fromInt(targetType)) {
            NotificationTarget.TASK -> "Задача"
            NotificationTarget.HABIT -> "Привычка"
            NotificationTarget.SYSTEM -> "DHbt"
        }
    }
}