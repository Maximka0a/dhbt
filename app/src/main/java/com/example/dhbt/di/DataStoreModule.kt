package com.example.dhbt.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import com.example.dhbt.data.local.datastore.PomodoroPreferencesSerializer
import com.example.dhbt.data.local.datastore.UserDataSerializer
import com.example.dhbt.data.local.datastore.UserPreferencesSerializer
import com.example.dhbt.domain.model.UserData
import com.example.dhbt.domain.model.UserPreferences
import com.example.dhbt.domain.model.PomodoroPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// Extension property for user data DataStore
private val Context.userDataStore: DataStore<UserData> by dataStore(
    fileName = "user_data.json",
    serializer = UserDataSerializer
)

// Extension property for user preferences DataStore
private val Context.userPreferencesStore: DataStore<UserPreferences> by dataStore(
    fileName = "user_preferences.json",
    serializer = UserPreferencesSerializer
)

// Extension property for pomodoro preferences DataStore
private val Context.pomodoroPreferencesStore: DataStore<PomodoroPreferences> by dataStore(
    fileName = "pomodoro_preferences.json",
    serializer = PomodoroPreferencesSerializer
)

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun provideUserDataStore(@ApplicationContext context: Context): DataStore<UserData> {
        return context.userDataStore
    }

    @Provides
    @Singleton
    fun provideUserPreferencesStore(@ApplicationContext context: Context): DataStore<UserPreferences> {
        return context.userPreferencesStore
    }

    @Provides
    @Singleton
    fun providePomodoroPreferencesStore(@ApplicationContext context: Context): DataStore<PomodoroPreferences> {
        return context.pomodoroPreferencesStore
    }
}