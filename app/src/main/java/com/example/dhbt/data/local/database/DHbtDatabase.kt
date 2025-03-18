package com.example.dhbt.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.dhbt.data.local.dao.*
import com.example.dhbt.data.local.entity.*

@Database(
    entities = [
        TaskEntity::class,
        TaskRecurrenceEntity::class,
        SubtaskEntity::class,
        HabitEntity::class,
        HabitFrequencyEntity::class,
        HabitTrackingEntity::class,
        CategoryEntity::class,
        TagEntity::class,
        TaskTagCrossRef::class,
        PomodoroSessionEntity::class,
        NotificationEntity::class,
        StatisticSummaryEntity::class,
        QuoteEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class DHbtDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao
    abstract fun taskRecurrenceDao(): TaskRecurrenceDao
    abstract fun subtaskDao(): SubtaskDao
    abstract fun habitDao(): HabitDao
    abstract fun habitFrequencyDao(): HabitFrequencyDao
    abstract fun habitTrackingDao(): HabitTrackingDao
    abstract fun categoryDao(): CategoryDao
    abstract fun tagDao(): TagDao
    abstract fun taskTagDao(): TaskTagDao
    abstract fun pomodoroSessionDao(): PomodoroSessionDao
    abstract fun notificationDao(): NotificationDao
    abstract fun statisticSummaryDao(): StatisticSummaryDao
    abstract fun quoteDao(): QuoteDao

    companion object {
        @Volatile
        private var INSTANCE: DHbtDatabase? = null

        fun getInstance(context: Context): DHbtDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DHbtDatabase::class.java,
                    "dhbt_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}