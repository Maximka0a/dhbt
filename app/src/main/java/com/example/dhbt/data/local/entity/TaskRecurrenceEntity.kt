package com.example.dhbt.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import java.util.UUID

@Entity(
    tableName = "task_recurrences",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["taskId"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("taskId")] // Добавленный индекс
)
data class TaskRecurrenceEntity(
    @PrimaryKey
    val recurrenceId: String = UUID.randomUUID().toString(),
    val taskId: String,
    val recurrenceType: Int,
    val daysOfWeek: String? = null,
    val monthDay: Int? = null,
    val customInterval: Int? = null,
    val startDate: Long,
    val endDate: Long? = null
)