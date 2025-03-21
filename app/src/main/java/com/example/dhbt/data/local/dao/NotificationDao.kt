package com.example.dhbt.data.local.dao

import androidx.room.*
import com.example.dhbt.data.local.entity.NotificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications")
    fun getAllNotifications(): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE targetId = :targetId AND targetType = :targetType")
    fun getNotificationsForTarget(targetId: String, targetType: Int): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    @Update
    suspend fun updateNotification(notification: NotificationEntity)

    @Delete
    suspend fun deleteNotification(notification: NotificationEntity)

    @Query("DELETE FROM notifications WHERE notificationId = :notificationId")
    suspend fun deleteNotificationById(notificationId: String)

    @Query("DELETE FROM notifications WHERE targetId = :targetId AND targetType = :targetType")
    suspend fun deleteNotificationsForTarget(targetId: String, targetType: Int)

    @Query("UPDATE notifications SET isEnabled = :isEnabled WHERE notificationId = :notificationId")
    suspend fun updateNotificationStatus(notificationId: String, isEnabled: Boolean)
}