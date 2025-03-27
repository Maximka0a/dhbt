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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
            is EditHabitEvent.ReminderEnabledChanged -> updateReminderEnabled(event.enabled)
            is EditHabitEvent.ReminderTimeChanged -> updateReminderTime(event.time)
            is EditHabitEvent.ReminderDaysChanged -> updateReminderDays(event.days)
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
    }

    private fun loadHabit(id: String) {
        viewModelScope.launch {
            try {
                val (habit, frequency) = habitRepository.getHabitWithFrequency(id)

                val daysOfWeek = frequency?.daysOfWeek?.mapNotNull { dayValue ->
                    try {
                        DayOfWeek.of(dayValue)
                    } catch (e: Exception) {
                        null
                    }
                }?.toSet() ?: emptySet()

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

                validateForm()
            } catch (e: Exception) {
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
                    }
            } catch (e: Exception) {
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
                    }
            } catch (e: Exception) {
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
            } catch (e: Exception) {
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
            } catch (e: Exception) {
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
                val state = _uiState.value
                val newHabitId = habitId ?: UUID.randomUUID().toString()

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

                if (isCreationMode) {
                    habitRepository.addHabit(habit)
                    habitRepository.setHabitFrequency(newHabitId, habitFrequency)
                    _saveResult.value = SaveResult.Success
                    emitEvent(EditHabitOneTimeEvent.HabitCreated("Habit created successfully"))
                } else {
                    habitRepository.updateHabit(habit)
                    habitRepository.setHabitFrequency(newHabitId, habitFrequency)
                    _saveResult.value = SaveResult.Success
                    emitEvent(EditHabitOneTimeEvent.HabitUpdated("Habit updated successfully"))
                }

            } catch (e: Exception) {
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
                habitRepository.deleteHabit(habitId)
                _saveResult.value = SaveResult.Deleted
                emitEvent(EditHabitOneTimeEvent.HabitDeleted("Habit deleted successfully"))
            } catch (e: Exception) {
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