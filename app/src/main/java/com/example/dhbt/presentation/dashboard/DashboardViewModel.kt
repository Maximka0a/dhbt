package com.example.dhbt.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dhbt.domain.model.Habit
import com.example.dhbt.domain.model.Task
import com.example.dhbt.domain.model.TaskStatus
import com.example.dhbt.domain.model.UserData
import com.example.dhbt.domain.repository.HabitRepository
import com.example.dhbt.domain.repository.TaskRepository
import com.example.dhbt.domain.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val habitRepository: HabitRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    // State flow для состояния экрана
    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    // Временные константы для фильтрации
    private val today: Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private val tomorrow: Long = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            // Комбинируем потоки данных из репозиториев
            combine(
                userPreferencesRepository.getUserData(),
                taskRepository.getAllTasks(),
                habitRepository.getAllHabits()
            ) { userData, tasks, habits ->
                // Фильтруем задачи и привычки на сегодня
                val todayTasks = filterTasksForToday(tasks)
                val todayHabits = filterHabitsForToday(habits)

                // Считаем статистику
                val completedTasks = todayTasks.count { it.status == TaskStatus.COMPLETED }
                val totalTasks = todayTasks.size

                val completedHabits = todayHabits.count { it.status.value == 1 }
                val totalHabits = todayHabits.size

                // Обновляем состояние
                _state.update {
                    it.copy(
                        isLoading = false,
                        userData = userData,
                        todayTasks = todayTasks,
                        todayHabits = todayHabits,
                        completedTasks = completedTasks,
                        totalTasks = totalTasks,
                        completedHabits = completedHabits,
                        totalHabits = totalHabits,
                        error = null
                    )
                }
            }.catch { error ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = error.message ?: "Произошла ошибка при загрузке данных"
                    )
                }
            }.collect()
        }
    }

    // Обработчики событий UI
    fun onTaskCheckedChange(taskId: String, isCompleted: Boolean) {
        viewModelScope.launch {
            val newStatus = if (isCompleted) TaskStatus.COMPLETED else TaskStatus.ACTIVE
            taskRepository.updateTaskStatus(taskId, newStatus)
        }
    }

    fun onDeleteTask(taskId: String) {
        viewModelScope.launch {
            taskRepository.deleteTask(taskId)
        }
    }

    fun onHabitProgressIncrement(habitId: String) {
        viewModelScope.launch {
           habitRepository.incrementHabitProgress(habitId)
        }
    }

    fun dismissError() {
        _state.update { it.copy(error = null) }
    }

    fun refresh() {
        loadData()
    }

    // Фильтрация задач на сегодня
    private fun filterTasksForToday(tasks: List<Task>): List<Task> {
        return tasks.filter { task ->
            // Фильтруем активные задачи с дедлайном на сегодня или просроченные
            val dueDate = task.dueDate ?: return@filter false
            val isActive = task.status == TaskStatus.ACTIVE

            // Задача на сегодня или просрочена
            val isDueToday = dueDate in today until tomorrow
            val isOverdue = dueDate < today

            isActive && (isDueToday || isOverdue)
        }.sortedBy { it.dueDate } // Сортировка по дате выполнения
    }

    // Фильтрация привычек на сегодня
    private fun filterHabitsForToday(habits: List<Habit>): List<Habit> {
        val today = LocalDate.now()
        val dayOfWeek = today.dayOfWeek.value // 1 (понедельник) - 7 (воскресенье)

        return habits.filter { habit ->
            // Только активные привычки
            if (habit.status.value != 0) return@filter false

            // Проверка по расписанию
            val frequency = habit.frequency ?: return@filter true

            when (frequency.type.value) {
                0 -> true // Ежедневная привычка
                1 -> { // Привычка по конкретным дням недели
                    frequency.daysOfWeek?.contains(dayOfWeek) ?: false
                }
                2, 3 -> { // X раз в неделю/месяц - показываем каждый день для простоты
                    true
                }
                else -> false
            }
        }
    }

    // Форматирование текущей даты для отображения
    fun getTodayFormatted(): String {
        val dateFormatter = DateTimeFormatter.ofPattern("d MMMM")
        return LocalDate.now().format(dateFormatter)
    }
}

// Состояние UI для экрана Dashboard
data class DashboardState(
    val isLoading: Boolean = false,
    val userData: UserData? = null,
    val todayTasks: List<Task> = emptyList(),
    val todayHabits: List<Habit> = emptyList(),
    val completedTasks: Int = 0,
    val totalTasks: Int = 0,
    val completedHabits: Int = 0,
    val totalHabits: Int = 0,
    val error: String? = null
)