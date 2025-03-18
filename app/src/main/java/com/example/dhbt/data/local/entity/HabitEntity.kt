package com.example.dhbt.data.local.entity

import androidx.room.Entity
import androidx.room.Index
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
    ],
    indices = [Index("categoryId")] // Добавленный индекс
)
data class HabitEntity(
    @PrimaryKey
    val habitId: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String? = null,
    val iconEmoji: String? = null,
    val color: String? = null,
    val creationDate: Long = System.currentTimeMillis(),
    val habitType: Int,
    val targetValue: Float? = null,
    val unitOfMeasurement: String? = null,
    val targetStreak: Int? = null,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val status: Int = 0,
    val pausedDate: Long? = null,
    val categoryId: String? = null
)