package com.example.dhbt.di

import android.content.Context
import androidx.datastore.core.DataStore
import com.example.dhbt.data.local.datastore.DHbtDataStore
import com.example.dhbt.data.local.data.model.PomodoroPreferences
import com.example.dhbt.data.local.data.model.UserData
import com.example.dhbt.data.local.data.model.UserPreferences
import com.example.dhbt.data.local.datastore.pomodoroPreferencesStore
import com.example.dhbt.data.local.datastore.userDataStore
import com.example.dhbt.data.local.datastore.userPreferencesStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun provideDHbtDataStore(@ApplicationContext context: Context): DHbtDataStore {
        return DHbtDataStore(context)
    }

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