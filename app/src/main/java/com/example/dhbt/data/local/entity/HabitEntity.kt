package com.example.dhbt.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import java.util.UUID

@Entity(
    tableName = "habits",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["categoryId"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class HabitEntity(
    @PrimaryKey
    val habitId: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String? = null,
    val iconEmoji: String? = null,
    val color: String? = null,
    val creationDate: Long = System.currentTimeMillis(),
    val habitType: Int, // 0-бинарный, 1-количественный, 2-временной
    val targetValue: Float? = null, // Целевое значение для количественных привычек
    val unitOfMeasurement: String? = null,
    val targetStreak: Int? = null,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val status: Int = 0, // 0-активная, 1-на паузе, 2-архивирована
    val pausedDate: Long? = null,
    val categoryId: String? = null
)