package com.example.dhbt.domain.repository

import com.example.dhbt.domain.model.UserData
import com.example.dhbt.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    fun getUserData(): Flow<UserData>
    fun getUserPreferences(): Flow<UserPreferences>

    suspend fun updateUserData(userData: UserData)
    suspend fun updateUserPreferences(userPreferences: UserPreferences)

    suspend fun updateUserName(name: String)
    suspend fun updateUserEmail(email: String)
    suspend fun updateUserAvatar(avatarUrl: String)
    suspend fun updateWakeupAndSleepTime(wakeUpTime: String?, sleepTime: String?)

    suspend fun updateStartScreen(screenType: Int)
    suspend fun updateTheme(theme: Int)
    suspend fun updateLanguage(language: String)
    suspend fun updateQuietHours(enabled: Boolean, start: String?, end: String?)

    suspend fun markOnboardingCompleted()
    suspend fun clearUserData()
}