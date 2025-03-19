@file:OptIn(ExperimentalMaterialApi::class)

package com.example.dhbt.presentation.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.DismissDirection
import androidx.compose.material.DismissValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.rememberDismissState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.dhbt.R
import com.example.dhbt.domain.model.Habit
import com.example.dhbt.domain.model.Task
import com.example.dhbt.domain.model.TaskStatus
import com.example.dhbt.presentation.components.EmojiIcon
import com.example.dhbt.presentation.components.HabitCard
import com.example.dhbt.presentation.components.TaskCard
import com.example.dhbt.presentation.util.toColor
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onTaskClick: (String) -> Unit,
    onHabitClick: (String) -> Unit,
    onAddTask: () -> Unit,
    onAddHabit: () -> Unit,
    onViewAllTasks: () -> Unit,
    onViewAllHabits: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Обработка ошибок
    LaunchedEffect(state.error) {
        state.error?.let { errorMessage ->
            snackbarHostState.showSnackbar(message = errorMessage)
            viewModel.dismissError()
        }
    }

    Scaffold(
        topBar = {
            DashboardTopAppBar(
                userName = state.userData?.name ?: "Пользователь",
                formattedDate = viewModel.getTodayFormatted()
            )
        },
        floatingActionButton = {
            DashboardFab(
                onAddTask = onAddTask,
                onAddHabit = onAddHabit
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        if (state.isLoading) {
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
                    .padding(paddingValues),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Статистика
                item {
                    StatisticsCard(
                        completedTasks = state.completedTasks,
                        totalTasks = state.totalTasks,
                        completedHabits = state.completedHabits,
                        totalHabits = state.totalHabits
                    )
                }

                // Заголовок секции привычек
                item {
                    SectionHeader(
                        title = stringResource(R.string.habits_for_today),
                        onSeeAllClick = onViewAllHabits
                    )
                }

                // Привычки на сегодня
                item {
                    if (state.todayHabits.isEmpty()) {
                        EmptyStateMessage(message = stringResource(R.string.no_habits_for_today))
                    } else {
                        HabitsRow(
                            habits = state.todayHabits,
                            onHabitClick = onHabitClick,
                            onHabitProgressIncrement = viewModel::onHabitProgressIncrement
                        )
                    }
                }

                // Заголовок секции задач
                item {
                    SectionHeader(
                        title = stringResource(R.string.tasks_for_today),
                        onSeeAllClick = onViewAllTasks
                    )
                }

                // Задачи на сегодня
                if (state.todayTasks.isEmpty()) {
                    item {
                        EmptyStateMessage(message = stringResource(R.string.no_tasks_for_today))
                    }
                } else {
                    // Список задач
                    items(state.todayTasks) { task ->
                        SwipeableTaskItem(
                            task = task,
                            onTaskClick = onTaskClick,
                            onTaskCompleteChange = { isCompleted ->
                                viewModel.onTaskCheckedChange(task.id, isCompleted)
                            },
                            onDeleteTask = { viewModel.onDeleteTask(task.id) }
                        )
                    }
                }

                // Дополнительное пространство внизу
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardTopAppBar(
    userName: String,
    formattedDate: String
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = "Привет, $userName!",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        },
        actions = {
            IconButton(onClick = { /* Действие настроек */ }) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.settings)
                )
            }
        }
    )
}

@Composable
fun DashboardFab(
    onAddTask: () -> Unit,
    onAddHabit: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(horizontalAlignment = Alignment.End) {
        // Анимированное отображение мини-меню
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically()
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                // Кнопка добавления привычки
                ExtendedFloatingActionButton(
                    onClick = {
                        expanded = false
                        onAddHabit()
                    },
                    icon = { Icon(Icons.Default.Loop, contentDescription = null) },
                    text = { Text(stringResource(R.string.add_habit)) },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )

                // Кнопка добавления задачи
                ExtendedFloatingActionButton(
                    onClick = {
                        expanded = false
                        onAddTask()
                    },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.add_task)) },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // Основная FAB
        FloatingActionButton(
            onClick = { expanded = !expanded }
        ) {
            Icon(
                imageVector = if (expanded) Icons.Default.Close else Icons.Default.Add,
                contentDescription = if (expanded)
                    stringResource(R.string.close)
                else
                    stringResource(R.string.add)
            )
        }
    }
}

@Composable
fun StatisticsCard(
    completedTasks: Int,
    totalTasks: Int,
    completedHabits: Int,
    totalHabits: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.today_progress),
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Статистика по задачам
                ProgressMetric(
                    icon = Icons.Default.Task,
                    title = stringResource(R.string.tasks),
                    completed = completedTasks,
                    total = totalTasks,
                    modifier = Modifier.weight(1f)
                )

                // Статистика по привычкам
                ProgressMetric(
                    icon = Icons.Default.Loop,
                    title = stringResource(R.string.habits),
                    completed = completedHabits,
                    total = totalHabits,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun ProgressMetric(
    icon: ImageVector,
    title: String,
    completed: Int,
    total: Int,
    modifier: Modifier = Modifier
) {
    val progress = if (total > 0) completed.toFloat() / total else 0f
    val progressColor = animateColorAsState(
        targetValue = when {
            progress >= 0.8f -> Color(0xFF4CAF50) // Зеленый
            progress >= 0.5f -> Color(0xFFFFC107) // Желтый
            else -> if (total == 0) MaterialTheme.colorScheme.primary else Color(0xFFF44336) // Красный
        },
        animationSpec = tween(300)
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.size(56.dp),
            color = progressColor.value,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "$completed/$total",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    onSeeAllClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium
        )

        TextButton(onClick = onSeeAllClick) {
            Text(stringResource(R.string.see_all))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HabitsRow(
    habits: List<Habit>,
    onHabitClick: (String) -> Unit,
    onHabitProgressIncrement: (String) -> Unit
) {
    val hapticFeedback = LocalHapticFeedback.current

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(habits) { habit ->
            Card(
                modifier = Modifier
                    .width(160.dp)
                    .height(130.dp)
                    .combinedClickable(
                        onClick = { onHabitClick(habit.id) },
                        onLongClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            onHabitProgressIncrement(habit.id)
                        }
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Иконка привычки
                        EmojiIcon(
                            emoji = habit.iconEmoji,
                            backgroundColor = habit.color.toColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier.size(32.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Название привычки
                        Text(
                            text = habit.title,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Прогресс-бар
                    val progress = when (habit.type.value) {
                        1, 2 -> { // QUANTITY или TIME
                            val target = habit.targetValue ?: 1f
                            if (target > 0f) (habit.currentStreak.toFloat() / target).coerceIn(0f, 1f)
                            else 0f
                        }
                        else -> if (habit.status.value == 1) 1f else 0f // BINARY
                    }

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp),
                        color = habit.color.toColor(MaterialTheme.colorScheme.primary),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Текст прогресса
                    val progressText = when (habit.type.value) {
                        1 -> "${habit.currentStreak}/${habit.targetValue?.toInt() ?: 0} ${habit.unitOfMeasurement ?: ""}"
                        2 -> "${habit.currentStreak} мин"
                        else -> if (habit.status.value == 1) "Выполнено" else "Не выполнено"
                    }

                    Text(
                        text = progressText,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
fun SwipeableTaskItem(
    task: Task,
    onTaskClick: (String) -> Unit,
    onTaskCompleteChange: (Boolean) -> Unit,
    onDeleteTask: () -> Unit
) {
    // Реализуем свайп через SwipeToDismiss
    var dismissState = rememberDismissState(
        confirmStateChange = { dismissValue ->
            when (dismissValue) {
                DismissValue.DismissedToStart -> {
                    // Показать диалог удаления и удалить если подтверждено
                    onDeleteTask()
                    false // не удаляем визуально, обновление придет через Flow
                }
                DismissValue.DismissedToEnd -> {
                    // Переключить состояние выполнения
                    onTaskCompleteChange(task.status != TaskStatus.COMPLETED)
                    false // не удаляем визуально, обновление придет через Flow
                }
                DismissValue.Default -> false
            }
        }
    )

    SwipeToDismiss(
        state = dismissState,
        background = {
            val direction = dismissState.dismissDirection
            val color by animateColorAsState(
                when (direction) {
                    DismissDirection.StartToEnd -> Color(0xFF4CAF50) // Зеленый для выполнения
                    DismissDirection.EndToStart -> Color(0xFFF44336) // Красный для удаления
                    null -> Color.Transparent
                }
            )

            val alignment = when (direction) {
                DismissDirection.StartToEnd -> Alignment.CenterStart
                DismissDirection.EndToStart -> Alignment.CenterEnd
                null -> Alignment.Center
            }

            val icon = when (direction) {
                DismissDirection.StartToEnd -> Icons.Default.CheckCircle
                DismissDirection.EndToStart -> Icons.Default.Delete
                null -> Icons.Default.MoreVert
            }

            Box(
                Modifier
                    .fillMaxSize()
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = alignment
            ) {
                Icon(
                    icon,
                    contentDescription = when (direction) {
                        DismissDirection.StartToEnd -> "Выполнить"
                        DismissDirection.EndToStart -> "Удалить"
                        null -> null
                    },
                    tint = Color.White
                )
            }
        },
        dismissContent = {
            TaskCard(
                task = task,
                onTaskClick = onTaskClick,
                onCompleteToggle = { _, isCompleted -> onTaskCompleteChange(isCompleted) }
            )
        },
        directions = setOf(DismissDirection.StartToEnd, DismissDirection.EndToStart)
    )
}

@Composable
fun EmptyStateMessage(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}