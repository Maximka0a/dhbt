package com.example.dhbt.data.local.dao

import androidx.room.*
import com.example.dhbt.data.local.entity.PomodoroSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PomodoroSessionDao {
    @Query("SELECT * FROM pomodoro_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<PomodoroSessionEntity>>

    @Query("SELECT * FROM pomodoro_sessions WHERE taskId = :taskId ORDER BY startTime DESC")
    fun getSessionsForTask(taskId: String): Flow<List<PomodoroSessionEntity>>

    @Query("SELECT * FROM pomodoro_sessions WHERE startTime BETWEEN :startTime AND :endTime ORDER BY startTime ASC")
    fun getSessionsInTimeRange(startTime: Long, endTime: Long): Flow<List<PomodoroSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: PomodoroSessionEntity)

    @Update
    suspend fun updateSession(session: PomodoroSessionEntity)

    @Delete
    suspend fun deleteSession(session: PomodoroSessionEntity)

    @Query("UPDATE pomodoro_sessions SET endTime = :endTime, isCompleted = true WHERE sessionId = :sessionId")
    suspend fun completeSession(sessionId: String, endTime: Long)

    @Query("SELECT SUM(duration) FROM pomodoro_sessions WHERE isCompleted = 1 AND type = 0 AND startTime BETWEEN :startTime AND :endTime")
    suspend fun getTotalFocusTimeInRange(startTime: Long, endTime: Long): Int?

    @Query("SELECT SUM(duration) FROM pomodoro_sessions WHERE isCompleted = 1 AND type = 0 AND taskId = :taskId")
    suspend fun getTotalFocusTimeForTask(taskId: String): Int?
}