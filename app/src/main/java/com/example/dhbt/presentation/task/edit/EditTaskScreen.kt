@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.dhbt.presentation.task.edit

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.dhbt.R
import com.example.dhbt.presentation.task.edit.components.*
import kotlinx.coroutines.flow.collectLatest

@Composable
fun EditTaskScreen(
    taskId: String = "",
    onNavigateBack: () -> Unit,
    viewModel: EditTaskViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val categories by viewModel.categoryState.collectAsState()
    val tags by viewModel.tagsState.collectAsState()
    val subtasks by viewModel.subtasks.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current

    // Скроллы и анимации
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    // Загружаем данные задачи, если есть ID
    LaunchedEffect(taskId) {
        if (taskId.isNotEmpty()) {
            viewModel.setTaskId(taskId)
        }
    }

    // Обработка событий
    LaunchedEffect(key1 = true) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is EditTaskEvent.NavigateBack -> onNavigateBack()
                is EditTaskEvent.ShowError -> snackbarHostState.showSnackbar(event.message)
                is EditTaskEvent.ClearSubtaskInput -> Unit // Обрабатывается в SubtasksSection
            }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(
                            if (uiState.isEditing) R.string.edit_task
                            else R.string.create_task
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.onCancel() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    // Кнопка сохранения
                    IconButton(
                        onClick = { viewModel.saveTask() },
                        enabled = uiState.title.isNotBlank()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = stringResource(R.string.save)
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (uiState.title.isNotBlank()) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.saveTask() },
                    icon = { Icon(Icons.Default.Check, contentDescription = null) },
                    text = { Text(stringResource(R.string.save_task)) },
                    expanded = !scrollBehavior.state.overlappedFraction.equals(0f)
                )
            }
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Раздел базовой информации (название и описание)
                item {
                    TaskBasicInfoSection(
                        title = uiState.title,
                        description = uiState.description,
                        onTitleChanged = viewModel::onTitleChanged,
                        onDescriptionChanged = viewModel::onDescriptionChanged
                    )
                }

                // Раздел выбора цвета (цветная метка)
                item {
                    ColorLabelSection(
                        selectedColor = uiState.color,
                        onColorSelected = viewModel::onColorSelected
                    )
                }

                // Раздел выбора категории
                item {
                    CategorySection(
                        categories = categories,
                        selectedCategoryId = uiState.categoryId,
                        onCategorySelected = viewModel::onCategorySelected,
                        onAddNewCategory = viewModel::onAddNewCategory
                    )
                }

                // Раздел даты и времени
                item {
                    DateTimeSection(
                        dueDate = uiState.dueDate,
                        dueTime = uiState.dueTime,
                        onDueDateChanged = viewModel::onDueDateChanged,
                        onDueTimeChanged = viewModel::onDueTimeChanged
                    )
                }

                // Раздел приоритета и классификации
                item {
                    TaskPrioritySection(
                        priority = uiState.priority,
                        eisenhowerQuadrant = uiState.eisenhowerQuadrant,
                        onPriorityChanged = viewModel::onPriorityChanged,
                        onQuadrantChanged = viewModel::onEisenhowerQuadrantChanged
                    )
                }

                // Раздел тегов
                item {
                    TagsSection(
                        tags = tags,
                        selectedTagIds = uiState.selectedTagIds,
                        onTagToggled = viewModel::onTagToggled,
                        onAddNewTag = viewModel::onAddNewTag
                    )
                }

                // Раздел подзадач
                item {
                    SubtasksSection(
                        subtasks = subtasks,
                        onAddSubtask = viewModel::addSubtask,
                        onDeleteSubtask = viewModel::removeSubtask,
                        onToggleSubtaskCompletion = viewModel::toggleSubtaskCompletion
                    )
                }

                // Раздел повторяемости
                item {
                    RecurrenceSection(
                        selectedType = uiState.recurrenceType,
                        selectedDays = uiState.daysOfWeek,
                        monthDay = uiState.monthDay,
                        customInterval = uiState.customInterval,
                        onTypeSelected = viewModel::onRecurrenceTypeChanged,
                        onDayToggled = viewModel::onDayOfWeekToggled,
                        onMonthDayChanged = viewModel::onMonthDayChanged,
                        onCustomIntervalChanged = viewModel::onCustomIntervalChanged
                    )
                }

                // Дополнительные параметры (длительность, помидоры)
                item {
                    AdditionalParametersSection(
                        duration = uiState.duration,
                        estimatedPomodoros = uiState.estimatedPomodoros,
                        onDurationChanged = viewModel::onDurationChanged,
                        onEstimatedPomodorosChanged = viewModel::onEstimatedPomodorosChanged
                    )
                }

                // Нижний отступ
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}