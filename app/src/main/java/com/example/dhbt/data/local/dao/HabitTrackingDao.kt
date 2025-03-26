package com.example.dhbt.data.local.dao

import androidx.room.*
import com.example.dhbt.data.local.entity.HabitTrackingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitTrackingDao {
    @Query("SELECT * FROM habit_trackings WHERE habitId = :habitId ORDER BY date DESC")
    fun getTrackingsForHabit(habitId: String): Flow<List<HabitTrackingEntity>>

    @Query("SELECT * FROM habit_trackings WHERE habitId = :habitId AND date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun getTrackingsForHabitInRange(habitId: String, startDate: Long, endDate: Long): Flow<List<HabitTrackingEntity>>

    @Query("SELECT * FROM habit_trackings WHERE date = :date AND habitId = :habitId")
    suspend fun getHabitTrackingForDate(habitId: String, date: Long): HabitTrackingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabitTracking(tracking: HabitTrackingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabitTrackings(trackings: List<HabitTrackingEntity>)

    @Update
    suspend fun updateHabitTracking(tracking: HabitTrackingEntity)


    @Query("SELECT * FROM habit_trackings WHERE habitId = :habitId AND date = :date")
    suspend fun getHabitTrackingForDate(habitId: String, date: String): HabitTrackingEntity?

    @Query("SELECT * FROM habit_trackings WHERE date = :date")
    suspend fun getAllHabitTrackingsForDate(date: String): List<HabitTrackingEntity>

    @Delete
    suspend fun deleteHabitTracking(tracking: HabitTrackingEntity)

    @Query("DELETE FROM habit_trackings WHERE habitId = :habitId")
    suspend fun deleteTrackingsForHabit(habitId: String)

    @Query("SELECT COUNT(*) FROM habit_trackings WHERE habitId = :habitId AND isCompleted = 1 AND date BETWEEN :startDate AND :endDate")
    suspend fun countCompletedTrackingsInRange(habitId: String, startDate: Long, endDate: Long): Int
}