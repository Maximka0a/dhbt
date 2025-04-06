package com.example.dhbt.presentation.habit.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dhbt.domain.model.Category
import com.example.dhbt.domain.model.CategoryType
import com.example.dhbt.domain.model.FrequencyType
import com.example.dhbt.domain.model.Habit
import com.example.dhbt.domain.model.HabitFrequency
import com.example.dhbt.domain.model.HabitStatus
import com.example.dhbt.domain.model.HabitType
import com.example.dhbt.domain.model.Notification
import com.example.dhbt.domain.model.NotificationTarget
import com.example.dhbt.domain.model.PeriodType
import com.example.dhbt.domain.model.Tag
import com.example.dhbt.domain.repository.CategoryRepository
import com.example.dhbt.domain.repository.HabitRepository
import com.example.dhbt.domain.repository.NotificationRepository
import com.example.dhbt.domain.repository.TagRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class EditHabitViewModel @Inject constructor(
    private val habitRepository: HabitRepository,
    private val categoryRepository: CategoryRepository,
    private val tagRepository: TagRepository,
    private val notificationRepository: NotificationRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val habitId: String? = savedStateHandle["habitId"]
    val isCreationMode: Boolean = habitId == null

    private val _uiState = MutableStateFlow(EditHabitUiState())
    val uiState: StateFlow<EditHabitUiState> = _uiState.asStateFlow()

    private val _validationState = MutableStateFlow(EditHabitValidationState())
    val validationState: StateFlow<EditHabitValidationState> = _validationState.asStateFlow()

    private val _saveResult = MutableStateFlow<SaveResult?>(null)
    val saveResult: StateFlow<SaveResult?> = _saveResult.asStateFlow()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _tags = MutableStateFlow<List<Tag>>(emptyList())
    val tags: StateFlow<List<Tag>> = _tags.asStateFlow()

    private val _events = MutableSharedFlow<EditHabitOneTimeEvent>()
    val events: SharedFlow<EditHabitOneTimeEvent> = _events

    init {
        Timber.d("ViewModel инициализирован. isCreationMode=$isCreationMode, habitId=$habitId")
        loadInitialData()
    }

    fun onEvent(event: EditHabitEvent) {
        when (event) {
            is EditHabitEvent.TitleChanged -> updateTitle(event.title)
            is EditHabitEvent.DescriptionChanged -> updateDescription(event.description)
            is EditHabitEvent.IconEmojiChanged -> updateIconEmoji(event.emoji)
            is EditHabitEvent.ColorChanged -> updateColor(event.color)
            is EditHabitEvent.HabitTypeChanged -> updateHabitType(event.type)
            is EditHabitEvent.CategoryChanged -> updateCategory(event.categoryId)
            is EditHabitEvent.CreateNewCategory -> createNewCategory(event.name, event.color)
            is EditHabitEvent.FrequencyTypeChanged -> updateFrequencyType(event.type)
            is EditHabitEvent.DaysOfWeekChanged -> updateDaysOfWeek(event.days)
            is EditHabitEvent.TimesPerPeriodChanged -> updateTimesPerPeriod(event.times)
            is EditHabitEvent.PeriodTypeChanged -> updatePeriodType(event.type)
            is EditHabitEvent.TargetValueChanged -> updateTargetValue(event.value)
            is EditHabitEvent.UnitOfMeasurementChanged -> updateUnitOfMeasurement(event.unit)
            is EditHabitEvent.TargetStreakChanged -> updateTargetStreak(event.streak)
            is EditHabitEvent.ReminderEnabledChanged -> {
                Timber.d("Изменение статуса напоминания на ${event.enabled}")
                updateReminderEnabled(event.enabled)
            }
            is EditHabitEvent.ReminderTimeChanged -> {
                Timber.d("Изменение времени напоминания на ${event.time}")
                updateReminderTime(event.time)
            }
            is EditHabitEvent.ReminderDaysChanged -> {
                Timber.d("Изменение дней напоминания на ${event.days}")
                updateReminderDays(event.days)
            }
            is EditHabitEvent.TagsChanged -> updateTags(event.tagIds)
            is EditHabitEvent.CreateNewTag -> createNewTag(event.name, event.color)
            is EditHabitEvent.SaveHabit -> saveHabit()
            is EditHabitEvent.DeleteHabit -> deleteHabit()
        }
    }

    fun resetSaveResult() {
        _saveResult.value = null
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            try {
                launch { loadCategories() }
                launch { loadTags() }

                if (!isCreationMode) {
                    habitId?.let { loadHabit(it) }
                } else {
                    setDefaultValues()
                }
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при загрузке начальных данных")
                emitEvent(EditHabitOneTimeEvent.Error("Failed to load data: ${e.localizedMessage}"))
            }
        }
    }

    private fun setDefaultValues() {
        _uiState.update { state ->
            state.copy(
                habitType = HabitType.BINARY,
                frequencyType = FrequencyType.DAILY,
                status = HabitStatus.ACTIVE,
                selectedColor = "#FF6200EE",
                selectedDaysOfWeek = setOf(
                    DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                    DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
                ),
                reminderTime = LocalTime.of(9, 0),
                targetValue = 1f,
                timesPerPeriod = 1,
                periodType = PeriodType.WEEK
            )
        }
        Timber.d("Установлены значения по умолчанию")
    }

    private fun loadHabit(id: String) {
        viewModelScope.launch {
            try {
                Timber.d("Загрузка привычки с ID: $id")
                val (habit, frequency) = habitRepository.getHabitWithFrequency(id)
                Timber.d("Загружена привычка: ${habit.title}")

                val daysOfWeek = frequency?.daysOfWeek?.mapNotNull { dayValue ->
                    try {
                        DayOfWeek.of(dayValue)
                    } catch (e: Exception) {
                        Timber.e(e, "Ошибка при преобразовании дня недели: $dayValue")
                        null
                    }
                }?.toSet() ?: emptySet()

                val habitTags = tagRepository.getTagsForTask(id).first()
                Timber.d("Загружено ${habitTags.size} тегов для привычки")

                // Загрузка существующих уведомлений для привычки
                try {
                    Timber.d("Запрос уведомлений для привычки с ID: $id")
                    val notifications = notificationRepository.getNotificationsForTarget(id, NotificationTarget.HABIT).first()
                    Timber.d("Получено ${notifications.size} уведомлений для привычки")

                    val notification = notifications.firstOrNull()
                    if (notification != null) {
                        Timber.d("Найдено уведомление: id=${notification.id}, time=${notification.time}, enabled=${notification.isEnabled}")
                    } else {
                        Timber.d("Уведомления для привычки не найдены")
                    }

                    _uiState.update { state ->
                        state.copy(
                            title = habit.title,
                            description = habit.description ?: "",
                            iconEmoji = habit.iconEmoji ?: "📋",
                            selectedColor = habit.color ?: "#FF6200EE",
                            habitType = habit.type,
                            categoryId = habit.categoryId,
                            frequencyType = frequency?.type ?: FrequencyType.DAILY,
                            selectedDaysOfWeek = daysOfWeek,
                            timesPerPeriod = frequency?.timesPerPeriod ?: 1,
                            periodType = frequency?.periodType ?: PeriodType.WEEK,
                            targetValue = habit.targetValue ?: 1f,
                            unitOfMeasurement = habit.unitOfMeasurement ?: "",
                            targetStreak = habit.targetStreak ?: 0,
                            status = habit.status,
                            selectedTagIds = habitTags.map { it.id }.toSet(),
                            currentStreak = habit.currentStreak,
                            bestStreak = habit.bestStreak,
                            creationDate = habit.creationDate,
                            // Обновление полей уведомлений из базы данных
                            reminderEnabled = notification != null && notification.isEnabled,
                            reminderTime = notification?.time?.let {
                                try {
                                    LocalTime.parse(it, DateTimeFormatter.ofPattern("HH:mm"))
                                } catch (e: Exception) {
                                    Timber.e(e, "Ошибка при парсинге времени уведомления: $it")
                                    LocalTime.of(9, 0)
                                }
                            } ?: LocalTime.of(9, 0),
                            reminderDays = notification?.daysOfWeek?.mapNotNull {
                                try {
                                    DayOfWeek.of(it)
                                } catch (e: Exception) {
                                    Timber.e(e, "Ошибка при парсинге дня недели: $it")
                                    null
                                }
                            }?.toSet() ?: emptySet()
                        )
                    }
                    Timber.d("Состояние UI обновлено с данными уведомлений: enabled=${notification != null && notification.isEnabled}")
                } catch (e: Exception) {
                    Timber.e(e, "Ошибка при загрузке уведомлений")
                    _uiState.update { state ->
                        state.copy(
                            title = habit.title,
                            description = habit.description ?: "",
                            iconEmoji = habit.iconEmoji ?: "📋",
                            selectedColor = habit.color ?: "#FF6200EE",
                            habitType = habit.type,
                            categoryId = habit.categoryId,
                            frequencyType = frequency?.type ?: FrequencyType.DAILY,
                            selectedDaysOfWeek = daysOfWeek,
                            timesPerPeriod = frequency?.timesPerPeriod ?: 1,
                            periodType = frequency?.periodType ?: PeriodType.WEEK,
                            targetValue = habit.targetValue ?: 1f,
                            unitOfMeasurement = habit.unitOfMeasurement ?: "",
                            targetStreak = habit.targetStreak ?: 0,
                            status = habit.status,
                            selectedTagIds = habitTags.map { it.id }.toSet(),
                            currentStreak = habit.currentStreak,
                            bestStreak = habit.bestStreak,
                            creationDate = habit.creationDate
                        )
                    }
                }

                validateForm()
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при загрузке привычки с ID: $id")
                _saveResult.value = SaveResult.Error("Error loading habit: ${e.localizedMessage}")
                emitEvent(EditHabitOneTimeEvent.Error("Failed to load habit: ${e.localizedMessage}"))
            }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            try {
                categoryRepository.getCategoriesByType(CategoryType.HABIT)
                    .collect { habitCategories ->
                        _categories.value = habitCategories
                        Timber.d("Загружено ${habitCategories.size} категорий")
                    }
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при загрузке категорий")
                emitEvent(EditHabitOneTimeEvent.Error("Failed to load categories"))
            }
        }
    }

    private fun loadTags() {
        viewModelScope.launch {
            try {
                tagRepository.getAllTags()
                    .collect { allTags ->
                        _tags.value = allTags
                        Timber.d("Загружено ${allTags.size} тегов")
                    }
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при загрузке тегов")
                emitEvent(EditHabitOneTimeEvent.Error("Failed to load tags"))
            }
        }
    }

    private fun updateTitle(title: String) {
        _uiState.update { it.copy(title = title) }
        validateForm()
    }

    private fun updateDescription(description: String) {
        _uiState.update { it.copy(description = description) }
    }

    private fun updateIconEmoji(emoji: String) {
        _uiState.update { it.copy(iconEmoji = emoji) }
    }

    private fun updateColor(color: String) {
        _uiState.update { it.copy(selectedColor = color) }
    }

    private fun updateHabitType(type: HabitType) {
        _uiState.update { it.copy(habitType = type) }
    }

    private fun updateCategory(categoryId: String?) {
        _uiState.update { it.copy(categoryId = categoryId) }
    }

    private fun updateFrequencyType(type: FrequencyType) {
        _uiState.update { it.copy(frequencyType = type) }
    }

    private fun updateDaysOfWeek(days: Set<DayOfWeek>) {
        _uiState.update { it.copy(selectedDaysOfWeek = days) }
    }

    private fun updateTimesPerPeriod(times: Int) {
        _uiState.update { it.copy(timesPerPeriod = times.coerceAtLeast(1)) }
    }

    private fun updatePeriodType(type: PeriodType) {
        _uiState.update { it.copy(periodType = type) }
    }

    private fun updateTargetValue(value: Float) {
        _uiState.update { it.copy(targetValue = value.coerceAtLeast(0.1f)) }
    }

    private fun updateUnitOfMeasurement(unit: String) {
        _uiState.update { it.copy(unitOfMeasurement = unit) }
    }

    private fun updateTargetStreak(streak: Int) {
        _uiState.update { it.copy(targetStreak = streak.coerceAtLeast(0)) }
    }

    private fun updateReminderEnabled(enabled: Boolean) {
        _uiState.update { it.copy(reminderEnabled = enabled) }
        Timber.d("Статус напоминания изменен на: $enabled")
    }

    private fun updateReminderTime(time: LocalTime) {
        _uiState.update { it.copy(reminderTime = time) }
        Timber.d("Время напоминания изменено на: $time")
    }

    private fun updateReminderDays(days: Set<DayOfWeek>) {
        _uiState.update { it.copy(reminderDays = days) }
        Timber.d("Дни недели для напоминания изменены на: $days")
    }

    private fun updateTags(tagIds: Set<String>) {
        _uiState.update { it.copy(selectedTagIds = tagIds) }
    }

    private fun createNewCategory(name: String, color: String) {
        if (name.isBlank()) {
            emitEvent(EditHabitOneTimeEvent.Error("Category name cannot be empty"))
            return
        }

        viewModelScope.launch {
            try {
                val newCategory = Category(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    color = color,
                    type = CategoryType.HABIT
                )
                val categoryId = categoryRepository.addCategory(newCategory)
                updateCategory(categoryId)
                emitEvent(EditHabitOneTimeEvent.CategoryCreated("Category created: $name"))
                Timber.d("Создана новая категория: $name с ID: $categoryId")
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при создании категории")
                emitEvent(EditHabitOneTimeEvent.Error("Failed to create category: ${e.localizedMessage}"))
            }
        }
    }

    private fun createNewTag(name: String, color: String) {
        if (name.isBlank()) {
            emitEvent(EditHabitOneTimeEvent.Error("Tag name cannot be empty"))
            return
        }

        viewModelScope.launch {
            try {
                val newTag = Tag(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    color = color
                )
                val tagId = tagRepository.addTag(newTag)
                val updatedTags = _uiState.value.selectedTagIds + tagId
                updateTags(updatedTags)
                emitEvent(EditHabitOneTimeEvent.TagCreated("Tag created: $name"))
                Timber.d("Создан новый тег: $name с ID: $tagId")
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при создании тега")
                emitEvent(EditHabitOneTimeEvent.Error("Failed to create tag: ${e.localizedMessage}"))
            }
        }
    }

    private fun validateForm(): Boolean {
        val titleError = when {
            _uiState.value.title.isBlank() -> "Название не может быть пустым"
            _uiState.value.title.length > 50 -> "Название слишком длинное (максимум 50 символов)"
            else -> null
        }

        val isValid = titleError == null

        _validationState.update {
            it.copy(
                titleError = titleError,
                isFormValid = isValid
            )
        }

        return isValid
    }

    private fun saveHabit() {
        if (!validateForm()) {
            _saveResult.value = SaveResult.Error("Please fill in all required fields")
            return
        }

        viewModelScope.launch {
            try {
                Timber.d("Начато сохранение привычки")
                val state = _uiState.value
                val newHabitId = habitId ?: UUID.randomUUID().toString()
                Timber.d("ID привычки: $newHabitId (isCreationMode=$isCreationMode)")

                val habit = Habit(
                    id = newHabitId,
                    title = state.title,
                    description = state.description.takeIf { it.isNotEmpty() },
                    iconEmoji = state.iconEmoji.takeIf { it.isNotEmpty() },
                    color = state.selectedColor,
                    creationDate = if (isCreationMode) System.currentTimeMillis()
                    else uiState.value.creationDate ?: System.currentTimeMillis(),
                    type = state.habitType,
                    targetValue = if (state.habitType != HabitType.BINARY) state.targetValue else null,
                    unitOfMeasurement = if (state.habitType == HabitType.QUANTITY) state.unitOfMeasurement.takeIf { it.isNotEmpty() } else null,
                    targetStreak = state.targetStreak.takeIf { it > 0 },
                    currentStreak = if (isCreationMode) 0 else uiState.value.currentStreak,
                    bestStreak = if (isCreationMode) 0 else uiState.value.bestStreak,
                    status = state.status,
                    categoryId = state.categoryId,
                )
                Timber.d("Подготовлен объект привычки: ${habit.title}")

                val daysOfWeek = state.selectedDaysOfWeek.map { it.value }

                val habitFrequency = HabitFrequency(
                    id = UUID.randomUUID().toString(),
                    habitId = newHabitId,
                    type = state.frequencyType,
                    daysOfWeek = if (state.frequencyType == FrequencyType.SPECIFIC_DAYS) daysOfWeek else null,
                    timesPerPeriod = if (state.frequencyType == FrequencyType.TIMES_PER_WEEK ||
                        state.frequencyType == FrequencyType.TIMES_PER_MONTH)
                        state.timesPerPeriod else null,
                    periodType = if (state.frequencyType == FrequencyType.TIMES_PER_WEEK)
                        PeriodType.WEEK
                    else if (state.frequencyType == FrequencyType.TIMES_PER_MONTH)
                        PeriodType.MONTH
                    else null
                )
                Timber.d("Подготовлен объект частоты привычки: тип=${habitFrequency.type}")

                // Сохраняем привычку и частоту
                if (isCreationMode) {
                    habitRepository.addHabit(habit)
                    habitRepository.setHabitFrequency(newHabitId, habitFrequency)
                    Timber.d("Создана новая привычка с ID: $newHabitId")
                } else {
                    habitRepository.updateHabit(habit)
                    habitRepository.setHabitFrequency(newHabitId, habitFrequency)
                    Timber.d("Обновлена привычка с ID: $newHabitId")
                }

                // Сохраняем настройки тегов для привычки (если требуется)

                // Обработка уведомлений
                try {
                    Timber.d("Начато сохранение уведомлений. reminderEnabled=${state.reminderEnabled}")

                    // Проверяем, есть ли существующие уведомления
                    val existingNotifications = notificationRepository.getNotificationsForTarget(newHabitId, NotificationTarget.HABIT).first()
                    Timber.d("Найдено ${existingNotifications.size} существующих уведомлений для привычки")

                    // Удаляем существующие уведомления для привычки
                    Timber.d("Удаление существующих уведомлений для привычки с ID: $newHabitId")
                    notificationRepository.deleteNotificationsForTarget(newHabitId, NotificationTarget.HABIT)
                    Timber.d("Существующие уведомления удалены")

                    // Если включены напоминания, создаем новое уведомление
                    if (state.reminderEnabled) {
                        Timber.d("Включено напоминание, создаем новое уведомление")

                        // Форматируем время в строку формата "HH:mm"
                        val timeStr = state.reminderTime.format(DateTimeFormatter.ofPattern("HH:mm"))
                        Timber.d("Время напоминания: $timeStr")

                        // Получаем выбранные дни недели или все дни, если ничего не выбрано
                        val reminderDaysOfWeek = if (state.reminderDays.isNotEmpty()) {
                            state.reminderDays.map { it.value }
                        } else {
                            // Если дни не выбраны, используем все дни недели
                            (1..7).toList()
                        }
                        Timber.d("Дни недели для напоминания: $reminderDaysOfWeek")

                        // Создаем объект уведомления
                        val notificationId = UUID.randomUUID().toString()
                        val notification = Notification(
                            id = notificationId,
                            targetId = newHabitId,
                            targetType = NotificationTarget.HABIT,
                            time = timeStr,
                            daysOfWeek = reminderDaysOfWeek,
                            isEnabled = true,
                            message = "Пора выполнить привычку: ${state.title}"
                        )
                        Timber.d("Создан объект уведомления с ID: $notificationId")

                        // Сохраняем уведомление в БД
                        val savedNotificationId = notificationRepository.addNotification(notification)
                        Timber.d("Уведомление сохранено в БД с ID: $savedNotificationId")

                        // Проверяем, что уведомление действительно сохранилось
                        val savedNotifications = notificationRepository.getNotificationsForTarget(newHabitId, NotificationTarget.HABIT).first()
                        Timber.d("После сохранения найдено ${savedNotifications.size} уведомлений")
                        for (n in savedNotifications) {
                            Timber.d("Сохраненное уведомление: id=${n.id}, targetId=${n.targetId}, time=${n.time}, enabled=${n.isEnabled}")
                        }
                    } else {
                        Timber.d("Напоминание отключено, уведомления не создаются")
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Ошибка при сохранении уведомления")
                    // Не прерываем основной процесс сохранения из-за ошибки с уведомлениями
                }

                _saveResult.value = SaveResult.Success
                if (isCreationMode) {
                    emitEvent(EditHabitOneTimeEvent.HabitCreated("Habit created successfully"))
                    Timber.d("Привычка успешно создана")
                } else {
                    emitEvent(EditHabitOneTimeEvent.HabitUpdated("Habit updated successfully"))
                    Timber.d("Привычка успешно обновлена")
                }

            } catch (e: Exception) {
                Timber.e(e, "Ошибка при сохранении привычки")
                _saveResult.value = SaveResult.Error("Error saving: ${e.localizedMessage}")
                emitEvent(EditHabitOneTimeEvent.Error("Failed to save habit: ${e.localizedMessage}"))
            }
        }
    }

    private fun deleteHabit() {
        if (isCreationMode || habitId == null) {
            emitEvent(EditHabitOneTimeEvent.Error("Cannot delete a habit that hasn't been saved"))
            return
        }

        viewModelScope.launch {
            try {
                Timber.d("Начато удаление привычки с ID: $habitId")

                // Удаляем все уведомления для этой привычки
                Timber.d("Удаление уведомлений для привычки с ID: $habitId")
                notificationRepository.deleteNotificationsForTarget(habitId, NotificationTarget.HABIT)
                Timber.d("Уведомления удалены")

                // Удаляем саму привычку
                habitRepository.deleteHabit(habitId)
                Timber.d("Привычка удалена")

                _saveResult.value = SaveResult.Deleted
                emitEvent(EditHabitOneTimeEvent.HabitDeleted("Habit deleted successfully"))
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при удалении привычки")
                _saveResult.value = SaveResult.Error("Error deleting: ${e.localizedMessage}")
                emitEvent(EditHabitOneTimeEvent.Error("Failed to delete habit: ${e.localizedMessage}"))
            }
        }
    }

    private fun emitEvent(event: EditHabitOneTimeEvent) {
        viewModelScope.launch {
            _events.emit(event)
        }
    }
}

data class EditHabitUiState(
    val title: String = "",
    val description: String = "",
    val iconEmoji: String = "📋",
    val selectedColor: String = "#FF6200EE",
    val habitType: HabitType = HabitType.BINARY,
    val categoryId: String? = null,
    val frequencyType: FrequencyType = FrequencyType.DAILY,
    val selectedDaysOfWeek: Set<DayOfWeek> = emptySet(),
    val timesPerPeriod: Int = 1,
    val periodType: PeriodType = PeriodType.WEEK,
    val targetValue: Float = 1f,
    val unitOfMeasurement: String = "",
    val targetStreak: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val status: HabitStatus = HabitStatus.ACTIVE,
    val selectedTagIds: Set<String> = emptySet(),
    val reminderEnabled: Boolean = false,
    val reminderTime: LocalTime = LocalTime.of(9, 0),
    val reminderDays: Set<DayOfWeek> = emptySet(),
    val creationDate: Long? = null
)

data class EditHabitValidationState(
    val titleError: String? = null,
    val isFormValid: Boolean = false
)

sealed class SaveResult {
    object Success : SaveResult()
    object Deleted : SaveResult()
    data class Error(val message: String) : SaveResult()
}

sealed class EditHabitOneTimeEvent {
    data class HabitCreated(val message: String) : EditHabitOneTimeEvent()
    data class HabitUpdated(val message: String) : EditHabitOneTimeEvent()
    data class HabitDeleted(val message: String) : EditHabitOneTimeEvent()
    data class CategoryCreated(val message: String) : EditHabitOneTimeEvent()
    data class TagCreated(val message: String) : EditHabitOneTimeEvent()
    data class Error(val message: String) : EditHabitOneTimeEvent()
}

sealed class EditHabitEvent {
    data class TitleChanged(val title: String) : EditHabitEvent()
    data class DescriptionChanged(val description: String) : EditHabitEvent()
    data class IconEmojiChanged(val emoji: String) : EditHabitEvent()
    data class ColorChanged(val color: String) : EditHabitEvent()
    data class HabitTypeChanged(val type: HabitType) : EditHabitEvent()
    data class CategoryChanged(val categoryId: String?) : EditHabitEvent()
    data class CreateNewCategory(val name: String, val color: String) : EditHabitEvent()
    data class FrequencyTypeChanged(val type: FrequencyType) : EditHabitEvent()
    data class DaysOfWeekChanged(val days: Set<DayOfWeek>) : EditHabitEvent()
    data class TimesPerPeriodChanged(val times: Int) : EditHabitEvent()
    data class PeriodTypeChanged(val type: PeriodType) : EditHabitEvent()
    data class TargetValueChanged(val value: Float) : EditHabitEvent()
    data class UnitOfMeasurementChanged(val unit: String) : EditHabitEvent()
    data class TargetStreakChanged(val streak: Int) : EditHabitEvent()
    data class ReminderEnabledChanged(val enabled: Boolean) : EditHabitEvent()
    data class ReminderTimeChanged(val time: LocalTime) : EditHabitEvent()
    data class ReminderDaysChanged(val days: Set<DayOfWeek>) : EditHabitEvent()
    data class TagsChanged(val tagIds: Set<String>) : EditHabitEvent()
    data class CreateNewTag(val name: String, val color: String) : EditHabitEvent()
    object SaveHabit : EditHabitEvent()
    object DeleteHabit : EditHabitEvent()
}