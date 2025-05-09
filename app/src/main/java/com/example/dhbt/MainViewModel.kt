package com.example.dhbt.presentation

import androidx.lifecycle.viewModelScope
import com.example.dhbt.domain.model.AppTheme
import com.example.dhbt.domain.model.StartScreen
import com.example.dhbt.domain.model.UserPreferences
import com.example.dhbt.domain.repository.UserPreferencesRepository
import com.example.dhbt.presentation.base.BaseViewModel
import com.example.dhbt.presentation.base.UiEvent
import com.example.dhbt.presentation.base.UiState
import com.example.dhbt.util.LogUtils.logDetailed
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

private const val TAG = "MainViewModel"

/**
 * Основной ViewModel приложения, управляющий глобальным состоянием и конфигурациями.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    val userPreferencesRepository: UserPreferencesRepository // Сделаем публичным для доступа из MainActivity если потребуется
) : BaseViewModel<MainViewModel.MainState, MainViewModel.MainEvent>() {

    /**
     * Создает начальное состояние при инициализации ViewModel.
     * Обратите внимание, что состояние isLoading=true гарантирует отображение SplashScreen
     * до завершения загрузки настроек.
     */
    override fun createInitialState(): MainState = MainState(
        isLoading = true,
        hasCompletedOnboarding = false,
        userName = "",
        language = "ru",
        theme = AppTheme.SYSTEM,
        startScreen = StartScreen.DASHBOARD
    )

    init {
        // Запускаем параллельную загрузку всех данных и наблюдение за обновлениями
        loadInitialData()
        setupSettingsObservers()
    }

    /**
     * Асинхронно загружает все необходимые данные приложения.
     * Использует блокирующие операции, чтобы гарантировать полную загрузку перед отображением UI.
     */
    private fun loadInitialData() {
        viewModelScope.launch {
            try {
                Timber.tag(TAG).d("Начало загрузки данных приложения")

                // Логируем перед загрузкой
                Timber.tag(TAG).d("Текущее состояние перед загрузкой: isLoading=${state.value.isLoading}, hasCompletedOnboarding=${state.value.hasCompletedOnboarding}")

                // Используем асинхронную загрузку для оптимизации производительности
                val preferencesDeferred = async { userPreferencesRepository.getUserPreferences().first() }
                val userDataDeferred = async { userPreferencesRepository.getUserData().first() }

                // Дожидаемся завершения обеих операций
                val preferences = preferencesDeferred.await()
                val userData = userDataDeferred.await()

                // Выводим данные построчно
                Timber.tag(TAG).d("Загружены настройки (сводно): $preferences")
                preferences.logDetailed("PREFERENCES-DETAIL")

                Timber.tag(TAG).d("Загружены данные пользователя (сводно): $userData")
                userData.logDetailed("USER-DETAIL")

                // Обновляем состояние только один раз после загрузки всех данных
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

                // Логируем после обновления состояния
                Timber.tag(TAG).d("Состояние после загрузки: isLoading=${state.value.isLoading}, hasCompletedOnboarding=${state.value.hasCompletedOnboarding}")

                // Применяем языковые настройки
                sendEvent(MainEvent.ApplyLanguage(preferences.language))
                Timber.tag(TAG).d("Загрузка данных завершена успешно")
            } catch (e: Exception) {
                if (e is CancellationException) throw e

                Timber.tag(TAG).e(e, "Ошибка при загрузке данных")
                setState {
                    copy(
                        isLoading = false,
                        error = e.localizedMessage ?: "Неизвестная ошибка"
                    )
                }
                sendEvent(MainEvent.ShowError("Не удалось загрузить данные приложения"))
            }
        }
    }

    /**
     * Настраивает наблюдателей для отслеживания изменений в настройках пользователя.
     * Использует отдельные потоки для разных типов данных для оптимизации обновлений.
     */
    @OptIn(FlowPreview::class)
    private fun setupSettingsObservers() {
        // Наблюдаем за настройками с дебаунсом для предотвращения частых обновлений
        viewModelScope.launch {
            userPreferencesRepository.getUserPreferences()
                .debounce(300) // Предотвращаем слишком частые обновления
                .catch { e ->
                    Timber.tag(TAG).e(e, "Ошибка при наблюдении за настройками")
                }
                .collectLatest { preferences ->
                    // Логируем изменения настроек
                    preferences.logDetailed("PREFS-UPDATE")

                    setState {
                        copy(
                            theme = preferences.theme,
                            language = preferences.language,
                            startScreen = preferences.startScreenType,
                            hasCompletedOnboarding = preferences.hasCompletedOnboarding
                        )
                    }
                }
        }

        // Наблюдаем за данными пользователя
        viewModelScope.launch {
            userPreferencesRepository.getUserData()
                .debounce(300)
                .catch { e ->
                    Timber.tag(TAG).e(e, "Ошибка при наблюдении за данными пользователя")
                }
                .collectLatest { userData ->
                    // Логируем изменения данных пользователя
                    userData.logDetailed("USER-UPDATE")

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

    /**
     * Помечает онбординг как завершенный.
     * Эта операция является критической и обновляет как хранилище, так и состояние UI.
     */
    fun completeOnboarding() {
        viewModelScope.launch {
            try {
                Timber.tag(TAG).d("Завершение онбординга")

                // Получаем состояние до изменения
                val preferencesBeforeUpdate = userPreferencesRepository.getUserPreferences().first()
                Timber.tag(TAG).d("Состояние onboarding до обновления: ${preferencesBeforeUpdate.hasCompletedOnboarding}")

                userPreferencesRepository.markOnboardingCompleted()

                // Получаем состояние после изменения
                val preferencesAfterUpdate = userPreferencesRepository.getUserPreferences().first()
                Timber.tag(TAG).d("Состояние onboarding после обновления: ${preferencesAfterUpdate.hasCompletedOnboarding}")
                preferencesAfterUpdate.logDetailed("ONBOARDING-COMPLETE")

                setState { copy(hasCompletedOnboarding = true) }
                sendEvent(MainEvent.RefreshApp) // Обновляем приложение для применения изменений
            } catch (e: Exception) {
                if (e is CancellationException) throw e

                Timber.tag(TAG).e(e, "Ошибка при завершении онбординга")
                sendEvent(MainEvent.ShowError("Не удалось завершить ознакомление с приложением"))
            }
        }
    }

    /**
     * Обновляет имя пользователя в профиле.
     *
     * @param name Новое имя пользователя
     */
    fun updateUserName(name: String) {
        viewModelScope.launch {
            try {
                Timber.tag(TAG).d("Обновление имени пользователя: $name")
                userPreferencesRepository.updateUserName(name)
                setState { copy(userName = name) }
                sendEvent(MainEvent.ShowMessage("Имя успешно обновлено"))
            } catch (e: Exception) {
                if (e is CancellationException) throw e

                Timber.tag(TAG).e(e, "Ошибка при обновлении имени пользователя")
                sendEvent(MainEvent.ShowError("Не удалось обновить имя пользователя"))
            }
        }
    }

    /**
     * Обновляет время пробуждения и сна в профиле пользователя.
     *
     * @param wakeUpTime Время пробуждения в формате "HH:mm"
     * @param sleepTime Время отхода ко сну в формате "HH:mm"
     */
    fun updateWakeupAndSleepTime(wakeUpTime: String?, sleepTime: String?) {
        viewModelScope.launch {
            try {
                Timber.tag(TAG).d("Обновление времени пробуждения/сна: $wakeUpTime / $sleepTime")
                userPreferencesRepository.updateWakeupAndSleepTime(wakeUpTime, sleepTime)
                setState { copy(wakeUpTime = wakeUpTime, sleepTime = sleepTime) }
                sendEvent(MainEvent.ShowMessage("Расписание сохранено"))
            } catch (e: Exception) {
                if (e is CancellationException) throw e

                Timber.tag(TAG).e(e, "Ошибка при обновлении времени пробуждения/сна")
                sendEvent(MainEvent.ShowError("Не удалось обновить расписание"))
            }
        }
    }

    /**
     * Обновляет тему приложения.
     *
     * @param theme Новая тема приложения
     */
    fun updateTheme(theme: AppTheme) {
        viewModelScope.launch {
            try {
                userPreferencesRepository.updateTheme(theme.value)
                setState { copy(theme = theme) }
            } catch (e: Exception) {
                if (e is CancellationException) throw e

                Timber.tag(TAG).e(e, "Ошибка при обновлении темы")
                sendEvent(MainEvent.ShowError("Не удалось обновить тему приложения"))
            }
        }
    }

    /**
     * Обновляет язык приложения.
     *
     * @param languageCode Код языка (например, "ru", "en")
     */
    fun updateLanguage(languageCode: String) {
        viewModelScope.launch {
            try {
                // Синхронно обновляем настройки для быстрого сохранения
                userPreferencesRepository.updateLanguage(languageCode)

                // Обновляем состояние UI сразу же
                setState { copy(language = languageCode) }

                // Отправляем событие для пересоздания активити
                sendEvent(MainEvent.ApplyLanguage(languageCode))

                Timber.tag(TAG).d("Язык обновлен в настройках: $languageCode")
            } catch (e: Exception) {
                if (e is CancellationException) throw e

                Timber.tag(TAG).e(e, "Ошибка при обновлении языка")
                sendEvent(MainEvent.ShowError("Не удалось изменить язык приложения"))
            }
        }
    }
    /**
     * Обновляет стартовый экран приложения.
     *
     * @param startScreen Тип стартового экрана
     */
    fun updateStartScreen(startScreen: StartScreen) {
        viewModelScope.launch {
            try {
                userPreferencesRepository.updateStartScreen(startScreen.value)
                setState { copy(startScreen = startScreen) }
                sendEvent(MainEvent.ShowMessage("Стартовый экран изменен"))
            } catch (e: Exception) {
                if (e is CancellationException) throw e

                Timber.tag(TAG).e(e, "Ошибка при обновлении стартового экрана")
                sendEvent(MainEvent.ShowError("Не удалось изменить стартовый экран"))
            }
        }
    }

    /**
     * Состояние UI для главного экрана приложения.
     */
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

    /**
     * События UI для главного экрана приложения.
     */
    sealed class MainEvent : UiEvent {
        data class ShowError(val message: String) : MainEvent()
        data class ShowMessage(val message: String) : MainEvent()
        data class NavigateToScreen(val screen: String) : MainEvent()
        data class ApplyLanguage(val language: String) : MainEvent()
        object RefreshApp : MainEvent()
    }
}