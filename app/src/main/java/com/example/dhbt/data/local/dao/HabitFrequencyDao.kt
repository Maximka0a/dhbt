package com.example.dhbt.data.local.dao

import androidx.room.*
import com.example.dhbt.data.local.entity.HabitFrequencyEntity

@Dao
interface HabitFrequencyDao {
    @Query("SELECT * FROM habit_frequencies WHERE habitId = :habitId")
    suspend fun getFrequencyForHabit(habitId: String): HabitFrequencyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabitFrequency(habitFrequency: HabitFrequencyEntity)

    @Update
    suspend fun updateHabitFrequency(habitFrequency: HabitFrequencyEntity)

    @Delete
    suspend fun deleteHabitFrequency(habitFrequency: HabitFrequencyEntity)

    @Query("DELETE FROM habit_frequencies WHERE habitId = :habitId")
    suspend fun deleteFrequencyForHabit(habitId: String)
}