package com.example.dhbt.presentation.eisenhower

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dhbt.domain.model.Category
import com.example.dhbt.domain.model.Task
import com.example.dhbt.domain.model.TaskPriority
import com.example.dhbt.domain.model.TaskStatus
import com.example.dhbt.domain.repository.CategoryRepository
import com.example.dhbt.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class EisenhowerViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    // Текущий выбранный фильтр категории
    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    val selectedCategoryId = _selectedCategoryId.asStateFlow()

    // Статус загрузки
    private val _isLoading = MutableStateFlow(false) // Изменено на false по умолчанию
    val isLoading = _isLoading.asStateFlow()

    // Ошибка, если есть
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    init {
        // Инициализируем загрузку данных при создании ViewModel
        loadData()
    }

    private fun loadData() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                // Просто запускаем поток для первичной загрузки данных
                quadrantTasks.collect { }
                // Загрузка завершена после первой эмиссии данных
                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = "Ошибка при загрузке данных: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    // Все категории для выбора в фильтре
    val categories: StateFlow<List<Category>> = categoryRepository.getAllCategories()
        .catch { e ->
            Log.e("EisenhowerViewModel", "Ошибка при загрузке категорий", e)
            _error.value = "Не удалось загрузить категории: ${e.message}"
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Все задачи, разбитые по квадрантам
    @OptIn(ExperimentalCoroutinesApi::class)
    val quadrantTasks: StateFlow<Map<Int, List<Task>>> = _selectedCategoryId
        .flatMapLatest { selectedCategory ->
            taskRepository.getAllTasks()
                .map { tasks ->
                    val filteredTasks = if (selectedCategory != null) {
                        tasks.filter { it.categoryId == selectedCategory && it.status != TaskStatus.COMPLETED }
                    } else {
                        tasks.filter { it.status != TaskStatus.COMPLETED }
                    }

                    // Распределяем задачи по квадрантам
                    val result = mutableMapOf<Int, List<Task>>()
                    for (quadrant in 1..4) {
                        val tasksInQuadrant = filteredTasks.filter { it.eisenhowerQuadrant == quadrant }
                        result[quadrant] = tasksInQuadrant
                    }

                    // Если у задачи нет явного определения квадранта, определяем его по приоритету и дате
                    val unassignedTasks = filteredTasks.filter { it.eisenhowerQuadrant == null }
                    val urgentImportantTasks = mutableListOf<Task>()
                    val importantNotUrgentTasks = mutableListOf<Task>()
                    val urgentNotImportantTasks = mutableListOf<Task>()
                    val notUrgentNotImportantTasks = mutableListOf<Task>()

                    for (task in unassignedTasks) {
                        when {
                            task.priority == TaskPriority.HIGH && isTaskUrgent(task) ->
                                urgentImportantTasks.add(task)
                            task.priority == TaskPriority.HIGH ->
                                importantNotUrgentTasks.add(task)
                            isTaskUrgent(task) ->
                                urgentNotImportantTasks.add(task)
                            else ->
                                notUrgentNotImportantTasks.add(task)
                        }
                    }

                    result[1] = (result[1] ?: emptyList()) + urgentImportantTasks
                    result[2] = (result[2] ?: emptyList()) + importantNotUrgentTasks
                    result[3] = (result[3] ?: emptyList()) + urgentNotImportantTasks
                    result[4] = (result[4] ?: emptyList()) + notUrgentNotImportantTasks

                    result
                }
                .catch { e ->
                    Log.e("EisenhowerViewModel", "Ошибка при загрузке задач", e)
                    _error.value = "Не удалось загрузить задачи: ${e.message}"
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    // Определение того, является ли задача срочной
    private fun isTaskUrgent(task: Task): Boolean {
        val currentTime = System.currentTimeMillis()
        val dueDate = task.dueDate ?: return false

        // Задача считается срочной, если до дедлайна осталось меньше 24 часов
        return (dueDate - currentTime) < 24 * 60 * 60 * 1000
    }

    // Выбрать категорию для фильтрации
    fun selectCategory(categoryId: String?) {
        _selectedCategoryId.value = categoryId
    }

    // Переместить задачу в другой квадрант
    fun moveTaskToQuadrant(taskId: String, quadrant: Int) {
        viewModelScope.launch {
            try {
                val task = taskRepository.getTaskById(taskId)
                task?.let {
                    val updatedTask = it.copy(eisenhowerQuadrant = quadrant)
                    taskRepository.updateTask(updatedTask)
                }
            } catch (e: Exception) {
                Log.e("EisenhowerViewModel", "Ошибка при перемещении задачи", e)
                _error.value = "Ошибка при перемещении задачи: ${e.message}"
            }
        }
    }

    // Создать новую задачу в конкретном квадранте
    fun createTaskInQuadrant(
        title: String,
        quadrant: Int,
        categoryId: String?,
        dueDate: Long? = null
    ) {
        viewModelScope.launch {
            try {
                // Определяем приоритет для задачи в зависимости от квадранта
                val priority = when (quadrant) {
                    1, 2 -> TaskPriority.HIGH
                    else -> TaskPriority.MEDIUM
                }

                val task = Task(
                    id = UUID.randomUUID().toString(),
                    title = title,
                    categoryId = categoryId,
                    priority = priority,
                    creationDate = System.currentTimeMillis(),
                    dueDate = dueDate,
                    eisenhowerQuadrant = quadrant,
                    status = TaskStatus.ACTIVE
                )

                taskRepository.addTask(task)
            } catch (e: Exception) {
                Log.e("EisenhowerViewModel", "Ошибка при создании задачи", e)
                _error.value = "Ошибка при создании задачи: ${e.message}"
            }
        }
    }

    fun dismissError() {
        _error.value = null
    }

    // Принудительная перезагрузка данных
    fun refreshData() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                // Этот код заставит заново загрузить данные
                categories.collect { } // Просто запускаем сбор потока для обновления данных
                quadrantTasks.collect { }
                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = "Ошибка при обновлении данных: ${e.message}"
                _isLoading.value = false
            }
        }
    }
}