package com.example.dhbt.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import java.util.UUID

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["categoryId"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("categoryId")] // Добавленный индекс
)
data class TaskEntity(
    @PrimaryKey
    val taskId: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String? = null,
    val categoryId: String? = null,
    val color: String? = null,
    val creationDate: Long = System.currentTimeMillis(),
    val dueDate: Long? = null,
    val dueTime: String? = null, // "HH:MM"
    val duration: Int? = null, // В минутах
    val priority: Int = 1, // 0-низкий, 1-средний, 2-высокий
    val status: Int = 0, // 0-активная, 1-выполнена, 2-архивирована
    val completionDate: Long? = null,
    val eisenhowerQuadrant: Int? = null, // 1-4
    val estimatedPomodoroSessions: Int? = null
)