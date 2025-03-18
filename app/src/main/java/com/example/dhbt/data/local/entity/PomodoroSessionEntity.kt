package com.example.dhbt.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import java.util.UUID

@Entity(
    tableName = "pomodoro_sessions",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["taskId"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("taskId")] // Добавленный индекс
)
data class PomodoroSessionEntity(
    @PrimaryKey
    val sessionId: String = UUID.randomUUID().toString(),
    val taskId: String? = null,
    val startTime: Long,
    val endTime: Long? = null,
    val duration: Int,
    val type: Int,
    val isCompleted: Boolean = false,
    val notes: String? = null
)