package com.example.dhbt.data.local.entity

import androidx.room.Entity
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
    ]
)
data class HabitFrequencyEntity(
    @PrimaryKey
    val frequencyId: String = UUID.randomUUID().toString(),
    val habitId: String,
    val frequencyType: Int, // 0-ежедневно, 1-определенные дни недели, 2-X раз в неделю, 3-X раз в месяц
    val daysOfWeek: String? = null, // JSON-массив с днями недели
    val timesPerPeriod: Int? = null, // Количество раз для выполнения в период
    val periodType: Int? = null // 0-неделя, 1-месяц
)