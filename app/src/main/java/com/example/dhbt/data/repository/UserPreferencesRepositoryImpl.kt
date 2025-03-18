package com.example.dhbt.data.repository

import androidx.datastore.core.DataStore
import com.example.dhbt.domain.model.AppTheme
import com.example.dhbt.domain.model.StartScreen
import com.example.dhbt.domain.model.UserData
import com.example.dhbt.domain.model.UserPreferences
import com.example.dhbt.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class UserPreferencesRepositoryImpl @Inject constructor(
    private val userDataStore: DataStore<UserData>,
    private val userPreferencesStore: DataStore<UserPreferences>
) : UserPreferencesRepository {

    override fun getUserData(): Flow<UserData> {
        return userDataStore.data
    }

    override fun getUserPreferences(): Flow<UserPreferences> {
        return userPreferencesStore.data
    }

    override suspend fun updateUserData(userData: UserData) {
        userDataStore.updateData { userData }
    }

    override suspend fun updateUserPreferences(userPreferences: UserPreferences) {
        userPreferencesStore.updateData { userPreferences }
    }

    override suspend fun updateUserName(name: String) {
        userDataStore.updateData { currentData ->
            currentData.copy(name = name)
        }
    }

    override suspend fun updateUserEmail(email: String) {
        userDataStore.updateData { currentData ->
            currentData.copy(email = email)
        }
    }

    override suspend fun updateUserAvatar(avatarUrl: String) {
        userDataStore.updateData { currentData ->
            currentData.copy(avatarUrl = avatarUrl)
        }
    }

    override suspend fun updateWakeupAndSleepTime(wakeUpTime: String?, sleepTime: String?) {
        userDataStore.updateData { currentData ->
            currentData.copy(wakeUpTime = wakeUpTime, sleepTime = sleepTime)
        }
    }

    override suspend fun updateStartScreen(screenType: Int) {
        userPreferencesStore.updateData { currentPrefs ->
            currentPrefs.copy(startScreenType = StartScreen.fromInt(screenType))
        }
    }

    override suspend fun updateTheme(theme: Int) {
        userPreferencesStore.updateData { currentPrefs ->
            currentPrefs.copy(theme = AppTheme.fromInt(theme))
        }
    }

    override suspend fun updateLanguage(language: String) {
        userPreferencesStore.updateData { currentPrefs ->
            currentPrefs.copy(language = language)
        }
    }

    override suspend fun updateQuietHours(enabled: Boolean, start: String?, end: String?) {
        userPreferencesStore.updateData { currentPrefs ->
            currentPrefs.copy(
                quietHoursEnabled = enabled,
                quietHoursStart = start,
                quietHoursEnd = end
            )
        }
    }

    override suspend fun markOnboardingCompleted() {
        userPreferencesStore.updateData { currentPrefs ->
            currentPrefs.copy(hasCompletedOnboarding = true)
        }
    }

    override suspend fun clearUserData() {
        // Сбрасываем данные пользователя к значениям по умолчанию
        val defaultUserData = UserData(
            userId = userDataStore.data.first().userId, // Сохраняем ID пользователя
            name = "",
            email = null,
            avatarUrl = null,
            wakeUpTime = null,
            sleepTime = null,
            usageGoal = null,
            isPremium = false,
            premiumExpiryDate = null,
            customEmoji = null,
            registrationDate = System.currentTimeMillis(),
            lastLoginDate = System.currentTimeMillis()
        )

        userDataStore.updateData { defaultUserData }

        // Сбрасываем настройки пользователя к значениям по умолчанию, но сохраняем язык
        val language = userPreferencesStore.data.first().language
        val defaultPreferences = UserPreferences(language = language)
        userPreferencesStore.updateData { defaultPreferences }
    }
}