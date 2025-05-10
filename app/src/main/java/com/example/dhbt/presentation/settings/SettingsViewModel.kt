package com.example.dhbt.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dhbt.domain.model.AppTheme
import com.example.dhbt.domain.model.Category
import com.example.dhbt.domain.model.PomodoroPreferences
import com.example.dhbt.domain.model.UserData
import com.example.dhbt.domain.model.UserPreferences
import com.example.dhbt.domain.repository.CategoryRepository
import com.example.dhbt.domain.repository.PomodoroRepository
import com.example.dhbt.domain.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

private const val TAG = "SettingsViewModel"

/**
 * ViewModel для экрана настроек приложения
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val categoryRepository: CategoryRepository,
    private val pomodoroRepository: PomodoroRepository
) : ViewModel() {

    // Состояние UI с признаком загрузки и ошибками
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    // События для одноразового оповещения UI
    private val _events = MutableSharedFlow<SettingsEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    // Данные пользователя из репозитория с дефолтным значением для быстрой инициализации
    val userData: StateFlow<UserData> = userPreferencesRepository.getUserData()
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserData())

    // Настройки пользователя из репозитория
    val userPreferences: StateFlow<UserPreferences> = userPreferencesRepository.getUserPreferences()
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserPreferences())

    // Настройки Pomodoro
    val pomodoroPreferences: StateFlow<PomodoroPreferences> = pomodoroRepository.getPomodoroPreferences()
        .stateIn(viewModelScope, SharingStarted.Eagerly, PomodoroPreferences())

    // Категории для задач и привычек
    val categories: StateFlow<List<Category>> = categoryRepository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Состояния диалогов - используем однократную переменную для всех
    private val _showDeleteDialog = MutableStateFlow(false)
    val showDeleteDialog = _showDeleteDialog.asStateFlow()

    private val _showLanguageDialog = MutableStateFlow(false)
    val showLanguageDialog = _showLanguageDialog.asStateFlow()

    private val _showCategoryDialog = MutableStateFlow(false)
    val showCategoryDialog = _showCategoryDialog.asStateFlow()

    private val _showThemeDialog = MutableStateFlow(false)
    val showThemeDialog = _showThemeDialog.asStateFlow()

    private val _showQuietHoursDialog = MutableStateFlow(false)
    val showQuietHoursDialog = _showQuietHoursDialog.asStateFlow()

    // Сообщения об успешных операциях и ошибках
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage = _successMessage.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    // Редактируемая категория для диалога
    private val _editCategory = MutableStateFlow<Category?>(null)
    val editCategory = _editCategory.asStateFlow()

    // Список поддерживаемых языков
    val availableLanguages = listOf(
        LanguageOption("ru", "Русский"),
        LanguageOption("en", "English"),
        LanguageOption("de", "Deutsch"),
        LanguageOption("fr", "Français"),
        LanguageOption("es", "Español")
    )

    init {
        loadSettings()
    }

    /**
     * Загружает начальные настройки
     */
    private fun loadSettings() {
        viewModelScope.launch {
            try {
                Timber.d("Загрузка начальных настроек")
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Timber.e(e, "Ошибка загрузки настроек")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Ошибка загрузки настроек: ${e.message}"
                )
            }
        }
    }

    /**
     * Обрабатывает действия пользователя на экране настроек
     */
    fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.UpdateTheme -> updateTheme(action.themeMode)
            is SettingsAction.UpdateLanguage -> updateLanguage(action.language)
            is SettingsAction.UpdateStartScreen -> updateStartScreen(action.screenType)
            is SettingsAction.UpdateDefaultTaskView -> updateDefaultTaskView(action.viewType)
            is SettingsAction.UpdateDefaultTaskSort -> updateDefaultTaskSort(action.sortType)
            is SettingsAction.UpdateDefaultHabitSort -> updateDefaultHabitSort(action.sortType)
            is SettingsAction.UpdateReminderTime -> updateReminderTime(action.minutes)
            is SettingsAction.UpdateSoundEnabled -> updateSoundEnabled(action.enabled)
            is SettingsAction.UpdateVibrationEnabled -> updateVibrationEnabled(action.enabled)
            is SettingsAction.UpdateQuietHours -> updateQuietHours(action.enabled, action.start, action.end)
            is SettingsAction.UpdateCloudSync -> updateCloudSync(action.enabled)
            is SettingsAction.UpdateUserName -> updateUserName(action.name)
            is SettingsAction.UpdateUserEmail -> updateUserEmail(action.email)
            is SettingsAction.UpdateUserAvatar -> updateUserAvatar(action.avatarUrl)
            is SettingsAction.UpdateWakeupSleepTime -> updateWakeupSleepTime(action.wakeupTime, action.sleepTime)
            is SettingsAction.AddCategory -> addCategory(action.category)
            is SettingsAction.UpdateCategory -> updateCategory(action.category)
            is SettingsAction.DeleteCategory -> deleteCategory(action.categoryId)
            is SettingsAction.StartEditCategory -> startEditCategory(action.category)
            is SettingsAction.ClearEditCategory -> _editCategory.value = null
            is SettingsAction.UpdatePomodoroSettings -> updatePomodoroSettings(action.settings)
            is SettingsAction.ResetAllData -> resetAllData()
            is SettingsAction.ToggleDeleteDialog -> _showDeleteDialog.value = action.show
            is SettingsAction.ToggleLanguageDialog -> _showLanguageDialog.value = action.show
            is SettingsAction.ToggleCategoryDialog -> _showCategoryDialog.value = action.show
            is SettingsAction.ToggleThemeDialog -> _showThemeDialog.value = action.show
            is SettingsAction.ToggleQuietHoursDialog -> _showQuietHoursDialog.value = action.show
            is SettingsAction.DismissMessage -> _successMessage.value = null
            is SettingsAction.DismissError -> _errorMessage.value = null
        }
    }

    /**
     * Обновляет тему приложения
     */
    private fun updateTheme(themeMode: Int) {
        executeOperation(
            operation = { userPreferencesRepository.updateTheme(themeMode) },
            successMessage = "Тема оформления обновлена",
            errorMessage = "Не удалось обновить тему",
            onSuccess = {
                viewModelScope.launch {
                    _events.emit(SettingsEvent.ThemeChanged(AppTheme.fromInt(themeMode)))
                }
            }
        )
    }

    /**
     * Обновляет язык приложения
     */
    private fun updateLanguage(language: String) {
        viewModelScope.launch {
            try {
                // Проверка текущего языка для избежания лишних обновлений
                val currentLanguage = userPreferencesRepository.getUserPreferences().first().language
                if (currentLanguage == language) {
                    _successMessage.value = "Язык уже установлен"
                    return@launch
                }

                // Обновление настроек и уведомление о смене языка
                userPreferencesRepository.updateLanguage(language)
                _events.emit(SettingsEvent.LanguageChanged(language))

                // Небольшая задержка перед показом сообщения
                delay(300)
                _successMessage.value = "Язык обновлен и применен"
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Timber.e(e, "Ошибка обновления языка")
                _errorMessage.value = "Не удалось обновить язык: ${e.message}"
            }
        }
    }

    /**
     * Обновляет стартовый экран приложения
     */
    private fun updateStartScreen(screenType: Int) {
        executeOperation(
            operation = { userPreferencesRepository.updateStartScreen(screenType) },
            successMessage = "Стартовый экран обновлен",
            errorMessage = "Не удалось обновить стартовый экран",
            onSuccess = {
                viewModelScope.launch {
                    _events.emit(SettingsEvent.StartScreenChanged(screenType))
                }
            }
        )
    }

    /**
     * Обновляет тип отображения задач по умолчанию
     */
    private fun updateDefaultTaskView(viewType: Int) {
        viewModelScope.launch {
            try {
                val preferences = userPreferencesRepository.getUserPreferences().first()
                val newPreferences = preferences.copy(
                    defaultTaskView = com.example.dhbt.domain.model.TaskView.fromInt(viewType)
                )
                userPreferencesRepository.updateUserPreferences(newPreferences)
                _successMessage.value = "Тип отображения задач обновлен"
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Timber.e(e, "Ошибка обновления типа отображения задач")
                _errorMessage.value = "Не удалось обновить тип отображения задач: ${e.message}"
            }
        }
    }

    /**
     * Обновляет сортировку задач по умолчанию
     */
    private fun updateDefaultTaskSort(sortType: Int) {
        viewModelScope.launch {
            try {
                val preferences = userPreferencesRepository.getUserPreferences().first()
                val newPreferences = preferences.copy(
                    defaultTaskSort = com.example.dhbt.domain.model.TaskSort.fromInt(sortType)
                )
                userPreferencesRepository.updateUserPreferences(newPreferences)
                _successMessage.value = "Сортировка задач обновлена"
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Timber.e(e, "Ошибка обновления сортировки задач")
                _errorMessage.value = "Не удалось обновить сортировку задач: ${e.message}"
            }
        }
    }

    /**
     * Обновляет сортировку привычек по умолчанию
     */
    private fun updateDefaultHabitSort(sortType: Int) {
        viewModelScope.launch {
            try {
                val preferences = userPreferencesRepository.getUserPreferences().first()
                val newPreferences = preferences.copy(
                    defaultHabitSort = com.example.dhbt.domain.model.HabitSort.fromInt(sortType)
                )
                userPreferencesRepository.updateUserPreferences(newPreferences)
                _successMessage.value = "Сортировка привычек обновлена"
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Timber.e(e, "Ошибка обновления сортировки привычек")
                _errorMessage.value = "Не удалось обновить сортировку привычек: ${e.message}"
            }
        }
    }

    /**
     * Обновляет время напоминания о задачах
     */
    private fun updateReminderTime(minutes: Int) {
        viewModelScope.launch {
            try {
                val preferences = userPreferencesRepository.getUserPreferences().first()
                val newPreferences = preferences.copy(
                    reminderTimeBeforeTask = minutes
                )
                userPreferencesRepository.updateUserPreferences(newPreferences)
                _successMessage.value = "Время напоминания обновлено"
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Timber.e(e, "Ошибка обновления времени напоминания")
                _errorMessage.value = "Не удалось обновить время напоминания: ${e.message}"
            }
        }
    }

    /**
     * Включает/отключает звук уведомлений
     */
    private fun updateSoundEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                val preferences = userPreferencesRepository.getUserPreferences().first()
                val newPreferences = preferences.copy(
                    defaultSoundEnabled = enabled
                )
                userPreferencesRepository.updateUserPreferences(newPreferences)
                _successMessage.value = if (enabled) "Звук включен" else "Звук отключен"
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Timber.e(e, "Ошибка обновления настроек звука")
                _errorMessage.value = "Не удалось обновить настройки звука: ${e.message}"
            }
        }
    }

    /**
     * Включает/отключает вибрацию для уведомлений
     */
    private fun updateVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                val preferences = userPreferencesRepository.getUserPreferences().first()
                val newPreferences = preferences.copy(
                    defaultVibrationEnabled = enabled
                )
                userPreferencesRepository.updateUserPreferences(newPreferences)
                _successMessage.value = if (enabled) "Вибрация включена" else "Вибрация отключена"
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Timber.e(e, "Ошибка обновления настроек вибрации")
                _errorMessage.value = "Не удалось обновить настройки вибрации: ${e.message}"
            }
        }
    }

    /**
     * Обновляет настройки тихих часов
     */
    private fun updateQuietHours(enabled: Boolean, start: String?, end: String?) {
        executeOperation(
            operation = { userPreferencesRepository.updateQuietHours(enabled, start, end) },
            successMessage = if (enabled) "Тихие часы настроены" else "Тихие часы отключены",
            errorMessage = "Не удалось обновить настройки тихих часов"
        )
    }

    /**
     * Обновляет настройки облачной синхронизации
     */
    private fun updateCloudSync(enabled: Boolean) {
        viewModelScope.launch {
            try {
                val preferences = userPreferencesRepository.getUserPreferences().first()
                val newPreferences = preferences.copy(
                    cloudSyncEnabled = enabled
                )
                userPreferencesRepository.updateUserPreferences(newPreferences)
                _successMessage.value = if (enabled) "Облачная синхронизация включена" else "Облачная синхронизация отключена"
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Timber.e(e, "Ошибка обновления настроек синхронизации")
                _errorMessage.value = "Не удалось обновить настройки синхронизации: ${e.message}"
            }
        }
    }

    /**
     * Обновляет имя пользователя
     */
    private fun updateUserName(name: String) {
        executeOperation(
            operation = { userPreferencesRepository.updateUserName(name) },
            successMessage = "Имя пользователя обновлено",
            errorMessage = "Не удалось обновить имя пользователя"
        )
    }

    /**
     * Обновляет email пользователя
     */
    private fun updateUserEmail(email: String) {
        executeOperation(
            operation = { userPreferencesRepository.updateUserEmail(email) },
            successMessage = "Email пользователя обновлен",
            errorMessage = "Не удалось обновить email пользователя"
        )
    }

    /**
     * Обновляет URL аватара пользователя
     */
    private fun updateUserAvatar(avatarUrl: String) {
        executeOperation(
            operation = { userPreferencesRepository.updateUserAvatar(avatarUrl) },
            successMessage = "Аватар пользователя обновлен",
            errorMessage = "Не удалось обновить аватар пользователя"
        )
    }

    /**
     * Обновляет время пробуждения и сна
     */
    private fun updateWakeupSleepTime(wakeupTime: String?, sleepTime: String?) {
        executeOperation(
            operation = { userPreferencesRepository.updateWakeupAndSleepTime(wakeupTime, sleepTime) },
            successMessage = "Время пробуждения и сна обновлены",
            errorMessage = "Не удалось обновить время пробуждения и сна"
        )
    }

    /**
     * Добавляет новую категорию
     */
    private fun addCategory(category: Category) {
        executeOperation(
            operation = { categoryRepository.addCategory(category) },
            successMessage = "Категория добавлена",
            errorMessage = "Не удалось добавить категорию",
            onSuccess = { _showCategoryDialog.value = false }
        )
    }

    /**
     * Обновляет существующую категорию
     */
    private fun updateCategory(category: Category) {
        executeOperation(
            operation = { categoryRepository.updateCategory(category) },
            successMessage = "Категория обновлена",
            errorMessage = "Не удалось обновить категорию",
            onSuccess = {
                _showCategoryDialog.value = false
                _editCategory.value = null
            }
        )
    }

    /**
     * Удаляет категорию если она не используется
     */
    private fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            try {
                // Проверяем, используется ли категория
                val tasksCount = categoryRepository.getTaskCountInCategory(categoryId)
                val habitsCount = categoryRepository.getHabitCountInCategory(categoryId)

                if (tasksCount > 0 || habitsCount > 0) {
                    _errorMessage.value = "Нельзя удалить категорию, которая используется в задачах или привычках"
                    return@launch
                }

                // Удаляем категорию если она не используется
                categoryRepository.deleteCategory(categoryId)
                _successMessage.value = "Категория удалена"
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Timber.e(e, "Ошибка удаления категории")
                _errorMessage.value = "Не удалось удалить категорию: ${e.message}"
            }
        }
    }

    /**
     * Начинает редактирование категории
     */
    private fun startEditCategory(category: Category) {
        _editCategory.value = category
        _showCategoryDialog.value = true
    }

    /**
     * Обновляет настройки Pomodoro
     */
    private fun updatePomodoroSettings(settings: PomodoroPreferences) {
        executeOperation(
            operation = { pomodoroRepository.updatePomodoroPreferences(settings) },
            successMessage = "Настройки Pomodoro обновлены",
            errorMessage = "Не удалось обновить настройки Pomodoro"
        )
    }

    /**
     * Сбрасывает все данные приложения и настройки
     */
    private fun resetAllData() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)

                    userPreferencesRepository.clearUserData()

                _uiState.value = _uiState.value.copy(isLoading = false)
                _successMessage.value = "Все данные сброшены"
                _showDeleteDialog.value = false

                // Отправляем событие о завершении сброса данных
                _events.emit(SettingsEvent.SettingsResetComplete)

                // Добавляем событие перезапуска приложения для применения изменений
                _events.emit(SettingsEvent.RestartApp)
            } catch (e: Exception) {
                if (e is CancellationException) throw e

                Timber.e(e, "Ошибка сброса данных")
                _uiState.value = _uiState.value.copy(isLoading = false)
                _errorMessage.value = "Не удалось сбросить данные: ${e.message}"
            }
        }
    }

    /**
     * Универсальный обработчик операций с репозиториями для унификации кода
     */
    private fun executeOperation(
        operation: suspend () -> Unit,
        successMessage: String,
        errorMessage: String,
        onSuccess: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            try {
                operation()
                _successMessage.value = successMessage
                onSuccess?.invoke()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Timber.e(e, errorMessage)
                _errorMessage.value = "$errorMessage: ${e.message}"
            }
        }
    }
}

/**
 * Состояние UI экрана настроек
 */
data class SettingsUiState(
    val isLoading: Boolean = true,
    val error: String? = null
)

/**
 * Опция языка для выбора в настройках
 */
data class LanguageOption(
    val code: String,
    val displayName: String
)

/**
 * События для уведомления UI об изменениях
 */
sealed class SettingsEvent {
    data class LanguageChanged(val language: String) : SettingsEvent()
    data class StartScreenChanged(val screenType: Int) : SettingsEvent()
    data class ThemeChanged(val theme: AppTheme) : SettingsEvent()
    object SettingsResetComplete : SettingsEvent()
    object RestartApp : SettingsEvent() // Новое событие для перезапуска приложения
}

/**
 * Действия, которые могут быть выполнены пользователем на экране настроек
 */
sealed class SettingsAction {
    data class UpdateTheme(val themeMode: Int) : SettingsAction()
    data class UpdateLanguage(val language: String) : SettingsAction()
    data class UpdateStartScreen(val screenType: Int) : SettingsAction()
    data class UpdateDefaultTaskView(val viewType: Int) : SettingsAction()
    data class UpdateDefaultTaskSort(val sortType: Int) : SettingsAction()
    data class UpdateDefaultHabitSort(val sortType: Int) : SettingsAction()
    data class UpdateReminderTime(val minutes: Int) : SettingsAction()
    data class UpdateSoundEnabled(val enabled: Boolean) : SettingsAction()
    data class UpdateVibrationEnabled(val enabled: Boolean) : SettingsAction()
    data class UpdateQuietHours(val enabled: Boolean, val start: String?, val end: String?) : SettingsAction()
    data class UpdateCloudSync(val enabled: Boolean) : SettingsAction()
    data class UpdateUserName(val name: String) : SettingsAction()
    data class UpdateUserEmail(val email: String) : SettingsAction()
    data class UpdateUserAvatar(val avatarUrl: String) : SettingsAction()
    data class UpdateWakeupSleepTime(val wakeupTime: String?, val sleepTime: String?) : SettingsAction()
    data class AddCategory(val category: Category) : SettingsAction()
    data class UpdateCategory(val category: Category) : SettingsAction()
    data class DeleteCategory(val categoryId: String) : SettingsAction()
    data class StartEditCategory(val category: Category) : SettingsAction()
    object ClearEditCategory : SettingsAction()
    data class UpdatePomodoroSettings(val settings: PomodoroPreferences) : SettingsAction()
    object ResetAllData : SettingsAction()
    data class ToggleDeleteDialog(val show: Boolean) : SettingsAction()
    data class ToggleLanguageDialog(val show: Boolean) : SettingsAction()
    data class ToggleCategoryDialog(val show: Boolean) : SettingsAction()
    data class ToggleThemeDialog(val show: Boolean) : SettingsAction()
    data class ToggleQuietHoursDialog(val show: Boolean) : SettingsAction()
    object DismissMessage : SettingsAction()
    object DismissError : SettingsAction()
}