package com.example.dhbt.domain.repository

import com.example.dhbt.domain.model.PomodoroPreferences
import com.example.dhbt.domain.model.PomodoroSession
import com.example.dhbt.domain.model.PomodoroSessionType
import kotlinx.coroutines.flow.Flow

interface PomodoroRepository {
    // Настройки Pomodoro
    fun getPomodoroPreferences(): Flow<PomodoroPreferences>
    suspend fun updatePomodoroPreferences(preferences: PomodoroPreferences)

    // Сессии Pomodoro
    fun getAllSessions(): Flow<List<PomodoroSession>>
    fun getSessionsForTask(taskId: String): Flow<List<PomodoroSession>>
    fun getSessionsForTimeRange(startTime: Long, endTime: Long): Flow<List<PomodoroSession>>

    suspend fun startSession(taskId: String?, sessionType: PomodoroSessionType): String
    suspend fun completeSession(sessionId: String)
    suspend fun cancelSession(sessionId: String)
    suspend fun updateSessionNotes(sessionId: String, notes: String)

    suspend fun getTotalFocusTime(startDate: Long, endDate: Long): Int
    suspend fun getTotalFocusTimeForTask(taskId: String): Int
}