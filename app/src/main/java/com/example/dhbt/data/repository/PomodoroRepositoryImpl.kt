package com.example.dhbt.data.repository

import androidx.datastore.core.DataStore
import com.example.dhbt.data.local.dao.PomodoroSessionDao
import com.example.dhbt.data.mapper.PomodoroSessionMapper
import com.example.dhbt.domain.model.PomodoroPreferences
import com.example.dhbt.domain.model.PomodoroSession
import com.example.dhbt.domain.model.PomodoroSessionType
import com.example.dhbt.domain.repository.PomodoroRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

class PomodoroRepositoryImpl @Inject constructor(
    private val pomodoroSessionDao: PomodoroSessionDao,
    private val pomodoroSessionMapper: PomodoroSessionMapper,
    private val pomodoroPreferencesStore: DataStore<PomodoroPreferences>
) : PomodoroRepository {

    override fun getPomodoroPreferences(): Flow<PomodoroPreferences> {
        return pomodoroPreferencesStore.data
    }

    override suspend fun updatePomodoroPreferences(preferences: PomodoroPreferences) {
        pomodoroPreferencesStore.updateData { preferences }
    }

    override fun getAllSessions(): Flow<List<PomodoroSession>> {
        return pomodoroSessionDao.getAllSessions().map { entities ->
            entities.map { pomodoroSessionMapper.mapFromEntity(it) }
        }
    }

    override fun getSessionsForTask(taskId: String): Flow<List<PomodoroSession>> {
        return pomodoroSessionDao.getSessionsForTask(taskId).map { entities ->
            entities.map { pomodoroSessionMapper.mapFromEntity(it) }
        }
    }

    override fun getSessionsForTimeRange(startTime: Long, endTime: Long): Flow<List<PomodoroSession>> {
        return pomodoroSessionDao.getSessionsInTimeRange(startTime, endTime).map { entities ->
            entities.map { pomodoroSessionMapper.mapFromEntity(it) }
        }
    }

    override suspend fun startSession(taskId: String?, sessionType: PomodoroSessionType): String {
        val currentTime = System.currentTimeMillis()

        // Получаем продолжительность сессии из настроек
        val preferences = pomodoroPreferencesStore.data.first()
        val duration = when(sessionType) {
            PomodoroSessionType.WORK -> preferences.workDuration
            PomodoroSessionType.SHORT_BREAK -> preferences.shortBreakDuration
            PomodoroSessionType.LONG_BREAK -> preferences.longBreakDuration
        }

        val sessionId = UUID.randomUUID().toString()
        val session = PomodoroSession(
            id = sessionId,
            taskId = taskId,
            startTime = currentTime,
            endTime = null,
            duration = duration,
            type = sessionType,
            isCompleted = false,
            notes = null
        )

        pomodoroSessionDao.insertSession(pomodoroSessionMapper.mapToEntity(session))
        return sessionId
    }

    override suspend fun completeSession(sessionId: String) {
        val currentTime = System.currentTimeMillis()
        pomodoroSessionDao.completeSession(sessionId, currentTime)
    }

    override suspend fun cancelSession(sessionId: String) {
        val sessions = pomodoroSessionDao.getAllSessions().first()
        val session = sessions.find { it.sessionId == sessionId } ?: return
        pomodoroSessionDao.deleteSession(session)
    }

    override suspend fun updateSessionNotes(sessionId: String, notes: String) {
        val sessions = pomodoroSessionDao.getAllSessions().first()
        val session = sessions.find { it.sessionId == sessionId } ?: return
        val updatedSession = session.copy(notes = notes)
        pomodoroSessionDao.updateSession(updatedSession)
    }

    override suspend fun getTotalFocusTime(startDate: Long, endDate: Long): Int {
        return pomodoroSessionDao.getTotalFocusTimeInRange(startDate, endDate) ?: 0
    }

    override suspend fun getTotalFocusTimeForTask(taskId: String): Int {
        return pomodoroSessionDao.getTotalFocusTimeForTask(taskId) ?: 0
    }
}