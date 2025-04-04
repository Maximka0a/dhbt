package com.example.dhbt.presentation

import androidx.lifecycle.viewModelScope
import com.example.dhbt.domain.model.AppTheme
import com.example.dhbt.domain.model.StartScreen
import com.example.dhbt.domain.model.UserPreferences
import com.example.dhbt.domain.repository.UserPreferencesRepository
import com.example.dhbt.presentation.base.BaseViewModel
import com.example.dhbt.presentation.base.UiEvent
import com.example.dhbt.presentation.base.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : BaseViewModel<MainViewModel.MainState, MainViewModel.MainEvent>() {

    override fun createInitialState(): MainState = MainState(
        isLoading = true,
        hasCompletedOnboarding = false,
        userName = "",
        language = "ru",
        theme = AppTheme.SYSTEM,
        startScreen = StartScreen.DASHBOARD
    )

    init {
        checkInitialState()
        observeSettingsChanges()
    }

    private fun observeSettingsChanges() {
        viewModelScope.launch {
            userPreferencesRepository.getUserPreferences().collectLatest { preferences ->
                setState {
                    copy(
                        theme = preferences.theme,
                        language = preferences.language,
                        startScreen = preferences.startScreenType
                    )
                }
            }
        }

        viewModelScope.launch {
            userPreferencesRepository.getUserData().collectLatest { userData ->
                setState {
                    copy(
                        userName = userData.name,
                        userEmail = userData.email,
                        avatarUrl = userData.avatarUrl,
                        wakeUpTime = userData.wakeUpTime,
                        sleepTime = userData.sleepTime,
                        isPremium = userData.isPremium
                    )
                }
            }
        }
    }

    fun checkInitialState() {
        viewModelScope.launch {
            try {
                // Загружаем настройки пользователя
                val preferences = userPreferencesRepository.getUserPreferences().first()
                // Загружаем данные пользователя
                val userData = userPreferencesRepository.getUserData().first()

                setState {
                    copy(
                        isLoading = false,
                        hasCompletedOnboarding = preferences.hasCompletedOnboarding,
                        userName = userData.name,
                        userEmail = userData.email,
                        avatarUrl = userData.avatarUrl,
                        wakeUpTime = userData.wakeUpTime,
                        sleepTime = userData.sleepTime,
                        theme = preferences.theme,
                        language = preferences.language,
                        startScreen = preferences.startScreenType,
                        isPremium = userData.isPremium
                    )
                }

                // Send event to update language immediately if needed
                sendEvent(MainEvent.ApplyLanguage(preferences.language))
            } catch (e: Exception) {
                setState { copy(isLoading = false, error = e.message) }
                sendEvent(MainEvent.ShowError(e.message ?: "Произошла ошибка при загрузке данных"))
            }
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            try {
                userPreferencesRepository.markOnboardingCompleted()
                setState {
                    copy(
                        hasCompletedOnboarding = true
                    )
                }
            } catch (e: Exception) {
                sendEvent(MainEvent.ShowError(e.message ?: "Не удалось завершить онбординг"))
            }
        }
    }

    fun updateUserName(name: String) {
        viewModelScope.launch {
            try {
                userPreferencesRepository.updateUserName(name)
                setState { copy(userName = name) }
            } catch (e: Exception) {
                sendEvent(MainEvent.ShowError("Не удалось обновить имя пользователя"))
            }
        }
    }

    fun updateWakeupAndSleepTime(wakeUpTime: String?, sleepTime: String?) {
        viewModelScope.launch {
            try {
                userPreferencesRepository.updateWakeupAndSleepTime(wakeUpTime, sleepTime)
                setState { copy(wakeUpTime = wakeUpTime, sleepTime = sleepTime) }
            } catch (e: Exception) {
                sendEvent(MainEvent.ShowError("Не удалось обновить время пробуждения и сна"))
            }
        }
    }

    data class MainState(
        val isLoading: Boolean,
        val hasCompletedOnboarding: Boolean,
        val userName: String,
        val userEmail: String? = null,
        val avatarUrl: String? = null,
        val wakeUpTime: String? = null,
        val sleepTime: String? = null,
        val theme: AppTheme = AppTheme.SYSTEM,
        val language: String = "ru",
        val startScreen: StartScreen = StartScreen.DASHBOARD,
        val isPremium: Boolean = false,
        val error: String? = null
    ) : UiState

    sealed class MainEvent : UiEvent {
        data class ShowError(val message: String) : MainEvent()
        data class NavigateToScreen(val screen: String) : MainEvent()
        data class ApplyLanguage(val language: String) : MainEvent()
        object RefreshApp : MainEvent()
    }
}