package com.example.dhbt.data.local.dao

import androidx.room.*
import com.example.dhbt.data.local.entity.HabitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits")
    fun getAllHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE status = :status")
    fun getHabitsByStatus(status: Int): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE habitId = :habitId")
    suspend fun getHabitById(habitId: String): HabitEntity?

    @Query("SELECT * FROM habits WHERE categoryId = :categoryId")
    fun getHabitsByCategory(categoryId: String): Flow<List<HabitEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity): Long

    @Update
    suspend fun updateHabit(habit: HabitEntity)

    @Delete
    suspend fun deleteHabit(habit: HabitEntity)

    @Query("DELETE FROM habits WHERE habitId = :habitId")
    suspend fun deleteHabitById(habitId: String)

    @Query("UPDATE habits SET status = :status WHERE habitId = :habitId")
    suspend fun updateHabitStatus(habitId: String, status: Int)

    @Query("UPDATE habits SET currentStreak = :streak WHERE habitId = :habitId")
    suspend fun updateHabitStreak(habitId: String, streak: Int)

    @Query("UPDATE habits SET bestStreak = :bestStreak WHERE habitId = :habitId AND bestStreak < :bestStreak")
    suspend fun updateBestStreak(habitId: String, bestStreak: Int)
}