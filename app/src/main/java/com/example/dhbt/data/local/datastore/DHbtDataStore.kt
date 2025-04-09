package com.example.dhbt.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import com.example.dhbt.domain.model.PomodoroPreferences
import com.example.dhbt.domain.model.UserData
import com.example.dhbt.domain.model.UserPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DHbtDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userDataStore: DataStore<UserData>,
    private val userPreferencesStore: DataStore<UserPreferences>,
    private val pomodoroPreferencesStore: DataStore<PomodoroPreferences>
) {
    val userData: Flow<UserData> = userDataStore.data
    val userPreferences: Flow<UserPreferences> = userPreferencesStore.data
    val pomodoroPreferences: Flow<PomodoroPreferences> = pomodoroPreferencesStore.data

    suspend fun updateUserData(transform: (UserData) -> UserData) {
        userDataStore.updateData(transform)
    }

    suspend fun updateUserPreferences(transform: (UserPreferences) -> UserPreferences) {
        userPreferencesStore.updateData(transform)
    }

    suspend fun updatePomodoroPreferences(transform: (PomodoroPreferences) -> PomodoroPreferences) {
        pomodoroPreferencesStore.updateData(transform)
    }
}