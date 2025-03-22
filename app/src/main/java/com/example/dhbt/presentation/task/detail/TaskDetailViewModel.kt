package com.example.dhbt.presentation.task.detail

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dhbt.domain.model.Subtask
import com.example.dhbt.domain.model.TaskStatus
import com.example.dhbt.domain.repository.CategoryRepository
import com.example.dhbt.domain.repository.PomodoroRepository
import com.example.dhbt.domain.repository.TagRepository
import com.example.dhbt.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val categoryRepository: CategoryRepository,
    private val tagRepository: TagRepository,
    private val pomodoroRepository: PomodoroRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = mutableStateOf(TaskDetailState())
    val state: State<TaskDetailState> = _state

    private val taskId: String = savedStateHandle.get<String>("taskId") ?: ""

    init {
        if (taskId.isNotEmpty()) {
            loadTaskDetails()
        } else {
            _state.value = _state.value.copy(
                isLoading = false,
                error = "Не удалось загрузить задачу: отсутствует ID"
            )
        }
    }

    private fun loadTaskDetails() {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(isLoading = true)

                // Загружаем основные данные задачи
                val task = taskRepository.getTaskById(taskId)
                if (task == null) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Задача не найдена"
                    )
                    return@launch
                }

                // Загружаем категорию
                val category = if (task.categoryId != null) {
                    categoryRepository.getCategoryById(task.categoryId)
                } else null

                // Загружаем повторение задачи
                val recurrence = taskRepository.getTaskRecurrence(taskId)

                // Загружаем время в Pomodoro
                val totalFocusTime = pomodoroRepository.getTotalFocusTimeForTask(taskId)

                // Загружаем подзадачи
                taskRepository.getSubtasksForTask(taskId).collectLatest { subtasks ->
                    // Загружаем теги
                    tagRepository.getTagsForTask(taskId).collectLatest { tags ->
                        _state.value = _state.value.copy(
                            isLoading = false,
                            task = task,
                            subtasks = subtasks,
                            tags = tags,
                            category = category,
                            recurrence = recurrence,
                            totalFocusTime = totalFocusTime
                        )
                    }
                }
            } catch (e: IOException) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Ошибка загрузки задачи: ${e.message}"
                )
            }
        }
    }

    fun toggleSubtaskCompletion(subtask: Subtask) {
        viewModelScope.launch {
            try {
                taskRepository.completeSubtask(subtask.id, !subtask.isCompleted)
                // Обновленные данные подзадач придут через Flow в loadTaskDetails
            } catch (e: IOException) {
                // Обработка ошибки
            }
        }
    }

    fun updateTaskStatus(status: TaskStatus) {
        viewModelScope.launch {
            try {
                taskRepository.updateTaskStatus(taskId, status)
                // Повторно загружаем задачу для обновления данных
                loadTaskDetails()
            } catch (e: IOException) {
                _state.value = _state.value.copy(
                    error = "Не удалось обновить статус задачи: ${e.message}"
                )
            }
        }
    }

    fun deleteTask() {
        viewModelScope.launch {
            try {
                taskRepository.deleteTask(taskId)
                _state.value = _state.value.copy(
                    task = null, // Указываем, что задача удалена
                    isLoading = false
                )
            } catch (e: IOException) {
                _state.value = _state.value.copy(
                    error = "Не удалось удалить задачу: ${e.message}"
                )
            }
        }
    }

    fun toggleDeleteDialog() {
        _state.value = _state.value.copy(
            showDeleteDialog = !state.value.showDeleteDialog
        )
    }

    fun toggleEditTask() {
        _state.value = _state.value.copy(
            showEditTask = !state.value.showEditTask
        )
    }

    fun togglePomodoroDialog() {
        _state.value = _state.value.copy(
            showPomodoroDialog = !state.value.showPomodoroDialog
        )
    }

    fun startPomodoroSession() {
        viewModelScope.launch {
            try {
                pomodoroRepository.startSession(taskId, com.example.dhbt.domain.model.PomodoroSessionType.WORK)
                // Сессия началась, можно обновить UI или перейти на экран таймера
            } catch (e: IOException) {
                _state.value = _state.value.copy(
                    error = "Не удалось начать Pomodoro-сессию: ${e.message}"
                )
            }
        }
    }
}