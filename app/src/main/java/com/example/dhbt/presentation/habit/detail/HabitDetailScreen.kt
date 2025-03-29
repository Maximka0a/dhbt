@file:OptIn(ExperimentalLayoutApi::class)

package com.example.dhbt.presentation.habit.detail

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import com.example.dhbt.R
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.example.dhbt.domain.model.*
import com.example.dhbt.presentation.components.ConfirmDeleteDialog
import com.example.dhbt.presentation.habit.components.CalendarHeatMap
import com.example.dhbt.presentation.habit.components.StatsBarGraph
import com.example.dhbt.presentation.navigation.HabitEdit
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitDetailScreen(
    navController: NavController,
    viewModel: HabitDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val habit by viewModel.habit.collectAsState()
    val category by viewModel.category.collectAsState()
    val tags by viewModel.tags.collectAsState()
    val todayProgress by viewModel.todayProgress.collectAsState()
    val todayIsCompleted by viewModel.todayIsCompleted.collectAsState()
    val calendarData by viewModel.calendarData.collectAsState()
    val weeklyCompletion by viewModel.weeklyCompletion.collectAsState()
    val monthlyCompletion by viewModel.monthlyCompletion.collectAsState()
    val selectedChartPeriod by viewModel.selectedChartPeriod.collectAsState()
    val showDeleteDialog by viewModel.showDeleteDialog.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Получение цвета привычки с поддержкой темной/светлой темы
    val habitColor = calculateHabitColor(habit)

    // Добавляем состояние для отслеживания анимации FAB
    var isFabExtended by remember { mutableStateOf(true) }

    // Анимируем видимость FAB при прокрутке
    LaunchedEffect(scrollState) {
        snapshotFlow { scrollState.isScrollInProgress }
            .collect { isScrolling ->
                isFabExtended = !isScrolling
            }
    }

    // Обработка событий
    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is HabitDetailEvent.ProgressUpdated -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
                is HabitDetailEvent.HabitStatusChanged -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
                is HabitDetailEvent.ShareHabit -> {
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, event.text)
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, null))
                }
                is HabitDetailEvent.HabitDeleted -> {
                    Toast.makeText(context, "Привычка удалена", Toast.LENGTH_SHORT).show()
                    navController.popBackStack()
                }
                is HabitDetailEvent.Error -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            text = habit?.title ?: "Привычка",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.headlineMedium
                        )
                        if (habit?.iconEmoji != null) {
                            Text(
                                text = habit?.iconEmoji ?: "",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = "Назад",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    // Кнопка редактирования
                    IconButton(
                        onClick = { navController.navigate(HabitEdit(habit?.id ?: "")) }
                    ) {
                        Icon(
                            Icons.Rounded.Edit,
                            contentDescription = "Редактировать привычку",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Кнопка удаления
                    IconButton(
                        onClick = { viewModel.onAction(HabitDetailAction.ShowDeleteDialog(true)) }
                    ) {
                        Icon(
                            Icons.Rounded.Delete,
                            contentDescription = "Удалить привычку",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.onAction(HabitDetailAction.ToggleCompletion) },
                containerColor = habitColor,
                contentColor = MaterialTheme.colorScheme.surface,
                expanded = isFabExtended,
                icon = {
                    val completionIcon = if (todayIsCompleted)
                        Icons.Rounded.CheckCircle else
                        Icons.Rounded.RadioButtonUnchecked

                    Icon(
                        imageVector = completionIcon,
                        contentDescription = stringResource(R.string.mark_habit)
                    )
                },
                text = {
                    AnimatedVisibility(
                        visible = isFabExtended,
                        enter = fadeIn() + expandHorizontally(),
                        exit = fadeOut() + shrinkHorizontally()
                    ) {
                        Text(
                            text = if (todayIsCompleted)
                                stringResource(R.string.completed)
                            else
                                stringResource(R.string.mark),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                },
                modifier = Modifier.animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                // Отображение загрузки
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = habitColor)
                }
            } else if (uiState.error != null) {
                // Отображение ошибки
                ErrorState(
                    errorMessage = uiState.error ?: "Неизвестная ошибка",
                    onBackClick = { navController.popBackStack() }
                )
            } else {
                // Основной контент экрана
                MainContent(
                    scrollState = scrollState,
                    habit = habit,
                    category = category,
                    tags = tags,
                    todayProgress = todayProgress,
                    todayIsCompleted = todayIsCompleted,
                    calendarData = calendarData,
                    weeklyCompletion = weeklyCompletion,
                    monthlyCompletion = monthlyCompletion,
                    selectedChartPeriod = selectedChartPeriod,
                    habitColor = habitColor,
                    viewModel = viewModel
                )
            }
        }
    }

    // Диалог подтверждения удаления
    if (showDeleteDialog) {
        ConfirmDeleteDialog(
            title = "Удалить привычку?",
            text = "Вы уверены, что хотите удалить привычку «${habit?.title}»? Это действие невозможно отменить.",
            onConfirm = { viewModel.onAction(HabitDetailAction.DeleteHabit) },
            onDismiss = { viewModel.onAction(HabitDetailAction.ShowDeleteDialog(false)) }
        )
    }
}

@Composable
fun ErrorState(
    errorMessage: String,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(80.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Что-то пошло не так",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onBackClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                Icons.Rounded.ArrowBack,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Вернуться назад")
        }
    }
}

@Composable
fun MainContent(
    scrollState: ScrollState,
    habit: Habit?,
    category: Category?,
    tags: List<Tag>,
    todayProgress: Float,
    todayIsCompleted: Boolean,
    calendarData: Map<LocalDate, Float>,
    weeklyCompletion: List<Float>,
    monthlyCompletion: List<Float>,
    selectedChartPeriod: ChartPeriod,
    habitColor: Color,
    viewModel: HabitDetailViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 80.dp) // Пространство для FAB
    ) {
        // Карта прогресса и основная информация
        ProgressSection(
            progress = todayProgress,
            isCompleted = todayIsCompleted,
            habitType = habit?.type ?: HabitType.BINARY,
            progressText = viewModel.getCurrentProgressText(),
            description = habit?.description,
            habitColor = habitColor,
            onIncrementClick = { viewModel.onAction(HabitDetailAction.IncrementProgress) },
            onDecrementClick = { viewModel.onAction(HabitDetailAction.DecrementProgress) }
        )

        // Секция статистики серий
        StreakSection(
            currentStreak = habit?.currentStreak ?: 0,
            bestStreak = habit?.bestStreak ?: 0,
            habitColor = habitColor
        )

        // Календарь и график
        StatsSection(
            calendarData = calendarData,
            weeklyData = weeklyCompletion,
            monthlyData = monthlyCompletion,
            selectedPeriod = selectedChartPeriod,
            onPeriodSelected = { viewModel.onAction(HabitDetailAction.SetChartPeriod(it)) },
            habitColor = habitColor
        )

        // Информация о настройках
        DetailsSection(
            frequencyText = viewModel.getFrequencyText(),
            targetValueText = viewModel.getTargetValueText(),
            category = category,
            tags = tags,
            habitColor = habitColor
        )

        // Дополнительные действия
        ActionsSection(
            onShareClick = { viewModel.onAction(HabitDetailAction.ShareHabit) },
            onArchiveClick = { viewModel.onAction(HabitDetailAction.ArchiveHabit) },
            isArchived = habit?.status == HabitStatus.ARCHIVED
        )
    }
}

@Composable
fun ProgressSection(
    progress: Float,
    isCompleted: Boolean,
    habitType: HabitType,
    progressText: String,
    description: String?,
    habitColor: Color,
    onIncrementClick: () -> Unit,
    onDecrementClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            // Прогресс
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(150.dp)
                    .padding(8.dp)
            ) {
                // Анимированный прогресс
                val animatedProgress = animateFloatAsState(
                    targetValue = progress.coerceIn(0f, 1f), // Cap at 1.0 for visual indicator
                    animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
                    label = "ProgressAnimation"
                )


                // Фоновый индикатор
                CircularProgressIndicator(
                    progress = 1f,
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    strokeWidth = 16.dp,
                    strokeCap = StrokeCap.Round
                )

                // Прогресс пользователя
                CircularProgressIndicator(
                    progress = { animatedProgress.value },
                    modifier = Modifier.fillMaxSize(),
                    color = habitColor,
                    strokeWidth = 16.dp,
                    strokeCap = StrokeCap.Round
                )

                // Текст внутри индикатора
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (habitType == HabitType.BINARY) {
                        // Анимация иконки
                        val transition = updateTransition(isCompleted, label = "CompletionTransition")
                        val scale by transition.animateFloat(
                            label = "IconScale",
                            transitionSpec = { spring(stiffness = Spring.StiffnessLow) }
                        ) { completed -> if (completed) 1.2f else 1f }

                        Icon(
                            imageVector = if (isCompleted) Icons.Rounded.Check else Icons.Rounded.Close,
                            contentDescription = null,
                            tint = if (isCompleted) habitColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(48.dp)
                                .scale(scale)
                        )
                    } else {
                        val displayPercent = (progress * 100).toInt()
                        Text(
                            text = "$displayPercent%",
                            style = MaterialTheme.typography.displaySmall,
                            color = habitColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Описание прогресса
            Text(
                text = progressText,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Кнопки количественного прогресса
            if (habitType != HabitType.BINARY) {
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onDecrementClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = habitColor.copy(alpha = 0.1f),
                            contentColor = habitColor
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.Remove, contentDescription = "Уменьшить")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Уменьшить")
                    }

                    Button(
                        onClick = onIncrementClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = habitColor
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "Увеличить")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Увеличить")
                    }
                }
            }

            // Описание привычки
            if (!description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun StreakSection(
    currentStreak: Int,
    bestStreak: Int,
    habitColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // Текущая серия
        StreakCard(
            value = currentStreak,
            label = "Текущая серия",
            icon = Icons.Rounded.LocalFireDepartment,
            color = habitColor,
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        )

        // Лучшая серия
        StreakCard(
            value = bestStreak,
            label = "Рекордная серия",
            icon = Icons.Rounded.EmojiEvents,
            color = habitColor,
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp)
        )
    }
}

@Composable
fun StreakCard(
    value: Int,
    label: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Анимированное число
            val animatedValue = remember { Animatable(0f) }

            LaunchedEffect(value) {
                animatedValue.animateTo(
                    targetValue = value.toFloat(),
                    animationSpec = tween(
                        durationMillis = 1000,
                        easing = FastOutSlowInEasing
                    )
                )
            }

            Text(
                text = animatedValue.value.toInt().toString(),
                style = MaterialTheme.typography.headlineLarge,
                color = color,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun StatsSection(
    calendarData: Map<LocalDate, Float>,
    weeklyData: List<Float>,
    monthlyData: List<Float>,
    selectedPeriod: ChartPeriod,
    onPeriodSelected: (ChartPeriod) -> Unit,
    habitColor: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            // Заголовок секции
            Text(
                text = "Статистика",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Календарь активности
            Text(
                text = "История выполнения",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Тепловая карта календаря
            CalendarHeatMap(
                data = calendarData,
                startDate = LocalDate.now().minusDays(29),
                endDate = LocalDate.now(),
                colorEmpty = MaterialTheme.colorScheme.surfaceVariant,
                colorFilled = habitColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Переключатель периода графика
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Прогресс по дням",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )

                SegmentedButtons(
                    selectedPeriod = selectedPeriod,
                    onPeriodSelected = onPeriodSelected,
                    habitColor = habitColor
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Граф данных
            AnimatedContent(
                targetState = selectedPeriod,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) +
                            slideInHorizontally(
                                animationSpec = tween(300),
                                initialOffsetX = { if (targetState == ChartPeriod.WEEK) it else -it }
                            ) with
                            fadeOut(animationSpec = tween(300)) +
                            slideOutHorizontally(
                                animationSpec = tween(300),
                                targetOffsetX = { if (targetState == ChartPeriod.WEEK) -it else it }
                            )
                },
                label = "ChartAnimation"
            ) { period ->
                when (period) {
                    ChartPeriod.WEEK -> {
                        StatsBarGraph(
                            data = weeklyData,
                            maxValue = 1f,
                            barColor = habitColor,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            labels = getWeekLabels(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .padding(top = 8.dp)
                        )
                    }
                    ChartPeriod.MONTH -> {
                        StatsBarGraph(
                            data = monthlyData,
                            maxValue = 1f,
                            barColor = habitColor,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            labels = getMonthLabels(30),
                            showAllLabels = false,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SegmentedButtons(
    selectedPeriod: ChartPeriod,
    onPeriodSelected: (ChartPeriod) -> Unit,
    habitColor: Color
) {
    Row(
        modifier = Modifier
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(24.dp)
            )
            .clip(RoundedCornerShape(24.dp))
    ) {
        SegmentedButton(
            selected = selectedPeriod == ChartPeriod.WEEK,
            onClick = { onPeriodSelected(ChartPeriod.WEEK) },
            text = "Неделя",
            habitColor = habitColor,
            modifier = Modifier.weight(1f)
        )

        SegmentedButton(
            selected = selectedPeriod == ChartPeriod.MONTH,
            onClick = { onPeriodSelected(ChartPeriod.MONTH) },
            text = "Месяц",
            habitColor = habitColor,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun SegmentedButton(
    selected: Boolean,
    onClick: () -> Unit,
    text: String,
    habitColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clickable(onClick = onClick)
            .background(
                color = if (selected) habitColor.copy(alpha = 0.1f) else Color.Transparent,
                shape = RoundedCornerShape(24.dp)
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) habitColor else MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
        )
    }
}

@Composable
fun DetailsSection(
    frequencyText: String,
    targetValueText: String,
    category: Category?,
    tags: List<Tag>,
    habitColor: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            // Заголовок секции
            Text(
                text = "Информация",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Частота выполнения
            DetailItem(
                icon = Icons.Rounded.Repeat,
                label = "Частота",
                value = frequencyText,
                color = habitColor
            )

            Divider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            )

            // Целевое значение
            DetailItem(
                icon = Icons.Rounded.Flag,
                label = "Цель",
                value = targetValueText,
                color = habitColor
            )

            // Категория
            if (category != null) {
                Divider(
                    modifier = Modifier.padding(vertical = 16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                )

                val categoryColor = try {
                    category.color?.let { Color(android.graphics.Color.parseColor(it)) } ?: habitColor
                } catch (e: Exception) {
                    habitColor
                }

                DetailItem(
                    icon = Icons.Rounded.Folder,
                    label = "Категория",
                    value = category.name,
                    color = categoryColor,
                    chip = true
                )
            }

            // Теги
            if (tags.isNotEmpty()) {
                Divider(
                    modifier = Modifier.padding(vertical = 16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                )

                Column {
                    Text(
                        text = "Теги",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        tags.forEach { tag ->
                            val tagColor = try {
                                tag.color?.let { Color(android.graphics.Color.parseColor(it)) } ?: habitColor
                            } catch (e: Exception) {
                                habitColor
                            }

                            TagChip(
                                text = tag.name,
                                color = tagColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailItem(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color,
    chip: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Иконка
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Информация
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            if (chip) {
                SuggestionChip(
                    onClick = { },
                    label = {
                        Text(
                            text = value,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    icon = {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                    },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = color.copy(alpha = 0.1f),
                        labelColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            } else {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun TagChip(
    text: String,
    color: Color
) {
    SuggestionChip(
        onClick = { },
        label = { Text(text = text) },
        icon = {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        },
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = color.copy(alpha = 0.1f),
            labelColor = MaterialTheme.colorScheme.onSurface
        ),
        border = null
    )
}

@Composable
fun ActionsSection(
    onShareClick: () -> Unit,
    onArchiveClick: () -> Unit,
    isArchived: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Кнопка поделиться
            OutlinedButton(
                onClick = onShareClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Share,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Поделиться привычкой")
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Кнопка архивировать/восстановить
            OutlinedButton(
                onClick = onArchiveClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (isArchived)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.secondary
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Icon(
                    imageVector = if (isArchived)
                        Icons.Rounded.Unarchive
                    else
                        Icons.Rounded.Archive,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isArchived) "Восстановить из архива" else "Архивировать привычку"
                )
            }
        }
    }
}

/**
 * Получает список подписей для дней недели
 */
@Composable
private fun getWeekLabels(): List<String> {
    // Сокращенные названия дней недели
    val today = LocalDate.now()
    val dayFormatter = DateTimeFormatter.ofPattern("EE")

    return (0..6).map { dayOffset ->
        val day = today.minusDays((today.dayOfWeek.value - 1).toLong()).plusDays(dayOffset.toLong())
        dayFormatter.format(day)
    }
}

/**
 * Получает список подписей для дней месяца
 */
private fun getMonthLabels(days: Int): List<String> {
    // Номера дней для последних N дней
    val today = LocalDate.now()

    return (days - 1 downTo 0).map { daysAgo ->
        val date = today.minusDays(daysAgo.toLong())
        date.dayOfMonth.toString()
    }
}


@Composable
private fun calculateHabitColor(habit: Habit?): Color {
    val primaryColor = MaterialTheme.colorScheme.primary

    return remember(habit, primaryColor) {
        if (habit?.color == null) {
            primaryColor
        } else {
            try {
                Color(android.graphics.Color.parseColor(habit.color))
            } catch (e: Exception) {
                primaryColor
            }
        }
    }
}