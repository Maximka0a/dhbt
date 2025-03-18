package com.example.dhbt.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import com.example.dhbt.data.local.data.model.PomodoroPreferences
import com.example.dhbt.data.local.data.model.UserData
import com.example.dhbt.data.local.data.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

val Context.userDataStore: DataStore<UserData> by dataStore(
    fileName = "user_data.json",
    serializer = UserDataSerializer
)

val Context.userPreferencesStore: DataStore<UserPreferences> by dataStore(
    fileName = "user_preferences.json",
    serializer = UserPreferencesSerializer
)

val Context.pomodoroPreferencesStore: DataStore<PomodoroPreferences> by dataStore(
    fileName = "pomodoro_preferences.json",
    serializer = PomodoroPreferencesSerializer
)

@Singleton
class DHbtDataStore @Inject constructor(private val context: Context) {

    val userData: Flow<UserData> = context.userDataStore.data
    val userPreferences: Flow<UserPreferences> = context.userPreferencesStore.data
    val pomodoroPreferences: Flow<PomodoroPreferences> = context.pomodoroPreferencesStore.data

    suspend fun updateUserData(transform: (UserData) -> UserData) {
        context.userDataStore.updateData(transform)
    }

    suspend fun updateUserPreferences(transform: (UserPreferences) -> UserPreferences) {
        context.userPreferencesStore.updateData(transform)
    }

    suspend fun updatePomodoroPreferences(transform: (PomodoroPreferences) -> PomodoroPreferences) {
        context.pomodoroPreferencesStore.updateData(transform)
    }
}