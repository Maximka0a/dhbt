package com.example.dhbt.presentation.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dhbt.domain.model.AppTheme
import com.example.dhbt.domain.model.Category
import com.example.dhbt.domain.model.CategoryType
import com.example.dhbt.domain.model.PomodoroPreferences
import com.example.dhbt.domain.model.StartScreen
import com.example.dhbt.domain.model.UserData
import com.example.dhbt.domain.model.UserPreferences
import com.example.dhbt.domain.repository.CategoryRepository
import com.example.dhbt.domain.repository.PomodoroRepository
import com.example.dhbt.domain.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val categoryRepository: CategoryRepository,
    private val pomodoroRepository: PomodoroRepository
) : ViewModel() {

    // Состояние UI
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SettingsEvent>()
    val events = _events.asSharedFlow()

    // Потоки данных
    val userData: StateFlow<UserData> = userPreferencesRepository.getUserData()
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserData())

    val userPreferences: StateFlow<UserPreferences> = userPreferencesRepository.getUserPreferences()
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserPreferences())

    val pomodoroPreferences: StateFlow<PomodoroPreferences> = pomodoroRepository.getPomodoroPreferences()
        .stateIn(viewModelScope, SharingStarted.Eagerly, PomodoroPreferences())

    val categories: StateFlow<List<Category>> = categoryRepository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())


    // Состояния диалогов
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

    // Состояние успешных операций
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage = _successMessage.asStateFlow()

    // Состояние ошибок
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    // Конфигурация для диалогов
    private val _editCategory = MutableStateFlow<Category?>(null)
    val editCategory = _editCategory.asStateFlow()

    // Доступные языки
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

    private fun loadSettings() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Ошибка загрузки настроек: ${e.message}"
                )
                Log.e("SettingsVM", "Ошибка загрузки настроек", e)
            }
        }
    }

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
    private fun updateTheme(themeMode: Int) {
        viewModelScope.launch {
            try {
                userPreferencesRepository.updateTheme(themeMode)
                _successMessage.value = "Тема оформления обновлена"
                // Theme changes are observed and applied immediately via flow in MainViewModel
            } catch (e: Exception) {
                _errorMessage.value = "Не удалось обновить тему: ${e.message}"
                Log.e("SettingsVM", "Ошибка обновления темы", e)
            }
        }
    }

    private fun updateLanguage(language: String) {
        viewModelScope.launch {
            try {
                // First update the repository
                userPreferencesRepository.updateLanguage(language)

                // Then emit event (with a slight delay to prevent race conditions)
                delay(50)
                _events.emit(SettingsEvent.LanguageChanged(language))

                // Only show success message after language actually changes
                delay(200)
                _successMessage.value = "Язык обновлен и применен"
            } catch (e: Exception) {
                _errorMessage.value = "Не удалось обновить язык: ${e.message}"
                Log.e("SettingsVM", "Ошибка обновления языка", e)
            }
        }
    }

    private fun updateStartScreen(screenType: Int) {
        viewModelScope.launch {
            try {
                userPreferencesRepository.updateStartScreen(screenType)
                _successMessage.value = "Стартовый экран обновлен"

                // Send event to notify that start screen was updated
                _events.emit(SettingsEvent.StartScreenChanged(screenType))
            } catch (e: Exception) {
                _errorMessage.value = "Не удалось обновить стартовый экран: ${e.message}"
            }
        }
    }

    private fun updateDefaultTaskView(viewType: Int) {
        viewModelScope.launch {
            try {
                val currentPrefs = userPreferencesRepository.getUserPreferences().collect { prefs ->
                    userPreferencesRepository.updateUserPreferences(
                        prefs.copy(defaultTaskView = com.example.dhbt.domain.model.TaskView.fromInt(viewType))
                    )
                }
                _successMessage.value = "Тип отображения задач обновлен"
            } catch (e: Exception) {
                _errorMessage.value = "Не удалось обновить тип отображения задач: ${e.message}"
            }
        }
    }

    private fun updateDefaultTaskSort(sortType: Int) {
        viewModelScope.launch {
            try {
                userPreferencesRepository.getUserPreferences().collect { prefs ->
                    userPreferencesRepository.updateUserPreferences(
                        prefs.copy(defaultTaskSort = com.example.dhbt.domain.model.TaskSort.fromInt(sortType))
                    )
                }
                _successMessage.value = "Сортировка задач обновлена"
            } catch (e: Exception) {
                _errorMessage.value = "Не удалось обновить сортировку задач: ${e.message}"
            }
        }
    }

    private fun updateDefaultHabitSort(sortType: Int) {
        viewModelScope.launch {
            try {
                userPreferencesRepository.getUserPreferences().collect { prefs ->
                    userPreferencesRepository.updateUserPreferences(
                        prefs.copy(defaultHabitSort = com.example.dhbt.domain.model.HabitSort.fromInt(sortType))
                    )
                }
                _successMessage.value = "Сортировка привычек обновлена"
            } catch (e: Exception) {
                _errorMessage.value = "Не удалось обновить сортировку привычек: ${e.message}"
            }
        }
    }

    private fun updateReminderTime(minutes: Int) {
        viewModelScope.launch {
            try {
                userPreferencesRepository.getUserPreferences().collect { prefs ->
                    userPreferencesRepository.updateUserPreferences(
                        prefs.copy(reminderTimeBeforeTask = minutes)
                    )
                }
                _successMessage.value = "Время напоминания обновлено"
            } catch (e: Exception) {
                _errorMessage.value = "Не удалось обновить время напоминания: ${e.message}"
            }
        }
    }

    private fun updateSoundEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                userPreferencesRepository.getUserPreferences().collect { prefs ->
                    userPreferencesRepository.updateUserPreferences(
                        prefs.copy(defaultSoundEnabled = enabled)
                    )
                }
                _successMessage.value = if (enabled) "Звук включен" else "Звук отключен"
            } catch (e: Exception) {
                _errorMessage.value = "Не удалось обновить настройки звука: ${e.message}"
            }
        }
    }

    private fun updateVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                userPreferencesRepository.getUserPreferences().collect { prefs ->
                    userPreferencesRepository.updateUserPreferences(
                        prefs.copy(defaultVibrationEnabled = enabled)
                    )
                }
                _successMessage.value = if (enabled) "Вибрация включена" else "Вибрация отключена"
            } catch (e: Exception) {
                _errorMessage.value = "Не удалось обновить настройки вибрации: ${e.message}"
            }
        }
    }

    private fun updateQuietHours(enabled: Boolean, start: String?, end: String?) {
        viewModelScope.launch {
            try {
                userPreferencesRepository.updateQuietHours(enabled, start, end)
                _successMessage.value = if (enabled) "Тихие часы настроены" else "Тихие часы отключены"
            } catch (e: Exception) {
                _errorMessage.value = "Не удалось обновить настройки тихих часов: ${e.message}"
            }
        }
    }

    private fun updateCloudSync(enabled: Boolean) {
        viewModelScope.launch {
            try {
                userPreferencesRepository.getUserPreferences().collect { prefs ->
                    userPreferencesRepository.updateUserPreferences(
                        prefs.copy(cloudSyncEnabled = enabled)
                    )
                }
                _successMessage.value = if (enabled) "Облачная синхронизация включена" else "Облачная синхронизация отключена"
            } catch (e: Exception) {
                _errorMessage.value = "Не удалось обновить настройки синхронизации: ${e.message}"
            }
        }
    }

    private fun updateUserName(name: String) {
        viewModelScope.launch {
            try {
                userPreferencesRepository.updateUserName(name)
                _successMessage.value = "Имя пользователя обновлено"
            } catch (e: Exception) {
                _errorMessage.value = "Не удалось обновить имя пользователя: ${e.message}"
            }
        }
    }

    private fun updateUserEmail(email: String) {
        viewModelScope.launch {
            try {
                userPreferencesRepository.updateUserEmail(email)
                _successMessage.value = "Email пользователя обновлен"
            } catch (e: Exception) {
                _errorMessage.value = "Не удалось обновить email пользователя: ${e.message}"
            }
        }
    }

    private fun updateUserAvatar(avatarUrl: String) {
        viewModelScope.launch {
            try {
                userPreferencesRepository.updateUserAvatar(avatarUrl)
                _successMessage.value = "Аватар пользователя обновлен"
            } catch (e: Exception) {
                _errorMessage.value = "Не удалось обновить аватар пользователя: ${e.message}"
            }
        }
    }

    private fun updateWakeupSleepTime(wakeupTime: String?, sleepTime: String?) {
        viewModelScope.launch {
            try {
                userPreferencesRepository.updateWakeupAndSleepTime(wakeupTime, sleepTime)
                _successMessage.value = "Время пробуждения и сна обновлены"
            } catch (e: Exception) {
                _errorMessage.value = "Не удалось обновить время пробуждения и сна: ${e.message}"
            }
        }
    }

    private fun addCategory(category: Category) {
        viewModelScope.launch {
            try {
                categoryRepository.addCategory(category)
                _successMessage.value = "Категория добавлена"
                _showCategoryDialog.value = false
            } catch (e: Exception) {
                _errorMessage.value = "Не удалось добавить категорию: ${e.message}"
            }
        }
    }

    private fun updateCategory(category: Category) {
        viewModelScope.launch {
            try {
                categoryRepository.updateCategory(category)
                _successMessage.value = "Категория обновлена"
                _showCategoryDialog.value = false
                _editCategory.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Не удалось обновить категорию: ${e.message}"
            }
        }
    }

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

                categoryRepository.deleteCategory(categoryId)
                _successMessage.value = "Категория удалена"
            } catch (e: Exception) {
                _errorMessage.value = "Не удалось удалить категорию: ${e.message}"
            }
        }
    }

    private fun startEditCategory(category: Category) {
        _editCategory.value = category
        _showCategoryDialog.value = true
    }

    private fun updatePomodoroSettings(settings: PomodoroPreferences) {
        viewModelScope.launch {
            try {
                pomodoroRepository.updatePomodoroPreferences(settings)
                _successMessage.value = "Настройки Pomodoro обновлены"
            } catch (e: Exception) {
                _errorMessage.value = "Не удалось обновить настройки Pomodoro: ${e.message}"
            }
        }
    }

    private fun resetAllData() {
        viewModelScope.launch {
            try {
                userPreferencesRepository.clearUserData()
                _successMessage.value = "Все данные сброшены"
                _showDeleteDialog.value = false
            } catch (e: Exception) {
                _errorMessage.value = "Не удалось сбросить данные: ${e.message}"
            }
        }
    }
}

data class SettingsUiState(
    val isLoading: Boolean = true,
    val error: String? = null
)

data class LanguageOption(
    val code: String,
    val displayName: String
)

sealed class SettingsEvent {
    data class LanguageChanged(val language: String) : SettingsEvent()
    data class StartScreenChanged(val screenType: Int) : SettingsEvent()
    data class ThemeChanged(val theme: AppTheme) : SettingsEvent()
    object SettingsResetComplete : SettingsEvent()
}

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