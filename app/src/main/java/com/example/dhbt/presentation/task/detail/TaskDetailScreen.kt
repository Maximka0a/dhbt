package com.example.dhbt.presentation.task.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Unpublished
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.dhbt.R
import com.example.dhbt.domain.model.RecurrenceType
import com.example.dhbt.domain.model.Subtask
import com.example.dhbt.domain.model.Tag
import com.example.dhbt.domain.model.TaskPriority
import com.example.dhbt.domain.model.TaskRecurrence
import com.example.dhbt.domain.model.TaskStatus
import com.example.dhbt.presentation.util.toColor
import org.threeten.bp.Instant
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskDetailScreen(
    taskId: String,
    onNavigateBack: () -> Unit,
    onEditTask: (String) -> Unit,
    viewModel: TaskDetailViewModel = hiltViewModel()
) {
    val state = viewModel.state.value

    // Обработка удаленной задачи (навигация назад)
    LaunchedEffect(state.task) {
        if (state.task == null && !state.isLoading) {
            onNavigateBack()
        }
    }

    // Обработка перехода к редактированию
    LaunchedEffect(state.showEditTask) {
        if (state.showEditTask && state.task != null) {
            onEditTask(taskId)
            viewModel.toggleEditTask() // Сбрасываем флаг
        }
    }

    Scaffold(
        topBar = {
            TaskDetailTopBar(
                title = state.task?.title ?: "",
                status = state.task?.status ?: TaskStatus.ACTIVE,
                onBackClick = onNavigateBack,
                onEditClick = { viewModel.toggleEditTask() },
                onDeleteClick = { viewModel.toggleDeleteDialog() },
                onArchiveClick = {
                    viewModel.updateTaskStatus(TaskStatus.ARCHIVED)
                }
            )
        },
        floatingActionButton = {
            if (state.task != null && state.task.status == TaskStatus.ACTIVE) {
                FloatingActionButton(
                    onClick = {
                        viewModel.updateTaskStatus(TaskStatus.COMPLETED)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Завершить задачу"
                    )
                }
            } else if (state.task != null && state.task.status == TaskStatus.COMPLETED) {
                FloatingActionButton(
                    onClick = {
                        viewModel.updateTaskStatus(TaskStatus.ACTIVE)
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Default.Unpublished,
                        contentDescription = "Отметить как незавершенную"
                    )
                }
            }
        }
    ) { paddingValues ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            state.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = state.error,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(
                            onClick = onNavigateBack,
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            Text("Вернуться назад")
                        }
                    }
                }
            }
            state.task != null -> {
                val task = state.task
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Цветовая метка и приоритет
                    ColorAndPrioritySection(
                        color = task.color,
                        priority = task.priority,
                        categoryName = state.category?.name
                    )

                    // Основная информация: описание, даты
                    TaskMainInfoSection(
                        description = task.description,
                        dueDate = task.dueDate,
                        dueTime = task.dueTime,
                        creationDate = task.creationDate,
                        status = task.status,
                        completionDate = task.completionDate
                    )

                    Divider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp))

                    // Секция с параметрами задачи (категория, повторение и т.д.)
                    TaskParametersSection(
                        category = state.category,
                        recurrence = state.recurrence,
                        eisenhowerQuadrant = task.eisenhowerQuadrant
                    )

                    Divider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp))

                    // Секция с тегами
                    if (state.tags.isNotEmpty()) {
                        TagsSection(tags = state.tags)
                        Divider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp))
                    }

                    // Секция с подзадачами
                    if (state.subtasks.isNotEmpty()) {
                        SubtasksSection(
                            subtasks = state.subtasks,
                            onToggleSubtask = { viewModel.toggleSubtaskCompletion(it) }
                        )
                        Divider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp))
                    }

                    // Секция Pomodoro
                    if (task.estimatedPomodoroSessions != null || state.totalFocusTime != null) {
                        PomodoroSection(
                            estimatedSessions = task.estimatedPomodoroSessions,
                            totalFocusTime = state.totalFocusTime,
                            onStartSession = { viewModel.startPomodoroSession() }
                        )
                        Divider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp))
                    }

                    // Дополнительное пространство снизу
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }

    // Диалоги
    if (state.showDeleteDialog) {
        DeleteTaskDialog(
            onConfirm = { viewModel.deleteTask() },
            onDismiss = { viewModel.toggleDeleteDialog() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailTopBar(
    title: String,
    status: TaskStatus,
    onBackClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onArchiveClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = stringResource(R.string.navigate_back)
                )
            }
        },
        actions = {
            // Иконка статуса
            Icon(
                imageVector = when (status) {
                    TaskStatus.ACTIVE -> Icons.Outlined.CheckCircle
                    TaskStatus.COMPLETED -> Icons.Filled.CheckCircle
                    TaskStatus.ARCHIVED -> Icons.Default.Archive
                },
                contentDescription = null,
                tint = when (status) {
                    TaskStatus.ACTIVE -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    TaskStatus.COMPLETED -> MaterialTheme.colorScheme.primary
                    TaskStatus.ARCHIVED -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                },
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            // Кнопка редактирования
            IconButton(onClick = onEditClick) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(R.string.edit)
                )
            }

            // Меню
            IconButton(onClick = { showMenu = !showMenu }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = stringResource(R.string.more_options)
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.delete)) },
                    onClick = {
                        showMenu = false
                        onDeleteClick()
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null
                        )
                    }
                )

                if (status != TaskStatus.ARCHIVED) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.archive)) },
                        onClick = {
                            showMenu = false
                            onArchiveClick()
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Archive,
                                contentDescription = null
                            )
                        }
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
fun ColorAndPrioritySection(
    color: String?,
    priority: TaskPriority,
    categoryName: String?
) {
    val backgroundColor = color?.toColor() ?: MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .background(backgroundColor.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Категория
            if (categoryName != null) {
                Text(
                    text = categoryName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Приоритет
            Row(verticalAlignment = Alignment.CenterVertically) {
                val priorityColor = when (priority) {
                    TaskPriority.HIGH -> Color(0xFFE57373) // Красный
                    TaskPriority.MEDIUM -> Color(0xFFFFB74D) // Оранжевый
                    TaskPriority.LOW -> Color(0xFF81C784) // Зеленый
                }

                val priorityText = when (priority) {
                    TaskPriority.HIGH -> stringResource(R.string.high_priority)
                    TaskPriority.MEDIUM -> stringResource(R.string.medium_priority)
                    TaskPriority.LOW -> stringResource(R.string.low_priority)
                }

                Icon(
                    imageVector = Icons.Default.Flag,
                    contentDescription = null,
                    tint = priorityColor,
                    modifier = Modifier.size(16.dp)
                )

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = priorityText,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun TaskMainInfoSection(
    description: String?,
    dueDate: Long?,
    dueTime: String?,
    creationDate: Long,
    status: TaskStatus,
    completionDate: Long?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Описание
        if (!description.isNullOrBlank()) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        // Блок с датами
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Срок выполнения
                if (dueDate != null) {
                    val date = LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(dueDate),
                        ZoneId.systemDefault()
                    ).toLocalDate()

                    val formattedDate = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Column {
                            Text(
                                text = stringResource(R.string.due_date),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Text(
                                text = formattedDate + (dueTime?.let { " в $it" } ?: ""),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // Дата создания
                val creationLocalDate = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(creationDate),
                    ZoneId.systemDefault()
                ).toLocalDate()

                val formattedCreationDate = creationLocalDate.format(
                    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = if (status == TaskStatus.COMPLETED) 8.dp else 0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Text(
                            text = stringResource(R.string.creation_date),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = formattedCreationDate,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // Дата завершения (если завершена)
                if (status == TaskStatus.COMPLETED && completionDate != null) {
                    val completionLocalDate = LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(completionDate),
                        ZoneId.systemDefault()
                    ).toLocalDate()

                    val formattedCompletionDate = completionLocalDate.format(
                        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Column {
                            Text(
                                text = stringResource(R.string.completion_date),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Text(
                                text = formattedCompletionDate,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TaskParametersSection(
    category: com.example.dhbt.domain.model.Category?,
    recurrence: TaskRecurrence?,
    eisenhowerQuadrant: Int?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = stringResource(R.string.parameters),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Категория
        if (category != null) {
            ParameterItem(
                icon = Icons.Default.Category,
                label = stringResource(R.string.category),
                value = category.name,
                color = category.color?.toColor()
            )
        }

// Повторение
        if (recurrence != null) {
            val recurrenceText = when (recurrence.type) {
                RecurrenceType.DAILY -> stringResource(R.string.daily)
                RecurrenceType.WEEKLY -> {
                    val dayNames = recurrence.daysOfWeek?.map {
                        when (it) {
                            1 -> stringResource(R.string.monday_short)
                            2 -> stringResource(R.string.tuesday_short)
                            3 -> stringResource(R.string.wednesday_short)
                            4 -> stringResource(R.string.thursday_short)
                            5 -> stringResource(R.string.friday_short)
                            6 -> stringResource(R.string.saturday_short)
                            7 -> stringResource(R.string.sunday_short)
                            else -> "?"
                        }
                    }?.joinToString(", ") ?: stringResource(R.string.weekly)
                    stringResource(R.string.weekly_on, dayNames)
                }
                RecurrenceType.MONTHLY -> {
                    val dayOfMonth = recurrence.monthDay ?: 1
                    stringResource(R.string.monthly_on_day, dayOfMonth)
                }
                RecurrenceType.CUSTOM -> {
                    val interval = recurrence.customInterval ?: 1
                    stringResource(R.string.every_n_days, interval)
                }
                RecurrenceType.YEARLY -> {
                    // Добавьте обработку для годового повторения
                    stringResource(R.string.yearly)
                }
                // Если в будущем добавятся новые типы, это обеспечит безопасность
                else -> stringResource(R.string.custom)
            }

            ParameterItem(
                icon = Icons.Default.Repeat,
                label = stringResource(R.string.recurrence),
                value = recurrenceText
            )
        }

        // Матрица Эйзенхауэра
        if (eisenhowerQuadrant != null) {
            val quadrantText = when (eisenhowerQuadrant) {
                1 -> stringResource(R.string.important_urgent)
                2 -> stringResource(R.string.important_not_urgent)
                3 -> stringResource(R.string.not_important_urgent)
                4 -> stringResource(R.string.not_important_not_urgent)
                else -> "?"
            }

            val quadrantColor = when (eisenhowerQuadrant) {
                1 -> Color(0xFFE57373) // Красный
                2 -> Color(0xFF81C784) // Зеленый
                3 -> Color(0xFFFFB74D) // Оранжевый
                4 -> Color(0xFF64B5F6) // Синий
                else -> MaterialTheme.colorScheme.onSurface
            }

            ParameterItem(
                icon = Icons.Default.GridView,
                label = stringResource(R.string.eisenhower_matrix),
                value = quadrantText,
                color = quadrantColor
            )
        }
    }
}

@Composable
fun ParameterItem(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color ?: MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagsSection(tags: List<Tag>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = stringResource(R.string.tags),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tags.forEach { tag ->
                TagChip(tag = tag)
            }
        }
    }
}

@Composable
fun TagChip(tag: Tag) {
    val tagColor = tag.color?.toColor() ?: MaterialTheme.colorScheme.primary

    Surface(
        color = tagColor.copy(alpha = 0.1f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, tagColor)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 6.dp),
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
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun SubtasksSection(
    subtasks: List<Subtask>,
    onToggleSubtask: (Subtask) -> Unit
) {
    val completedSubtasks = subtasks.count { it.isCompleted }
    val progress = if (subtasks.isNotEmpty()) completedSubtasks.toFloat() / subtasks.size else 0f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.subtasks),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "$completedSubtasks / ${subtasks.size}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Прогресс выполнения подзадач
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )

        // Список подзадач
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            subtasks.forEach { subtask ->
                SubtaskItem(subtask = subtask, onToggleCompletion = { onToggleSubtask(subtask) })
            }
        }
    }
}

@Composable
fun SubtaskItem(
    subtask: Subtask,
    onToggleCompletion: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = subtask.isCompleted,
            onCheckedChange = { onToggleCompletion() }
        )

        Text(
            text = subtask.title,
            style = MaterialTheme.typography.bodyMedium,
            textDecoration = if (subtask.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
            color = if (subtask.isCompleted)
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            else
                MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun PomodoroSection(
    estimatedSessions: Int?,
    totalFocusTime: Int?,
    onStartSession: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = stringResource(R.string.pomodoro),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Column {
                            if (estimatedSessions != null) {
                                Text(
                                    text = stringResource(R.string.estimated_sessions),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Text(
                                    text = stringResource(R.string.pomodoro_sessions_count, estimatedSessions),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            if (totalFocusTime != null && totalFocusTime > 0) {
                                Text(
                                    text = stringResource(R.string.total_focus_time),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = if (estimatedSessions != null) 8.dp else 0.dp)
                                )

                                Text(
                                    text = formatDuration(totalFocusTime),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    // Кнопка запуска Pomodoro
                    Button(
                        onClick = onStartSession,
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        Text(text = stringResource(R.string.start_session))
                    }
                }
            }
        }
    }
}

@Composable
fun DeleteTaskDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_task)) },
        text = { Text(stringResource(R.string.delete_task_confirmation)) },
        confirmButton = {
            TextButton(
                onClick = onConfirm
            ) {
                Text(
                    text = stringResource(R.string.delete),
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

// Вспомогательные функции для форматирования
// (предполагая, что функция formatDuration определена в util пакете)

// Если функция formatDuration не определена, вот её реализация:
fun formatDuration(minutesTotal: Int): String {
    val hours = minutesTotal / 60
    val minutes = minutesTotal % 60

    return if (hours > 0) {
        "$hours ч $minutes мин"
    } else {
        "$minutes мин"
    }
}
