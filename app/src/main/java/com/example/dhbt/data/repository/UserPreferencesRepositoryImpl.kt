package com.example.dhbt.data.repository

import androidx.datastore.core.DataStore
import com.example.dhbt.domain.model.AppTheme
import com.example.dhbt.domain.model.StartScreen
import com.example.dhbt.domain.model.UserData
import com.example.dhbt.domain.model.UserPreferences
import com.example.dhbt.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject

private const val TAG = "UserPreferencesRepo"

class UserPreferencesRepositoryImpl @Inject constructor(
    private val userDataStore: DataStore<UserData>,
    private val userPreferencesStore: DataStore<UserPreferences>
) : UserPreferencesRepository {

    override fun getUserData(): Flow<UserData> {
        // Добавляем логирование каждого эмита данных
        return userDataStore.data.map { userData ->
            Timber.tag(TAG).d("Получены данные пользователя: $userData")
            userData
        }
    }

    override fun getUserPreferences(): Flow<UserPreferences> {
        // Добавляем логирование каждого эмита настроек
        return userPreferencesStore.data.map { preferences ->
            Timber.tag(TAG).d("Получены настройки пользователя: $preferences")
            preferences
        }
    }

    override suspend fun updateUserData(userData: UserData) {
        Timber.tag(TAG).d("Обновление данных пользователя: $userData")
        userDataStore.updateData { userData }
    }

    override suspend fun updateUserPreferences(userPreferences: UserPreferences) {
        Timber.tag(TAG).d("Обновление настроек пользователя: $userPreferences")
        userPreferencesStore.updateData { userPreferences }
    }

    override suspend fun updateUserName(name: String) {
        Timber.tag(TAG).d("Обновление имени пользователя: $name")
        userDataStore.updateData { currentData ->
            val updatedData = currentData.copy(name = name)
            Timber.tag(TAG).d("Новые данные после обновления имени: $updatedData")
            updatedData
        }
    }

    override suspend fun updateUserEmail(email: String) {
        Timber.tag(TAG).d("Обновление email пользователя: $email")
        userDataStore.updateData { currentData ->
            val updatedData = currentData.copy(email = email)
            Timber.tag(TAG).d("Новые данные после обновления email: $updatedData")
            updatedData
        }
    }

    override suspend fun updateUserAvatar(avatarUrl: String) {
        Timber.tag(TAG).d("Обновление аватара пользователя: $avatarUrl")
        userDataStore.updateData { currentData ->
            val updatedData = currentData.copy(avatarUrl = avatarUrl)
            Timber.tag(TAG).d("Новые данные после обновления аватара: $updatedData")
            updatedData
        }
    }

    override suspend fun updateWakeupAndSleepTime(wakeUpTime: String?, sleepTime: String?) {
        Timber.tag(TAG).d("Обновление времени пробуждения/сна: wakeUp=$wakeUpTime, sleep=$sleepTime")
        userDataStore.updateData { currentData ->
            val updatedData = currentData.copy(wakeUpTime = wakeUpTime, sleepTime = sleepTime)
            Timber.tag(TAG).d("Новые данные после обновления времени: $updatedData")
            updatedData
        }
    }

    override suspend fun updateStartScreen(screenType: Int) {
        Timber.tag(TAG).d("Обновление стартового экрана: $screenType")
        userPreferencesStore.updateData { currentPrefs ->
            val updatedPrefs = currentPrefs.copy(startScreenType = StartScreen.fromInt(screenType))
            Timber.tag(TAG).d("Новые настройки после обновления стартового экрана: $updatedPrefs")
            updatedPrefs
        }
    }

    override suspend fun updateTheme(theme: Int) {
        Timber.tag(TAG).d("Обновление темы: $theme")
        userPreferencesStore.updateData { currentPrefs ->
            val updatedPrefs = currentPrefs.copy(theme = AppTheme.fromInt(theme))
            Timber.tag(TAG).d("Новые настройки после обновления темы: $updatedPrefs")
            updatedPrefs
        }
    }

    override suspend fun updateLanguage(language: String) {
        Timber.tag(TAG).d("Обновление языка: $language")
        userPreferencesStore.updateData { currentPrefs ->
            val updatedPrefs = currentPrefs.copy(language = language)
            Timber.tag(TAG).d("Новые настройки после обновления языка: $updatedPrefs")
            updatedPrefs
        }
    }

    override suspend fun updateQuietHours(enabled: Boolean, start: String?, end: String?) {
        Timber.tag(TAG).d("Обновление тихих часов: enabled=$enabled, start=$start, end=$end")
        userPreferencesStore.updateData { currentPrefs ->
            val updatedPrefs = currentPrefs.copy(
                quietHoursEnabled = enabled,
                quietHoursStart = start,
                quietHoursEnd = end
            )
            Timber.tag(TAG).d("Новые настройки после обновления тихих часов: $updatedPrefs")
            updatedPrefs
        }
    }

    override suspend fun markOnboardingCompleted() {
        Timber.tag(TAG).d("Отметка онбординга как завершенного")
        val beforeUpdate = userPreferencesStore.data.first()
        Timber.tag(TAG).d("Состояние onboarding до обновления: ${beforeUpdate.hasCompletedOnboarding}")

        userPreferencesStore.updateData { currentPrefs ->
            val updatedPrefs = currentPrefs.copy(hasCompletedOnboarding = true)
            Timber.tag(TAG).d("Новые настройки после завершения онбординга: $updatedPrefs")
            updatedPrefs
        }

        // Проверяем, что изменение действительно применилось
        val afterUpdate = userPreferencesStore.data.first()
        Timber.tag(TAG).d("Состояние onboarding после обновления: ${afterUpdate.hasCompletedOnboarding}")
    }

    override suspend fun clearUserData() {
        Timber.tag(TAG).d("Очистка данных пользователя")
        val oldUserData = userDataStore.data.first()
        val oldPreferences = userPreferencesStore.data.first()

        Timber.tag(TAG).d("Данные до очистки: $oldUserData")
        Timber.tag(TAG).d("Настройки до очистки: $oldPreferences")

        // Сбрасываем данные пользователя к значениям по умолчанию
        val defaultUserData = UserData(
            userId = oldUserData.userId, // Сохраняем ID пользователя
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
        Timber.tag(TAG).d("Новые данные после очистки: $defaultUserData")

        // Сбрасываем настройки пользователя к значениям по умолчанию, но сохраняем язык
        val language = oldPreferences.language
        val defaultPreferences = UserPreferences(language = language)
        userPreferencesStore.updateData { defaultPreferences }
        Timber.tag(TAG).d("Новые настройки после очистки: $defaultPreferences")
    }
}