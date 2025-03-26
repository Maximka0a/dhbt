package com.example.dhbt.domain.repository

import com.example.dhbt.domain.model.*
import kotlinx.coroutines.flow.Flow

interface HabitRepository {
    // Базовые методы для привычек
    fun getAllHabits(): Flow<List<Habit>>
    fun getActiveHabits(): Flow<List<Habit>>
    fun getHabitsByCategory(categoryId: String): Flow<List<Habit>>
    suspend fun getHabitById(habitId: String): Habit?
    suspend fun addHabit(habit: Habit): String
    suspend fun updateHabit(habit: Habit)
    suspend fun deleteHabit(habitId: String)
    suspend fun changeHabitStatus(habitId: String, status: HabitStatus)
    suspend fun incrementHabitProgress(habitId: String)

    // Методы для получения полной информации (привычка + частота)
    suspend fun getHabitWithFrequency(habitId: String): Pair<Habit, HabitFrequency?>
    fun getAllHabitsWithFrequency(): Flow<List<Pair<Habit, HabitFrequency?>>>
    fun getActiveHabitsWithFrequency(): Flow<List<Pair<Habit, HabitFrequency?>>>

    // Методы для работы с частотой
    suspend fun getHabitFrequency(habitId: String): HabitFrequency?
    suspend fun setHabitFrequency(habitId: String, frequency: HabitFrequency)
    suspend fun updateHabitFrequency(frequency: HabitFrequency)
    suspend fun deleteHabitFrequency(habitId: String)

    // Другие методы остаются без изменений
    suspend fun getHabitProgressForToday(habitId: String): Float
    suspend fun getAllHabitsProgressForToday(): Map<String, Float>

    fun getHabitTrackings(habitId: String): Flow<List<HabitTracking>>
    fun getHabitTrackingsForRange(habitId: String, startDate: Long, endDate: Long): Flow<List<HabitTracking>>
    suspend fun getHabitTrackingForDate(habitId: String, date: Long): HabitTracking?
    suspend fun trackHabit(tracking: HabitTracking)
    suspend fun updateHabitTracking(tracking: HabitTracking)

    suspend fun getHabitStreak(habitId: String): Int
    suspend fun getBestStreak(habitId: String): Int
    suspend fun getCompletionRate(habitId: String, days: Int): Float
    suspend fun calculateHabitProgress(habitId: String, date: Long): Float
}