package com.example.dhbt.di

import android.content.Context
import androidx.room.Room
import com.example.dhbt.data.local.dao.*
import com.example.dhbt.data.local.database.DHbtDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): DHbtDatabase {
        return Room.databaseBuilder(
            context,
            DHbtDatabase::class.java,
            "dhbt_database"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideTaskDao(database: DHbtDatabase): TaskDao = database.taskDao()

    @Provides
    fun provideTaskRecurrenceDao(database: DHbtDatabase): TaskRecurrenceDao = database.taskRecurrenceDao()

    @Provides
    fun provideSubtaskDao(database: DHbtDatabase): SubtaskDao = database.subtaskDao()

    @Provides
    fun provideHabitDao(database: DHbtDatabase): HabitDao = database.habitDao()

    @Provides
    fun provideHabitFrequencyDao(database: DHbtDatabase): HabitFrequencyDao = database.habitFrequencyDao()

    @Provides
    fun provideHabitTrackingDao(database: DHbtDatabase): HabitTrackingDao = database.habitTrackingDao()

    @Provides
    fun provideCategoryDao(database: DHbtDatabase): CategoryDao = database.categoryDao()

    @Provides
    fun provideTagDao(database: DHbtDatabase): TagDao = database.tagDao()

    @Provides
    fun provideTaskTagDao(database: DHbtDatabase): TaskTagDao = database.taskTagDao()

    @Provides
    fun providePomodoroSessionDao(database: DHbtDatabase): PomodoroSessionDao = database.pomodoroSessionDao()

    @Provides
    fun provideNotificationDao(database: DHbtDatabase): NotificationDao = database.notificationDao()

    @Provides
    fun provideStatisticSummaryDao(database: DHbtDatabase): StatisticSummaryDao = database.statisticSummaryDao()

    @Provides
    fun provideQuoteDao(database: DHbtDatabase): QuoteDao = database.quoteDao()
}