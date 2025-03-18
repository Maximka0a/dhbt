package com.example.dhbt.data.mapper

import com.example.dhbt.data.local.entity.PomodoroSessionEntity
import com.example.dhbt.domain.model.PomodoroSession
import com.example.dhbt.domain.model.PomodoroSessionType
import javax.inject.Inject

class PomodoroSessionMapper @Inject constructor() {

    fun mapFromEntity(entity: PomodoroSessionEntity): PomodoroSession {
        return PomodoroSession(
            id = entity.sessionId,
            taskId = entity.taskId,
            startTime = entity.startTime,
            endTime = entity.endTime,
            duration = entity.duration,
            type = PomodoroSessionType.fromInt(entity.type),
            isCompleted = entity.isCompleted,
            notes = entity.notes
        )
    }

    fun mapToEntity(domain: PomodoroSession): PomodoroSessionEntity {
        return PomodoroSessionEntity(
            sessionId = domain.id,
            taskId = domain.taskId,
            startTime = domain.startTime,
            endTime = domain.endTime,
            duration = domain.duration,
            type = domain.type.value,
            isCompleted = domain.isCompleted,
            notes = domain.notes
        )
    }
}