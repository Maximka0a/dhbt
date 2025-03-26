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
import com.example.dhbt.domain.model.PeriodType
import com.example.dhbt.domain.model.Tag
import com.example.dhbt.domain.repository.CategoryRepository
import com.example.dhbt.domain.repository.HabitRepository
import com.example.dhbt.domain.repository.TagRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class EditHabitViewModel @Inject constructor(
    private val habitRepository: HabitRepository,
    private val categoryRepository: CategoryRepository,
    private val tagRepository: TagRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val habitId: String? = savedStateHandle["habitId"]
    private val isEditMode = habitId != null

    // UI состояния и потоки
    private val _uiState = MutableStateFlow(EditHabitUiState())
    val uiState = _uiState.asStateFlow()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories = _categories.asStateFlow()

    private val _tags = MutableStateFlow<List<Tag>>(emptyList())
    val tags = _tags.asStateFlow()

    private val _validationState = MutableStateFlow(EditHabitValidationState())
    val validationState = _validationState.asStateFlow()

    private val _saveResult = MutableStateFlow<SaveResult?>(null)
    val saveResult = _saveResult.asStateFlow()

    init {
        Timber.d("Инициализация EditHabitViewModel с habitId: $habitId")
        loadCategories()
        loadTags()

        if (isEditMode) {
            loadHabit(habitId!!)
        } else {
            // Значения по умолчанию для новой привычки
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
                    reminderTime = LocalTime.of(9, 0)
                )
            }
        }
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
            is EditHabitEvent.ReminderEnabledChanged -> updateReminderEnabled(event.enabled)
            is EditHabitEvent.ReminderTimeChanged -> updateReminderTime(event.time)
            is EditHabitEvent.ReminderDaysChanged -> updateReminderDays(event.days)
            is EditHabitEvent.TagsChanged -> updateTags(event.tagIds)
            is EditHabitEvent.CreateNewTag -> createNewTag(event.name, event.color)
            is EditHabitEvent.SaveHabit -> saveHabit()
            is EditHabitEvent.DeleteHabit -> deleteHabit()
        }
    }

    private fun loadHabit(id: String) {
        viewModelScope.launch {
            try {
                // Получаем привычку вместе с частотой
                val (habit, frequency) = habitRepository.getHabitWithFrequency(id)

                // Преобразование дней недели из Int в DayOfWeek
                val daysOfWeek = frequency?.daysOfWeek?.mapNotNull { dayValue ->
                    try {
                        DayOfWeek.of(dayValue)
                    } catch (e: Exception) {
                        Timber.e(e, "Ошибка при преобразовании дня недели: $dayValue")
                        null
                    }
                }?.toSet() ?: emptySet()

                // Загружаем теги для привычки
                val habitTags = tagRepository.getTagsForTask(id).first()

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
                        bestStreak = habit.bestStreak
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при загрузке привычки")
                _saveResult.value = SaveResult.Error("Ошибка при загрузке привычки: ${e.message}")
            }
        }
    }
    private fun loadCategories() {
        viewModelScope.launch {
            try {
                categoryRepository.getCategoriesByType(CategoryType.HABIT)
                    .collect { habitCategories ->
                        Timber.d("Загружены категории: ${habitCategories.size}")
                        _categories.value = habitCategories
                    }
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при загрузке категорий")
            }
        }
    }

    private fun loadTags() {
        viewModelScope.launch {
            try {
                tagRepository.getAllTags()
                    .collect { allTags ->
                        Timber.d("Загружены теги: ${allTags.size}")
                        _tags.value = allTags
                    }
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при загрузке тегов")
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

    private fun createNewCategory(name: String, color: String) {
        viewModelScope.launch {
            try {
                val newCategory = Category(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    color = color,
                    type = CategoryType.HABIT
                )
                Timber.d("Создание новой категории: $newCategory")
                val categoryId = categoryRepository.addCategory(newCategory)
                updateCategory(categoryId)
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при создании категории")
                _saveResult.value = SaveResult.Error("Ошибка при создании категории: ${e.message}")
            }
        }
    }

    private fun updateFrequencyType(type: FrequencyType) {
        _uiState.update { it.copy(frequencyType = type) }
    }

    private fun updateDaysOfWeek(days: Set<DayOfWeek>) {
        _uiState.update { it.copy(selectedDaysOfWeek = days) }
    }

    private fun updateTimesPerPeriod(times: Int) {
        _uiState.update { it.copy(timesPerPeriod = times) }
    }

    private fun updatePeriodType(type: PeriodType) {
        _uiState.update { it.copy(periodType = type) }
    }

    private fun updateTargetValue(value: Float) {
        _uiState.update { it.copy(targetValue = value) }
    }

    private fun updateUnitOfMeasurement(unit: String) {
        _uiState.update { it.copy(unitOfMeasurement = unit) }
    }

    private fun updateTargetStreak(streak: Int) {
        _uiState.update { it.copy(targetStreak = streak) }
    }

    private fun updateReminderEnabled(enabled: Boolean) {
        _uiState.update { it.copy(reminderEnabled = enabled) }
    }

    private fun updateReminderTime(time: LocalTime) {
        _uiState.update { it.copy(reminderTime = time) }
    }

    private fun updateReminderDays(days: Set<DayOfWeek>) {
        _uiState.update { it.copy(reminderDays = days) }
    }

    private fun updateTags(tagIds: Set<String>) {
        _uiState.update { it.copy(selectedTagIds = tagIds) }
    }

    private fun createNewTag(name: String, color: String) {
        viewModelScope.launch {
            try {
                val newTag = Tag(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    color = color
                )
                Timber.d("Создание нового тега: $newTag")
                val tagId = tagRepository.addTag(newTag)
                // Добавляем новый тег к выбранным
                val updatedTags = _uiState.value.selectedTagIds + tagId
                updateTags(updatedTags)
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при создании тега")
                _saveResult.value = SaveResult.Error("Ошибка при создании тега: ${e.message}")
            }
        }
    }

    private fun validateForm(): Boolean {
        val titleError = if (_uiState.value.title.isBlank()) "Название не может быть пустым" else null

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
            Timber.w("Форма не прошла валидацию. Прерываем сохранение.")
            _saveResult.value = SaveResult.Error("Заполните все обязательные поля")
            return
        }

        viewModelScope.launch {
            try {
                val state = _uiState.value
                Timber.d("Начало сохранения привычки. Данные: title=${state.title}, type=${state.habitType}")

                // Генерируем или используем существующий ID
                val newHabitId = habitId ?: UUID.randomUUID().toString()

                // 1. Создаем привычку
                val habit = Habit(
                    id = newHabitId,
                    title = state.title,
                    description = state.description.takeIf { it.isNotEmpty() },
                    iconEmoji = state.iconEmoji.takeIf { it.isNotEmpty() },
                    color = state.selectedColor,
                    creationDate = System.currentTimeMillis(),
                    type = state.habitType,
                    targetValue = if (state.habitType != HabitType.BINARY) state.targetValue else null,
                    unitOfMeasurement = if (state.habitType == HabitType.QUANTITY) state.unitOfMeasurement else null,
                    targetStreak = state.targetStreak.takeIf { it > 0 },
                    currentStreak = if (isEditMode) uiState.value.currentStreak else 0,
                    bestStreak = if (isEditMode) uiState.value.bestStreak else 0,
                    status = state.status,
                    categoryId = state.categoryId,
                )

                // 2. Создаем частоту
                val daysOfWeek = state.selectedDaysOfWeek.map { it.value }
                Timber.d("Дни недели для сохранения: $daysOfWeek")

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

                // 3. Сохраняем всё в одной транзакции
                if (isEditMode) {
                    Timber.d("Обновление существующей привычки: $newHabitId")
                    habitRepository.updateHabit(habit)
                    habitRepository.setHabitFrequency(newHabitId, habitFrequency)
                } else {
                    Timber.d("Создание новой привычки: $newHabitId")
                    // Используем новый метод для транзакционного сохранения
                    val result = habitRepository.addHabit(habit)
                    habitRepository.setHabitFrequency(newHabitId, habitFrequency)
                    Timber.d("Результат сохранения привычки: $result")
                }

                _saveResult.value = SaveResult.Success
                Timber.d("Привычка и частота успешно сохранены")

            } catch (e: Exception) {
                Timber.e(e, "Ошибка при сохранении привычки")
                _saveResult.value = SaveResult.Error("Ошибка сохранения: ${e.message}")
            }
        }
    }

    private fun deleteHabit() {
        if (!isEditMode || habitId == null) return

        viewModelScope.launch {
            try {
                Timber.d("Удаление привычки: $habitId")
                habitRepository.deleteHabit(habitId)
                _saveResult.value = SaveResult.Deleted
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при удалении привычки")
                _saveResult.value = SaveResult.Error(e.message ?: "Ошибка при удалении")
            }
        }
    }

    // Сбрасываем состояние сохранения при навигации
    fun resetSaveResult() {
        _saveResult.value = null
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
    val endDate: Long? = null
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