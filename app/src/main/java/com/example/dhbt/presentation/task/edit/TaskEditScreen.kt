package com.example.dhbt.presentation.task.edit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.dhbt.R
import com.example.dhbt.domain.model.Category
import com.example.dhbt.domain.model.RecurrenceType
import com.example.dhbt.domain.model.Subtask
import com.example.dhbt.domain.model.Tag
import com.example.dhbt.domain.model.TaskPriority
import com.example.dhbt.domain.model.TaskRecurrence
import com.example.dhbt.presentation.util.toColor
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalTime
import org.threeten.bp.format.DateTimeFormatter
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditScreen(
    taskId: String,
    onNavigateBack: () -> Unit,
    viewModel: TaskEditViewModel = hiltViewModel()
) {
    val state = viewModel.state.value
    val categories by viewModel.categories
    val allTags by viewModel.allTags
    val isLoading by viewModel.isLoading
    val validationErrors by viewModel.validationErrors

    val showDatePicker by viewModel.showDatePicker
    val showTimePicker by viewModel.showTimePicker
    val showCategoryDialog by viewModel.showCategoryDialog
    val showTagDialog by viewModel.showTagDialog
    val showRecurrenceDialog by viewModel.showRecurrenceDialog

    // Создаем локальные состояния для диалогов
    val isNewSubtaskDialogVisible = remember { mutableStateOf(false) }
    val newSubtaskText = remember { mutableStateOf("") }
    val isEisenhowerDialogVisible = remember { mutableStateOf(false) }
    val isPomodoroDialogVisible = remember { mutableStateOf(false) }
    val pomodoroSessions = remember { mutableStateOf(state.estimatedPomodoroSessions?.toString() ?: "") }
    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (state.id.isEmpty())
                            stringResource(R.string.create_task)
                        else
                            stringResource(R.string.edit_task)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.saveTask(onSuccess = onNavigateBack)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = stringResource(R.string.save)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Top
            ) {
                // Основные поля задачи
                TaskBasicFields(
                    title = state.title,
                    description = state.description,
                    onTitleChanged = { viewModel.onTitleChanged(it) },
                    onDescriptionChanged = { viewModel.onDescriptionChanged(it) },
                    titleError = validationErrors[TaskEditField.TITLE]
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Секция даты и времени
                DateAndTimeSection(
                    dueDate = state.dueDate,
                    dueTime = state.dueTime,
                    onDateClick = { viewModel.onToggleDatePicker() },
                    onTimeClick = { viewModel.onToggleTimePicker() },
                    onClearDate = { viewModel.onDueDateSelected(null) },
                    onClearTime = { viewModel.onDueTimeSelected(null) }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Секция категории
                CategorySection(
                    selectedCategoryId = state.categoryId,
                    categories = categories,
                    onCategorySelected = { viewModel.onCategorySelected(it) },
                    onAddCategoryClick = { viewModel.onToggleCategoryDialog() }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Секция приоритета
                PrioritySection(
                    selectedPriority = state.priority,
                    onPrioritySelected = { viewModel.onPriorityChanged(it) }
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Секция тегов
                TagsSection(
                    selectedTagIds = state.tags,
                    allTags = allTags,
                    onTagToggle = { viewModel.onToggleTag(it) },
                    onAddTagClick = { viewModel.onToggleTagDialog() }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Секция цвета
                ColorSection(
                    selectedColor = state.color,
                    onColorSelected = { viewModel.onColorSelected(it) }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Секция подзадач
                SubtasksSection(
                    subtasks = state.subtasks,
                    onAddSubtask = {
                        isNewSubtaskDialogVisible.value = true
                    },
                    onUpdateSubtask = { id, title -> viewModel.onUpdateSubtask(id, title) },
                    onDeleteSubtask = { viewModel.onDeleteSubtask(it) },
                    onToggleSubtaskCompletion = { id, completed ->
                        viewModel.onToggleSubtaskCompletion(id, completed)
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Дополнительные настройки
                AdditionalOptions(
                    eisenhowerQuadrant = state.eisenhowerQuadrant,
                    pomodoroSessions = state.estimatedPomodoroSessions,
                    hasRecurrence = state.recurrence != null,
                    onEisenhowerClick = { isEisenhowerDialogVisible.value = true },
                    onPomodoroClick = { isPomodoroDialogVisible.value = true },
                    onRecurrenceClick = { viewModel.onToggleRecurrenceDialog() }
                )

                // Кнопка сохранения (дублирующая для удобства)
                Button(
                    onClick = { viewModel.saveTask(onSuccess = onNavigateBack) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(stringResource(R.string.save_task))
                }
            }
        }
    }

    // Диалоги
    if (showDatePicker) {
        DatePickerDialog(
            initialDate = state.dueDate ?: LocalDate.now(),
            onDateSelected = { viewModel.onDueDateSelected(it) },
            onDismiss = { viewModel.onToggleDatePicker() }
        )
    }

    if (showTimePicker) {
        TimePickerDialog(
            initialTime = state.dueTime ?: LocalTime.now(),
            onTimeSelected = { viewModel.onDueTimeSelected(it) },
            onDismiss = { viewModel.onToggleTimePicker() }
        )
    }

    if (showCategoryDialog) {
        CategoryDialog(
            onCreateCategory = { name, color, emoji ->
                viewModel.onCreateCategory(name, color, emoji)
                viewModel.onToggleCategoryDialog()
            },
            onDismiss = { viewModel.onToggleCategoryDialog() }
        )
    }

    if (showTagDialog) {
        TagDialog(
            onCreateTag = { name, color ->
                viewModel.onCreateTag(name, color)
                viewModel.onToggleTagDialog()
            },
            onDismiss = { viewModel.onToggleTagDialog() }
        )
    }

    if (showRecurrenceDialog) {
        RecurrenceDialog(
            currentRecurrence = state.recurrence,
            onRecurrenceSet = {
                viewModel.onRecurrenceChanged(it)
                viewModel.onToggleRecurrenceDialog()
            },
            onDismiss = { viewModel.onToggleRecurrenceDialog() }
        )
    }

    if (isNewSubtaskDialogVisible.value) {
        AddSubtaskDialog(
            text = newSubtaskText.value,
            onTextChange = { newSubtaskText.value = it },
            onAddClick = {
                if (newSubtaskText.value.isNotBlank()) {
                    viewModel.onAddSubtask(newSubtaskText.value)
                    newSubtaskText.value = ""
                }
                isNewSubtaskDialogVisible.value = false
            },
            onDismiss = {
                isNewSubtaskDialogVisible.value = false
                newSubtaskText.value = ""
            }
        )
    }

    if (isEisenhowerDialogVisible.value) {
        EisenhowerQuadrantDialog(
            selectedQuadrant = state.eisenhowerQuadrant,
            onQuadrantSelected = {
                viewModel.onEisenhowerQuadrantChanged(it)
                isEisenhowerDialogVisible.value = false
            },
            onDismiss = { isEisenhowerDialogVisible.value = false }
        )
    }

    if (isPomodoroDialogVisible.value) {
        PomodoroSessionsDialog(
            sessions = pomodoroSessions.value,
            onSessionsChange = { pomodoroSessions.value = it },
            onConfirm = {
                viewModel.onEstimatedPomodoroSessionsChanged(
                    pomodoroSessions.value.toIntOrNull()
                )
                isPomodoroDialogVisible.value = false
            },
            onClear = {
                viewModel.onEstimatedPomodoroSessionsChanged(null)
                isPomodoroDialogVisible.value = false
            },
            onDismiss = { isPomodoroDialogVisible.value = false }
        )
    }
}

@Composable
fun TaskBasicFields(
    title: String,
    description: String,
    onTitleChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    titleError: String?
) {
    val focusRequester = remember { FocusRequester() }

    Column(modifier = Modifier.padding(16.dp)) {
        // Поле заголовка
        OutlinedTextField(
            value = title,
            onValueChange = onTitleChanged,
            label = { Text(stringResource(R.string.title)) },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            isError = titleError != null,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            maxLines = 1,
            singleLine = true
        )

        // Отображение ошибки, если есть
        titleError?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Поле описания
        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChanged,
            label = { Text(stringResource(R.string.description)) },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            maxLines = 5
        )
    }

    LaunchedEffect(Unit) {
        // Автоматически ставим фокус на поле заголовка при открытии экрана
        if (title.isEmpty()) {
            focusRequester.requestFocus()
        }
    }
}

@Composable
fun DateAndTimeSection(
    dueDate: LocalDate?,
    dueTime: LocalTime?,
    onDateClick: () -> Unit,
    onTimeClick: () -> Unit,
    onClearDate: () -> Unit,
    onClearTime: () -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = stringResource(R.string.date_and_time),
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Выбор даты
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onDateClick)
                .padding(vertical = 16.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = if (dueDate != null)
                    dueDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy"))
                else
                    stringResource(R.string.select_date),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )

            if (dueDate != null) {
                IconButton(onClick = onClearDate) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = stringResource(R.string.clear)
                    )
                }
            }
        }

        // Выбор времени (только если выбрана дата)
        AnimatedVisibility(visible = dueDate != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onTimeClick)
                    .padding(vertical = 16.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = if (dueTime != null)
                        dueTime.format(DateTimeFormatter.ofPattern("HH:mm"))
                    else
                        stringResource(R.string.select_time),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )

                if (dueTime != null) {
                    IconButton(onClick = onClearTime) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = stringResource(R.string.clear)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategorySection(
    selectedCategoryId: String?,
    categories: List<Category>,
    onCategorySelected: (String?) -> Unit,
    onAddCategoryClick: () -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.category),
                style = MaterialTheme.typography.titleMedium
            )

            TextButton(onClick = onAddCategoryClick) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.new_category))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Горизонтальный список категорий
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(4.dp)
        ) {
            // Опция "Без категории"
            item {
                CategoryChip(
                    selected = selectedCategoryId == null,
                    onClick = { onCategorySelected(null) },
                    label = stringResource(R.string.no_category),
                    icon = null,
                    color = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            // Список всех категорий
            items(categories) { category ->
                CategoryChip(
                    selected = selectedCategoryId == category.id,
                    onClick = { onCategorySelected(category.id) },
                    label = category.name,
                    icon = category.iconEmoji,
                    color = category.color?.toColor() ?: MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun CategoryChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    icon: String?,
    color: Color
) {
    Surface(
        modifier = Modifier
            .height(36.dp),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) color.copy(alpha = 0.2f) else Color.Transparent,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) color else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Text(
                    text = icon,
                    modifier = Modifier.padding(end = 4.dp)
                )
            }

            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (selected) color else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun PrioritySection(
    selectedPriority: TaskPriority,
    onPrioritySelected: (TaskPriority) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = stringResource(R.string.priority),
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Чипы приоритета
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PriorityChip(
                priority = TaskPriority.LOW,
                selected = selectedPriority == TaskPriority.LOW,
                onClick = { onPrioritySelected(TaskPriority.LOW) }
            )

            PriorityChip(
                priority = TaskPriority.MEDIUM,
                selected = selectedPriority == TaskPriority.MEDIUM,
                onClick = { onPrioritySelected(TaskPriority.MEDIUM) }
            )

            PriorityChip(
                priority = TaskPriority.HIGH,
                selected = selectedPriority == TaskPriority.HIGH,
                onClick = { onPrioritySelected(TaskPriority.HIGH) }
            )
        }
    }
}

@Composable
fun PriorityChip(
    priority: TaskPriority,
    selected: Boolean,
    onClick: () -> Unit
) {
    val color = when (priority) {
        TaskPriority.LOW -> Color(0xFF4CAF50)    // Зеленый
        TaskPriority.MEDIUM -> Color(0xFFFF9800) // Оранжевый
        TaskPriority.HIGH -> Color(0xFFF44336)   // Красный
    }

    val label = when (priority) {
        TaskPriority.LOW -> stringResource(R.string.low)
        TaskPriority.MEDIUM -> stringResource(R.string.medium)
        TaskPriority.HIGH -> stringResource(R.string.high)
    }

    Surface(
        modifier = Modifier
            .height(36.dp),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) color.copy(alpha = 0.2f) else Color.Transparent,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) color else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )

                Spacer(modifier = Modifier.width(4.dp))
            }

            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (selected) color else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun TagsSection(
    selectedTagIds: List<String>,
    allTags: List<Tag>,
    onTagToggle: (String) -> Unit,
    onAddTagClick: () -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.tags),
                style = MaterialTheme.typography.titleMedium
            )

            TextButton(onClick = onAddTagClick) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.new_tag))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Выбранные теги
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            mainAxisSpacing = 8.dp,
            crossAxisSpacing = 8.dp
        ) {
            allTags.forEach { tag ->
                val selected = selectedTagIds.contains(tag.id)

                TagChip(
                    tag = tag,
                    selected = selected,
                    onClick = { onTagToggle(tag.id) }
                )
            }

            // Если нет тегов, показываем сообщение
            if (allTags.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_tags),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun TagChip(
    tag: Tag,
    selected: Boolean,
    onClick: () -> Unit
) {
    val tagColor = tag.color?.toColor() ?: MaterialTheme.colorScheme.primary

    Surface(
        modifier = Modifier.height(32.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) tagColor.copy(alpha = 0.2f) else Color.Transparent,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) tagColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(tagColor)
            )

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = tag.name,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun ColorSection(
    selectedColor: String?,
    onColorSelected: (String?) -> Unit
) {
    val colors = listOf(
        null,  // Без цвета
        "#F44336", // Красный
        "#E91E63", // Розовый
        "#9C27B0", // Фиолетовый
        "#673AB7", // Темно-фиолетовый
        "#3F51B5", // Индиго
        "#2196F3", // Синий
        "#03A9F4", // Светло-синий
        "#00BCD4", // Голубой
        "#009688", // Бирюзовый
        "#4CAF50", // Зеленый
        "#8BC34A", // Светло-зеленый
        "#CDDC39", // Лайм
        "#FFEB3B", // Желтый
        "#FFC107", // Янтарный
        "#FF9800", // Оранжевый
        "#FF5722", // Глубокий оранжевый
        "#795548", // Коричневый
        "#9E9E9E", // Серый
        "#607D8B"  // Сине-серый
    )

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = stringResource(R.string.color),
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Сетка цветов
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(4.dp)
        ) {
            items(colors) { colorHex ->
                ColorItem(
                    color = colorHex?.toColor(),
                    selected = colorHex == selectedColor,
                    onClick = { onColorSelected(colorHex) }
                )
            }
        }
    }
}

@Composable
fun ColorItem(
    color: Color?,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(color ?: Color.Transparent)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline,
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (color == null) {
            Icon(
                imageVector = Icons.Default.FormatColorReset,
                contentDescription = stringResource(R.string.no_color),
                tint = MaterialTheme.colorScheme.onSurface
            )
        } else if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = if (isColorDark(color)) Color.White else Color.Black
            )
        }
    }
}

@Composable
fun SubtasksSection(
    subtasks: List<Subtask>,
    onAddSubtask: () -> Unit,
    onUpdateSubtask: (String, String) -> Unit,
    onDeleteSubtask: (String) -> Unit,
    onToggleSubtaskCompletion: (String, Boolean) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.subtasks),
                style = MaterialTheme.typography.titleMedium
            )

            TextButton(onClick = onAddSubtask) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.add_subtask))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Список подзадач
        subtasks.forEachIndexed { index, subtask ->
            SubtaskItem(
                subtask = subtask,
                onUpdateSubtask = onUpdateSubtask,
                onDeleteSubtask = onDeleteSubtask,
                onToggleCompletion = onToggleSubtaskCompletion
            )

            // Разделитель между подзадачами, кроме последней
            if (index < subtasks.size - 1) {
                HorizontalDivider(
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .padding(start = 40.dp)
                )
            }
        }

        // Если нет подзадач, показываем подсказку
        if (subtasks.isEmpty()) {
            Text(
                text = stringResource(R.string.no_subtasks),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SubtaskItem(
    subtask: Subtask,
    onUpdateSubtask: (String, String) -> Unit,
    onDeleteSubtask: (String) -> Unit,
    onToggleCompletion: (String, Boolean) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var editText by remember(subtask.title) { mutableStateOf(subtask.title) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = subtask.isCompleted,
            onCheckedChange = { checked ->
                onToggleCompletion(subtask.id, checked)
            }
        )

        if (isEditing) {
            OutlinedTextField(
                value = editText,
                onValueChange = { editText = it },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (editText.isNotBlank()) {
                            onUpdateSubtask(subtask.id, editText)
                        }
                        isEditing = false
                    }
                )
            )

            IconButton(onClick = {
                if (editText.isNotBlank()) {
                    onUpdateSubtask(subtask.id, editText)
                }
                isEditing = false
            }) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = stringResource(R.string.save)
                )
            }
        } else {
            Text(
                text = subtask.title,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
                    .clickable { isEditing = true },
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (subtask.isCompleted)
                    TextDecoration.LineThrough
                else null,
                color = if (subtask.isCompleted)
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                else
                    MaterialTheme.colorScheme.onSurface
            )

            IconButton(onClick = { isEditing = true }) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(R.string.edit)
                )
            }

            IconButton(onClick = { onDeleteSubtask(subtask.id) }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete)
                )
            }
        }
    }
}

@Composable
fun AdditionalOptions(
    eisenhowerQuadrant: Int?,
    pomodoroSessions: Int?,
    hasRecurrence: Boolean,
    onEisenhowerClick: () -> Unit,
    onPomodoroClick: () -> Unit,
    onRecurrenceClick: () -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = stringResource(R.string.additional_settings),
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Опция матрицы Эйзенхауэра
        OptionalSettingItem(
            icon = Icons.Default.GridView,
            title = stringResource(R.string.eisenhower_matrix),
            subtitle = eisenhowerQuadrant?.let {
                getEisenhowerQuadrantName(it)
            },
            onClick = onEisenhowerClick
        )

        // Опция сеансов Pomodoro
        OptionalSettingItem(
            icon = Icons.Default.Timer,
            title = stringResource(R.string.pomodoro_sessions),
            subtitle = pomodoroSessions?.let {
                stringResource(R.string.pomodoro_sessions_count, it)
            },
            onClick = onPomodoroClick
        )

        // Опция повторения
        OptionalSettingItem(
            icon = Icons.Default.Repeat,
            title = stringResource(R.string.recurrence),
            subtitle = if (hasRecurrence)
                stringResource(R.string.recurring_task)
            else null,
            onClick = onRecurrenceClick
        )
    }
}

@Composable
fun OptionalSettingItem(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )

            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Вспомогательные диалоги

@Composable
fun DatePickerDialog(
    initialDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.select_date)) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.done))
            }
        },
        text = {
            // Здесь должен быть компонент датапикера
            // Для упрощения используем заглушку
            Text("Заглушка датапикера - в реальном приложении здесь будет календарь")
            // В полной реализации следует использовать DatePicker из библиотеки Material3
        }
    )
}

@Composable
fun TimePickerDialog(
    initialTime: LocalTime,
    onTimeSelected: (LocalTime) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.select_time)) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.done))
            }
        },
        text = {
            // Здесь должен быть компонент таймпикера
            // Для упрощения используем заглушку
            Text("Заглушка таймпикера - в реальном приложении здесь будет выбор времени")
            // В полной реализации следует использовать TimePicker из библиотеки Material3
        }
    )
}

@Composable
fun CategoryDialog(
    onCreateCategory: (name: String, color: String?, emoji: String?) -> Unit,
    onDismiss: () -> Unit
) {
    var categoryName by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf<String?>(null) }
    var selectedEmoji by remember { mutableStateOf<String?>("📝") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.new_category)) },
        confirmButton = {
            Button(
                onClick = {
                    if (categoryName.isNotBlank()) {
                        onCreateCategory(categoryName, selectedColor, selectedEmoji)
                    } else {
                        onDismiss()
                    }
                },
                enabled = categoryName.isNotBlank()
            ) {
                Text(stringResource(R.string.create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        text = {
            Column {
                // Название категории
                OutlinedTextField(
                    value = categoryName,
                    onValueChange = { categoryName = it },
                    label = { Text(stringResource(R.string.category_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Выбор цвета
                Text(
                    text = stringResource(R.string.select_color),
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Палитра цветов
                // Здесь должен быть компонент для выбора цвета категории
                Text(
                    text = "Здесь будет компонент для выбора цвета",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Выбор эмодзи
                Text(
                    text = stringResource(R.string.select_emoji),
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Список эмодзи
                // Здесь должен быть компонент для выбора эмодзи
                Text(
                    text = "Здесь будет компонент для выбора эмодзи",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

@Composable
fun TagDialog(
    onCreateTag: (name: String, color: String?) -> Unit,
    onDismiss: () -> Unit
) {
    var tagName by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.new_tag)) },
        confirmButton = {
            Button(
                onClick = {
                    if (tagName.isNotBlank()) {
                        onCreateTag(tagName, selectedColor)
                    } else {
                        onDismiss()
                    }
                },
                enabled = tagName.isNotBlank()
            ) {
                Text(stringResource(R.string.create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        text = {
            Column {
                // Название тега
                OutlinedTextField(
                    value = tagName,
                    onValueChange = { tagName = it },
                    label = { Text(stringResource(R.string.tag_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Выбор цвета
                Text(
                    text = stringResource(R.string.select_color),
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Палитра цветов
                // Здесь должен быть компонент для выбора цвета тега
                Text(
                    text = "Здесь будет компонент для выбора цвета",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

@Composable
fun RecurrenceDialog(
    currentRecurrence: TaskRecurrence?,
    onRecurrenceSet: (TaskRecurrence?) -> Unit,
    onDismiss: () -> Unit
) {
    // Используем type.value вместо type.ordinal
    var recurrenceType by remember { mutableStateOf(currentRecurrence?.type?.value ?: 0) }

    // Используем List<Int>? напрямую
    var daysOfWeek by remember {
        mutableStateOf(currentRecurrence?.daysOfWeek ?: emptyList<Int>())
    }

    var monthDay by remember { mutableStateOf(currentRecurrence?.monthDay ?: 1) }
    var interval by remember { mutableStateOf(currentRecurrence?.customInterval?.toString() ?: "1") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.recurrence)) },
        confirmButton = {
            Button(
                onClick = {
                    val newRecurrence = when (recurrenceType) {
                        0 -> { // Ежедневно
                            TaskRecurrence(
                                id = currentRecurrence?.id ?: UUID.randomUUID().toString(),
                                taskId = currentRecurrence?.taskId ?: "",
                                type = RecurrenceType.DAILY,
                                startDate = System.currentTimeMillis()
                            )
                        }
                        1 -> { // По дням недели
                            if (daysOfWeek.isNotEmpty()) {
                                TaskRecurrence(
                                    id = currentRecurrence?.id ?: UUID.randomUUID().toString(),
                                    taskId = currentRecurrence?.taskId ?: "",
                                    type = RecurrenceType.WEEKLY,
                                    daysOfWeek = daysOfWeek,
                                    startDate = System.currentTimeMillis()
                                )
                            } else null
                        }
                        2 -> { // Ежемесячно
                            TaskRecurrence(
                                id = currentRecurrence?.id ?: UUID.randomUUID().toString(),
                                taskId = currentRecurrence?.taskId ?: "",
                                type = RecurrenceType.MONTHLY,
                                monthDay = monthDay,
                                startDate = System.currentTimeMillis()
                            )
                        }
                        3 -> { // Пользовательский интервал
                            val customInterval = interval.toIntOrNull() ?: 1
                            TaskRecurrence(
                                id = currentRecurrence?.id ?: UUID.randomUUID().toString(),
                                taskId = currentRecurrence?.taskId ?: "",
                                type = RecurrenceType.CUSTOM,
                                customInterval = customInterval,
                                startDate = System.currentTimeMillis()
                            )
                        }
                        else -> null
                    }
                    onRecurrenceSet(newRecurrence)
                }
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        text = {
            Column {
                // Тип повторения
                Text(
                    text = stringResource(R.string.recurrence_type),
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Радио-кнопки для типа повторения
                RecurrenceTypeRadioGroup(
                    selectedType = recurrenceType,
                    onTypeSelected = { recurrenceType = it }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Дополнительные настройки в зависимости от типа повторения
                when (recurrenceType) {
                    1 -> { // По дням недели
                        DaysOfWeekSelector(
                            selectedDays = daysOfWeek,
                            onDayToggle = { day ->
                                daysOfWeek = if (daysOfWeek.contains(day)) {
                                    daysOfWeek - day
                                } else {
                                    daysOfWeek + day
                                }
                            }
                        )
                    }
                    2 -> { // Ежемесячно
                        MonthDaySelector(
                            selectedDay = monthDay,
                            onDaySelected = { monthDay = it }
                        )
                    }
                    3 -> { // Пользовательский интервал
                        CustomIntervalSelector(
                            interval = interval,
                            onIntervalChanged = { interval = it }
                        )
                    }
                }

                // Опция удаления повторения, если оно существует
                if (currentRecurrence != null) {
                    Spacer(modifier = Modifier.height(16.dp))

                    TextButton(
                        onClick = { onRecurrenceSet(null) },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.remove_recurrence))
                    }
                }
            }
        }
    )
}

@Composable
fun RecurrenceTypeRadioGroup(
    selectedType: Int,
    onTypeSelected: (Int) -> Unit
) {
    val types = listOf(
        0 to stringResource(R.string.daily),
        1 to stringResource(R.string.weekly),
        2 to stringResource(R.string.monthly),
        3 to stringResource(R.string.custom)
    )

    Column {
        types.forEach { (type, label) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onTypeSelected(type) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedType == type,
                    onClick = { onTypeSelected(type) }
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
fun DaysOfWeekSelector(
    selectedDays: List<Int>,
    onDayToggle: (Int) -> Unit
) {
    val days = listOf(
        1 to stringResource(R.string.monday),
        2 to stringResource(R.string.tuesday),
        3 to stringResource(R.string.wednesday),
        4 to stringResource(R.string.thursday),
        5 to stringResource(R.string.friday),
        6 to stringResource(R.string.saturday),
        7 to stringResource(R.string.sunday)
    )

    Text(
        text = stringResource(R.string.select_days),
        style = MaterialTheme.typography.bodyMedium
    )

    Spacer(modifier = Modifier.height(8.dp))

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        mainAxisSpacing = 8.dp,
        crossAxisSpacing = 8.dp
    ) {
        days.forEach { (dayNum, dayName) ->
            val isSelected = selectedDays.contains(dayNum)

            Surface(
                modifier = Modifier
                    .height(36.dp)
                    .width(52.dp),
                shape = RoundedCornerShape(18.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                ),
                onClick = { onDayToggle(dayNum) }
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = dayName.take(1),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun MonthDaySelector(
    selectedDay: Int,
    onDaySelected: (Int) -> Unit
) {
    var dayText by remember(selectedDay) { mutableStateOf(selectedDay.toString()) }

    Column {
        Text(
            text = stringResource(R.string.day_of_month),
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = dayText,
            onValueChange = { text ->
                val newValue = text.filter { it.isDigit() }
                dayText = newValue
                newValue.toIntOrNull()?.let { day ->
                    if (day in 1..31) {
                        onDaySelected(day)
                    }
                }
            },
            label = { Text(stringResource(R.string.day)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = stringResource(R.string.day_of_month_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun CustomIntervalSelector(
    interval: String,
    onIntervalChanged: (String) -> Unit
) {
    Column {
        Text(
            text = stringResource(R.string.repeat_every),
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = interval,
                onValueChange = { text ->
                    val newValue = text.filter { it.isDigit() }
                    if (newValue.isNotEmpty()) {
                        onIntervalChanged(newValue)
                    } else {
                        onIntervalChanged("1")
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.width(80.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = stringResource(R.string.days),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
fun AddSubtaskDialog(
    text: String,
    onTextChange: (String) -> Unit,
    onAddClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_subtask)) },
        confirmButton = {
            Button(
                onClick = onAddClick,
                enabled = text.isNotBlank()
            ) {
                Text(stringResource(R.string.add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                label = { Text(stringResource(R.string.subtask_title)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (text.isNotBlank()) {
                        onAddClick()
                    }
                })
            )
        }
    )

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Composable
fun EisenhowerQuadrantDialog(
    selectedQuadrant: Int?,
    onQuadrantSelected: (Int?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.eisenhower_matrix)) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.done))
            }
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.select_quadrant),
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Сетка квадрантов Эйзенхауэра 2x2
                Row(modifier = Modifier.fillMaxWidth()) {
                    // Первый ряд: квадранты 1 и 2
                    EisenhowerQuadrantItem(
                        quadrant = 1,
                        title = stringResource(R.string.important_urgent),
                        color = Color(0xFFFFCDD2), // Light Red
                        isSelected = selectedQuadrant == 1,
                        onClick = { onQuadrantSelected(1) },
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    EisenhowerQuadrantItem(
                        quadrant = 2,
                        title = stringResource(R.string.important_not_urgent),
                        color = Color(0xFFE8F5E9), // Light Green
                        isSelected = selectedQuadrant == 2,
                        onClick = { onQuadrantSelected(2) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    // Второй ряд: квадранты 3 и 4
                    EisenhowerQuadrantItem(
                        quadrant = 3,
                        title = stringResource(R.string.not_important_urgent),
                        color = Color(0xFFFFE0B2), // Light Orange
                        isSelected = selectedQuadrant == 3,
                        onClick = { onQuadrantSelected(3) },
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    EisenhowerQuadrantItem(
                        quadrant = 4,
                        title = stringResource(R.string.not_important_not_urgent),
                        color = Color(0xFFE1F5FE), // Light Blue
                        isSelected = selectedQuadrant == 4,
                        onClick = { onQuadrantSelected(4) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Опция "Не классифицировать"
                TextButton(
                    onClick = { onQuadrantSelected(null) },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(stringResource(R.string.no_quadrant))
                }
            }
        }
    )
}

@Composable
fun EisenhowerQuadrantItem(
    quadrant: Int,
    title: String,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .padding(4.dp)
            .then(
                if (isSelected) {
                    Modifier.shadow(8.dp, RoundedCornerShape(8.dp))
                } else {
                    Modifier
                }
            ),
        colors = CardDefaults.cardColors(containerColor = color),
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        },
        onClick = onClick
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun PomodoroSessionsDialog(
    sessions: String,
    onSessionsChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pomodoro_sessions)) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = sessions.toIntOrNull() != null
            ) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.pomodoro_sessions_desc),
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = sessions,
                    onValueChange = { text ->
                        val newValue = text.filter { it.isDigit() }
                        onSessionsChange(newValue)
                    },
                    label = { Text(stringResource(R.string.number_of_sessions)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = onClear,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.clear))
                }
            }
        }
    )
}

// Вспомогательные функции
fun getEisenhowerQuadrantName(quadrant: Int): String {
    return when (quadrant) {
        1 -> "Важно и срочно"
        2 -> "Важно, но не срочно"
        3 -> "Не важно, но срочно"
        4 -> "Не важно и не срочно"
        else -> "Не указано"
    }
}

fun isColorDark(color: Color): Boolean {
    // Простая формула для определения, является ли цвет тёмным
    val darkness = 1 - (0.299 * color.red + 0.587 * color.green + 0.114 * color.blue)
    return darkness >= 0.5
}

@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    mainAxisSpacing: Dp = 0.dp,
    crossAxisSpacing: Dp = 0.dp,
    content: @Composable () -> Unit
) {
    Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val rows = mutableListOf<MeasuredRow>()
        val rowConstraints = constraints.copy(minWidth = 0)

        // Измеряем каждый элемент
        var rowItems = mutableListOf<Pair<Measurable, androidx.compose.ui.layout.Placeable>>()
        var rowWidth = 0
        var rowHeight = 0

        // Размещаем элементы по рядам
        measurables.forEach { measurable ->
            val placeable = measurable.measure(rowConstraints)

            // Если элемент не помещается в текущий ряд, создаем новый
            if (rowItems.isNotEmpty() && rowWidth + placeable.width + mainAxisSpacing.roundToPx() > constraints.maxWidth) {
                // Сохраняем текущий ряд
                rows.add(MeasuredRow(rowItems, rowWidth, rowHeight))

                // Создаем новый ряд
                rowItems = mutableListOf()
                rowWidth = 0
                rowHeight = 0
            }

            // Добавляем элемент в текущий ряд
            rowItems.add(measurable to placeable)
            rowWidth += placeable.width + if (rowItems.size > 1) mainAxisSpacing.roundToPx() else 0
            rowHeight = rowHeight.coerceAtLeast(placeable.height)
        }

        // Добавляем последний ряд, если он не пустой
        if (rowItems.isNotEmpty()) {
            rows.add(MeasuredRow(rowItems, rowWidth, rowHeight))
        }

        // Определяем общую высоту
        val height = rows.sumOf { row ->
            row.height
        } + (rows.size - 1).coerceAtLeast(0) * crossAxisSpacing.roundToPx()

        layout(
            width = constraints.maxWidth,
            height = height.coerceIn(constraints.minHeight, constraints.maxHeight)
        ) {
            var yPosition = 0

            // Размещаем каждый ряд
            rows.forEach { row ->
                var xPosition = 0

                row.items.forEach { (_, placeable) ->
                    placeable.placeRelative(x = xPosition, y = yPosition)
                    xPosition += placeable.width + mainAxisSpacing.roundToPx()
                }

                yPosition += row.height + crossAxisSpacing.roundToPx()
            }
        }
    }
}

private class MeasuredRow(
    val items: List<Pair<Measurable, androidx.compose.ui.layout.Placeable>>,
    val width: Int,
    val height: Int
)