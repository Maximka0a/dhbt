package com.example.dhbt.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.dhbt.data.local.dao.CategoryDao
import com.example.dhbt.data.local.dao.HabitDao
import com.example.dhbt.data.local.dao.HabitFrequencyDao
import com.example.dhbt.data.local.dao.HabitTrackingDao
import com.example.dhbt.data.local.dao.NotificationDao
import com.example.dhbt.data.local.dao.PomodoroSessionDao
import com.example.dhbt.data.local.dao.QuoteDao
import com.example.dhbt.data.local.dao.StatisticSummaryDao
import com.example.dhbt.data.local.dao.SubtaskDao
import com.example.dhbt.data.local.dao.TagDao
import com.example.dhbt.data.local.dao.TaskDao
import com.example.dhbt.data.local.dao.TaskRecurrenceDao
import com.example.dhbt.data.local.dao.TaskTagDao
import com.example.dhbt.data.local.entity.CategoryEntity
import com.example.dhbt.data.local.entity.HabitEntity
import com.example.dhbt.data.local.entity.HabitFrequencyEntity
import com.example.dhbt.data.local.entity.HabitTrackingEntity
import com.example.dhbt.data.local.entity.NotificationEntity
import com.example.dhbt.data.local.entity.PomodoroSessionEntity
import com.example.dhbt.data.local.entity.QuoteEntity
import com.example.dhbt.data.local.entity.StatisticSummaryEntity
import com.example.dhbt.data.local.entity.SubtaskEntity
import com.example.dhbt.data.local.entity.TagEntity
import com.example.dhbt.data.local.entity.TaskEntity
import com.example.dhbt.data.local.entity.TaskRecurrenceEntity
import com.example.dhbt.data.local.entity.TaskTagCrossRef

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
                    .fallbackToDestructiveMigration(false)

                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}