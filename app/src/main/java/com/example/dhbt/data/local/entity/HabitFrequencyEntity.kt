package com.example.dhbt.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import java.util.UUID

@Entity(
    tableName = "habit_frequencies",
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
data class HabitFrequencyEntity(
    @PrimaryKey
    val frequencyId: String = UUID.randomUUID().toString(),
    val habitId: String,
    val frequencyType: Int,
    val daysOfWeek: String? = null,
    val timesPerPeriod: Int? = null,
    val periodType: Int? = null
)