package com.example.dhbt.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import com.example.dhbt.data.local.entity.TaskEntity
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
    ]
)
data class PomodoroSessionEntity(
    @PrimaryKey
    val sessionId: String = UUID.randomUUID().toString(),
    val taskId: String? = null,
    val startTime: Long,
    val endTime: Long? = null,
    val duration: Int, // Продолжительность сессии в минутах
    val type: Int, // 0-работа, 1-короткий перерыв, 2-длинный перерыв
    val isCompleted: Boolean = false,
    val notes: String? = null
)