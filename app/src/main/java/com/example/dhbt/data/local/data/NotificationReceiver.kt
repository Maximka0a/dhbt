package com.example.dhbt.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.dhbt.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getStringExtra("notificationId") ?: return
        val targetId = intent.getStringExtra("targetId") ?: return
        val targetType = intent.getIntExtra("targetType", 0)
        val message = intent.getStringExtra("message") ?: "Напоминание"

        // Создаем и отображаем уведомление
        createNotificationChannel(context)
        showNotification(context, notificationId, targetType, message)
    }

    private fun showNotification(context: Context, notificationId: String, targetType: Int, message: String) {
        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getTitle(targetType))
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId.hashCode(), notificationBuilder.build())
    }

    private fun getTitle(targetType: Int): String {
        return when (targetType) {
            0 -> "Задача"
            1 -> "Привычка"
            else -> "DHbt"
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "DHbt Notifications"
            val descriptionText = "Уведомления для задач и привычек"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "dhbt_notifications"
    }
}