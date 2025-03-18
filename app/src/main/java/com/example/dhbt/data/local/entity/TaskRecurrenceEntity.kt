package com.example.dhbt.data.local.entity

import androidx.room.Entity
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
    ]
)
data class TaskRecurrenceEntity(
    @PrimaryKey
    val recurrenceId: String = UUID.randomUUID().toString(),
    val taskId: String,
    val recurrenceType: Int, // 0-ежедневно, 1-еженедельно, 2-ежемесячно, 3-ежегодно, 4-пользовательский
    val daysOfWeek: String? = null, // JSON-массив с днями недели
    val monthDay: Int? = null, // День месяца для ежемесячного повторения
    val customInterval: Int? = null, // Пользовательский интервал в днях
    val startDate: Long, // Дата начала повторений
    val endDate: Long? = null // Дата окончания повторений (опционально)
)