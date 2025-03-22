package com.example.dhbt.presentation.pomodoro

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Task
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.example.dhbt.R
import com.example.dhbt.domain.model.PomodoroSessionType
import com.example.dhbt.domain.model.Task
import com.example.dhbt.presentation.navigation.TaskEdit
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PomodoroScreen(
    navController: NavController,
    viewModel: PomodoroViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val preferences by viewModel.preferences.collectAsState()
    val recentTasks by viewModel.recentTasks.collectAsState()
    val todayStats by viewModel.todayStats.collectAsState()
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Диалоги
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showTaskSelectionDialog by remember { mutableStateOf(false) }

    // При возвращении на экран Pomodoro, обновляем статистику
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadTodayStats()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // BackHandler для корректной обработки кнопки назад при активном таймере
    BackHandler(enabled = true) {
        if (viewModel.handleBackPressed()) {
            navController.popBackStack()
        }
    }

    // Цвета для разных типов сессий
    val sessionColor = when (uiState.pomodoroSessionType) {
        PomodoroSessionType.WORK -> MaterialTheme.colorScheme.primary
        PomodoroSessionType.SHORT_BREAK -> MaterialTheme.colorScheme.secondary
        PomodoroSessionType.LONG_BREAK -> MaterialTheme.colorScheme.tertiary
    }

    // Анимация пульсации для активного таймера
    val pulsateAnimation = rememberInfiniteTransition(label = "pulsate")
    val pulsateSize by pulsateAnimation.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulsate"
    )

    // Анимированное значение для прогресса таймера
    val progress = when (uiState.timerType) {
        TimerType.POMODORO -> {
            if (uiState.totalTime > 0) {
                1 - (uiState.remainingTime.toFloat() / uiState.totalTime.toFloat())
            } else 0f
        }
        TimerType.STOPWATCH -> {
            // Для секундомера ограничиваем максимальное значение для индикатора
            val maxValue = TimeUnit.MINUTES.toMillis(60) // Максимум 60 минут для индикатора
            (uiState.elapsedTime.toFloat() / maxValue).coerceAtMost(1f)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.pomodoro)) },
                actions = {
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.pomodoro_settings)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Добавляем дополнительный отступ от верхней панели
            Spacer(modifier = Modifier.height(8.dp))

            // Режимы таймера (Pomodoro/Секундомер)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                TabRow(
                    selectedTabIndex = uiState.timerType.ordinal,
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .clip(RoundedCornerShape(50))
                ) {
                    Tab(
                        selected = uiState.timerType == TimerType.POMODORO,
                        onClick = { viewModel.onTimerTypeSelected(TimerType.POMODORO) },
                        text = { Text(stringResource(R.string.pomodoro)) }
                    )
                    Tab(
                        selected = uiState.timerType == TimerType.STOPWATCH,
                        onClick = { viewModel.onTimerTypeSelected(TimerType.STOPWATCH) },
                        text = { Text(stringResource(R.string.stopwatch)) }
                    )
                }
            }

            // Режимы Pomodoro (если выбран Pomodoro)
            AnimatedVisibility(
                visible = uiState.timerType == TimerType.POMODORO,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                ) {
                    PomodoroModeButton(
                        text = stringResource(R.string.work),
                        selected = uiState.pomodoroSessionType == PomodoroSessionType.WORK,
                        onClick = { viewModel.onPomodoroSessionTypeSelected(PomodoroSessionType.WORK) }
                    )

                    PomodoroModeButton(
                        text = stringResource(R.string.short_break),
                        selected = uiState.pomodoroSessionType == PomodoroSessionType.SHORT_BREAK,
                        onClick = { viewModel.onPomodoroSessionTypeSelected(PomodoroSessionType.SHORT_BREAK) }
                    )

                    PomodoroModeButton(
                        text = stringResource(R.string.long_break),
                        selected = uiState.pomodoroSessionType == PomodoroSessionType.LONG_BREAK,
                        onClick = { viewModel.onPomodoroSessionTypeSelected(PomodoroSessionType.LONG_BREAK) }
                    )
                }
            }

            // Визуализация таймера
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(240.dp * (if (uiState.timerState == TimerState.RUNNING) pulsateSize else 1f))
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                // Фоновый круг для индикации прогресса
                CircularProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .matchParentSize()
                        .padding(12.dp),
                    strokeWidth = 12.dp,
                    color = sessionColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )

                // Основной счетчик времени
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (uiState.timerType) {
                        TimerType.POMODORO -> {
                            val minutes = TimeUnit.MILLISECONDS.toMinutes(uiState.remainingTime)
                            val seconds = TimeUnit.MILLISECONDS.toSeconds(uiState.remainingTime) % 60

                            Text(
                                text = String.format("%02d:%02d", minutes, seconds),
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 48.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = when (uiState.pomodoroSessionType) {
                                    PomodoroSessionType.WORK -> stringResource(R.string.work)
                                    PomodoroSessionType.SHORT_BREAK -> stringResource(R.string.short_break)
                                    PomodoroSessionType.LONG_BREAK -> stringResource(R.string.long_break)
                                },
                                style = MaterialTheme.typography.titleMedium
                            )

                            // Счетчик выполненных помидоров
                            if (uiState.pomodoroSessionType == PomodoroSessionType.WORK) {
                                Spacer(modifier = Modifier.height(4.dp))

                                val denominator = if (uiState.selectedTask != null && uiState.totalTaskSessions > 0) {
                                    uiState.totalTaskSessions
                                } else {
                                    preferences.pomodorosUntilLongBreak
                                }

                                Text(
                                    text = "${uiState.completedPomodoros + 1}/${denominator}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                        TimerType.STOPWATCH -> {
                            val hours = TimeUnit.MILLISECONDS.toHours(uiState.elapsedTime)
                            val minutes = TimeUnit.MILLISECONDS.toMinutes(uiState.elapsedTime) % 60
                            val seconds = TimeUnit.MILLISECONDS.toSeconds(uiState.elapsedTime) % 60

                            if (hours > 0) {
                                Text(
                                    text = String.format("%d:%02d:%02d", hours, minutes, seconds),
                                    style = MaterialTheme.typography.displayLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 42.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            } else {
                                Text(
                                    text = String.format("%02d:%02d", minutes, seconds),
                                    style = MaterialTheme.typography.displayLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 48.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = stringResource(R.string.stopwatch),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }

            // Кнопки управления таймером
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (uiState.timerState) {
                    TimerState.RUNNING -> {
                        // Кнопка паузы
                        FilledIconButton(
                            onClick = { viewModel.pauseTimer() },
                            modifier = Modifier.size(64.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Pause,
                                contentDescription = stringResource(R.string.pause),
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Кнопка остановки
                        OutlinedIconButton(
                            onClick = { viewModel.stopTimer() },
                            modifier = Modifier.size(64.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = stringResource(R.string.stop),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    TimerState.PAUSED -> {
                        // Кнопка продолжения
                        FilledIconButton(
                            onClick = { viewModel.resumeTimer() },
                            modifier = Modifier.size(64.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = stringResource(R.string.start),
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Кнопка остановки
                        OutlinedIconButton(
                            onClick = { viewModel.stopTimer() },
                            modifier = Modifier.size(64.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = stringResource(R.string.stop),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    else -> {
                        // Кнопка старта
                        FilledIconButton(
                            onClick = { viewModel.startTimer() },
                            modifier = Modifier.size(72.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = stringResource(R.string.start),
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                }
            }

            // Выбор задачи (если выбран Pomodoro)
            AnimatedVisibility(
                visible = uiState.timerType == TimerType.POMODORO,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable { showTaskSelectionDialog = true },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.selected_task),
                            style = MaterialTheme.typography.labelLarge
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (uiState.selectedTask != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Task,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = uiState.selectedTask!!.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f)
                                )

                                IconButton(onClick = { viewModel.onTaskSelected(null) }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = stringResource(R.string.clear)
                                    )
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.select_task_for_pomodoro),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )

                                IconButton(onClick = { showTaskSelectionDialog = true }) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = stringResource(R.string.select_task)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            TaskProgressCard(
                taskStats = viewModel.taskStats.collectAsState().value,
                selectedTask = uiState.selectedTask,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            // Статистика
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.today_statistics),
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (viewModel.statsLoading.collectAsState().value) {
                        // Показываем индикатор загрузки
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp))
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatisticItem(
                                title = stringResource(R.string.completed_sessions),
                                value = todayStats.completedSessions.toString(),
                                modifier = Modifier.weight(1f)
                            )

                            Divider(
                                modifier = Modifier
                                    .height(40.dp)
                                    .width(1.dp)
                            )

                            StatisticItem(
                                title = stringResource(R.string.focus_time),
                                value = formatMinutesAsTimeString(todayStats.totalFocusMinutes),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Добавляем дополнительный отступ внизу для удобства прокрутки
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Диалоги
    if (showSettingsDialog) {
        PomodoroSettingsDialog(
            pomodoroPreferences = preferences,
            onDismiss = { showSettingsDialog = false },
            onSaveSettings = { newPreferences ->
                viewModel.updatePreferences(newPreferences)
            }
        )
    }

    if (showTaskSelectionDialog) {
        TaskSelectionDialog(
            tasks = recentTasks,
            isLoading = viewModel.tasksLoading.collectAsState().value,
            onDismiss = { showTaskSelectionDialog = false },
            onTaskSelected = { task ->
                viewModel.onTaskSelected(task)
            },
            onAddNewTask = {
                // Переход к экрану создания задачи
                navController.navigate(TaskEdit())
            }
        )
    }
}

@Composable
fun PomodoroModeButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    val textColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            ),
            color = textColor
        )
    }
}

@Composable
fun StatisticItem(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
    }
}


@Composable
fun TaskProgressCard(
    taskStats: TaskPomodoroStats,
    selectedTask: Task?,
    modifier: Modifier = Modifier
) {
    if (selectedTask == null) return

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.task_progress),
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Прогресс по сессиям
            if (taskStats.hasEstimatedSessions) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.pomodoro_sessions),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "${taskStats.completedSessions}/${taskStats.estimatedTotalSessions}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    LinearProgressIndicator(
                        progress = taskStats.sessionsProgress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Прогресс по времени
            if (taskStats.hasEstimatedTime) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.focus_time_progress),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "${taskStats.focusTimeMinutes}/${taskStats.estimatedTotalMinutes} ${stringResource(R.string.minutes_short)}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    LinearProgressIndicator(
                        progress = taskStats.timeProgress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.secondary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }

            // Если нет ни сессий, ни времени
            if (!taskStats.hasEstimatedTime && !taskStats.hasEstimatedSessions) {
                Text(
                    text = stringResource(R.string.no_estimated_time_sessions),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatMinutesAsTimeString(minutes: Int): String {
    val hours = minutes / 60
    val remainingMinutes = minutes % 60

    return if (hours > 0) {
        "$hours ч $remainingMinutes мин"
    } else {
        "$remainingMinutes мин"
    }
}