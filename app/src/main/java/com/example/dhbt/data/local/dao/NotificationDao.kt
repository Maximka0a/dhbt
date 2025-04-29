package com.example.dhbt.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.dhbt.data.local.entity.NotificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications")
    fun getAllNotifications(): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE targetId = :targetId AND targetType = :targetType")
    fun getNotificationsForTarget(targetId: String, targetType: Int): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE targetType = :targetType")
    fun getNotificationsByType(targetType: Int): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE notificationId = :notificationId")
    suspend fun getNotificationById(notificationId: String): NotificationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity): Long

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

    @Query("UPDATE notifications SET workId = :workId WHERE notificationId = :notificationId")
    suspend fun updateWorkId(notificationId: String, workId: String?)
}

