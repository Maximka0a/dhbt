package com.example.dhbt.data.local.entity

import androidx.room.Entity
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
    ]
)
data class HabitTrackingEntity(
    @PrimaryKey
    val trackingId: String = UUID.randomUUID().toString(),
    val habitId: String,
    val date: Long,
    val isCompleted: Boolean = false,
    val value: Float? = null, // Значение для количественных привычек
    val duration: Int? = null, // Продолжительность для временных привычек в минутах
    val notes: String? = null // Заметки пользователя
)