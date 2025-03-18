package com.example.dhbt.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import java.util.UUID

@Entity(
    tableName = "habit_trackings",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["habitId"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("habitId")] // Добавленный индекс
)
data class HabitTrackingEntity(
    @PrimaryKey
    val trackingId: String = UUID.randomUUID().toString(),
    val habitId: String,
    val date: Long,
    val isCompleted: Boolean = false,
    val value: Float? = null,
    val duration: Int? = null,
    val notes: String? = null
)