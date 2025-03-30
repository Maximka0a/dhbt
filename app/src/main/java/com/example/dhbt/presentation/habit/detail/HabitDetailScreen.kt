@file:OptIn(ExperimentalLayoutApi::class)

package com.example.dhbt.presentation.habit.detail

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.example.dhbt.R
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.example.dhbt.domain.model.*
import com.example.dhbt.presentation.components.ConfirmDeleteDialog
import com.example.dhbt.presentation.components.CustomSnackbarHost
import com.example.dhbt.presentation.components.SnackbarType
import com.example.dhbt.presentation.components.rememberSnackbarHostState
import com.example.dhbt.presentation.habit.components.getHabitColor
import com.example.dhbt.presentation.navigation.HabitEdit
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
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
    val currentProgress by viewModel.currentProgress.collectAsState()
    val isCompleted by viewModel.isCompleted.collectAsState()
    val calendarData by viewModel.calendarData.collectAsState()
    val weeklyCompletion by viewModel.weeklyCompletion.collectAsState()
    val monthlyCompletion by viewModel.monthlyCompletion.collectAsState()
    val selectedChartPeriod by viewModel.selectedChartPeriod.collectAsState()
    val showDeleteDialog by viewModel.showDeleteDialog.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val showDatePicker by viewModel.showDatePicker.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val snackbarHostState = rememberSnackbarHostState()

    val defaultColor  = MaterialTheme.colorScheme.primary
    // Get habit color with dark/light theme support
    val habitColor = getHabitColor(
        habit?.color,
        defaultColor
    )

    // Add state to track FAB animation
    var isFabExtended by remember { mutableStateOf(true) }

    // Animate FAB visibility on scroll
    LaunchedEffect(scrollState) {
        snapshotFlow { scrollState.isScrollInProgress }
            .collect { isScrolling ->
                isFabExtended = !isScrolling
            }
    }

    // Handle lifecycle events for refreshing data
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.reloadHabitData()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Handle events
    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is HabitDetailEvent.ProgressUpdated -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        type = event.type
                    )
                }
                is HabitDetailEvent.HabitStatusChanged -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        type = event.type
                    )
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
                    snackbarHostState.showSnackbar(
                        message = "Привычка удалена",
                        type = SnackbarType.SUCCESS
                    )
                    navController.popBackStack()
                }
                is HabitDetailEvent.Error -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        type = event.type
                    )
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
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    // Date selector
                    IconButton(
                        onClick = { viewModel.onAction(HabitDetailAction.ShowDatePicker(true)) }
                    ) {
                        Icon(
                            Icons.Rounded.DateRange,
                            contentDescription = "Выбрать дату",
                            tint = if (!viewModel.isSelectedDateToday()) habitColor else MaterialTheme.colorScheme.primary
                        )
                    }

                    // Edit button
                    IconButton(
                        onClick = { navController.navigate(HabitEdit(habit?.id ?: "")) }
                    ) {
                        Icon(
                            Icons.Rounded.Edit,
                            contentDescription = "Редактировать привычку",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Delete button
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
                    val completionIcon = if (isCompleted)
                        Icons.Rounded.CheckCircle else
                        Icons.Rounded.RadioButtonUnchecked

                    Icon(
                        imageVector = completionIcon,
                        contentDescription = "Отметить привычку"
                    )
                },
                text = {
                    AnimatedVisibility(
                        visible = isFabExtended,
                        enter = fadeIn() + expandHorizontally(),
                        exit = fadeOut() + shrinkHorizontally()
                    ) {
                        Text(
                            text = if (isCompleted) "Выполнено" else "Отметить",
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
        },
        snackbarHost = { CustomSnackbarHost(snackbarHostState = snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                // Loading indicator
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = habitColor)
                }
            } else if (uiState.error != null) {
                // Error state
                ErrorState(
                    errorMessage = uiState.error ?: "Неизвестная ошибка",
                    onBackClick = { navController.popBackStack() }
                )
            } else {
                // Main content
                RedesignedContent(
                    scrollState = scrollState,
                    habit = habit,
                    category = category,
                    tags = tags,
                    todayProgress = currentProgress,
                    todayIsCompleted = isCompleted,
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
    // Date picker dialog
    if (showDatePicker) {
        DatePickerDialog(
            onDateSelected = { date ->
                viewModel.onAction(HabitDetailAction.SelectDate(date))
            },
            onDismiss = {
                viewModel.onAction(HabitDetailAction.ShowDatePicker(false))
            },
            initialDate = selectedDate
        )
    }
    // Delete confirmation dialog
    if (showDeleteDialog) {
        ConfirmDeleteDialog(
            title = "Удалить привычку?",
            text = "Вы уверены, что хотите удалить привычку «${habit?.title}»? Это действие невозможно отменить.",
            onConfirm = { viewModel.onAction(HabitDetailAction.DeleteHabit) },
            onDismiss = { viewModel.onAction(HabitDetailAction.ShowDeleteDialog(false)) }
        )
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialog(
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    initialDate: LocalDate
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.toEpochDay() * 24 * 60 * 60 * 1000
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = {
                datePickerState.selectedDateMillis?.let { millis ->
                    // Convert from millis to LocalDate
                    val days = millis / (24 * 60 * 60 * 1000)
                    val date = LocalDate.ofEpochDay(days)
                    onDateSelected(date)
                }
                onDismiss()
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@Composable
fun RedesignedContent(
    scrollState: ScrollState,
    habit: Habit?,
    category: Category?,
    tags: List<Tag>,
    todayProgress: Float,  // Changed from currentProgress
    todayIsCompleted: Boolean,  // Changed from isCompleted
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
            .padding(bottom = 80.dp) // Space for FAB
    ) {
        // Hero section with emoji and description
        HeroSection(
            habit = habit,
            habitColor = habitColor
        )

        // Progress tracker
        ProgressTracker(
            progress = todayProgress,
            isCompleted = todayIsCompleted,
            habitType = habit?.type ?: HabitType.BINARY,
            progressText = viewModel.getCurrentProgressText(),
            habitColor = habitColor,
            onIncrementClick = { viewModel.onAction(HabitDetailAction.IncrementProgress) },
            onDecrementClick = { viewModel.onAction(HabitDetailAction.DecrementProgress) }
        )

        // Streak info
        StreakSection(
            currentStreak = habit?.currentStreak ?: 0,
            bestStreak = habit?.bestStreak ?: 0,
            habitColor = habitColor
        )

        // Statistics with MPAndroidChart
        StatisticsSection(
            calendarData = calendarData,
            weeklyData = weeklyCompletion,
            monthlyData = monthlyCompletion,
            selectedPeriod = selectedChartPeriod,
            onPeriodSelected = { viewModel.onAction(HabitDetailAction.SetChartPeriod(it)) },
            habitColor = habitColor
        )

        // Habit details (frequency, target, etc.)
        HabitDetailsSection(
            frequencyText = viewModel.getFrequencyText(),
            targetValueText = viewModel.getTargetValueText(),
            category = category,
            tags = tags,
            habitColor = habitColor
        )

        // Actions (share, archive)
        ActionsSection(
            onShareClick = { viewModel.onAction(HabitDetailAction.ShareHabit) },
            onArchiveClick = { viewModel.onAction(HabitDetailAction.ArchiveHabit) },
            isArchived = habit?.status == HabitStatus.ARCHIVED
        )
    }
}

@Composable
fun HeroSection(
    habit: Habit?,
    habitColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Emoji icon with animation
            if (!habit?.iconEmoji.isNullOrEmpty()) {
                Text(
                    text = habit?.iconEmoji ?: "",
                    style = MaterialTheme.typography.displayLarge,
                    modifier = Modifier
                        .padding(16.dp)
                        .animateContentSize()
                )
            }

            // Description
            if (!habit?.description.isNullOrEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = habit?.description ?: "",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ProgressTracker(
    progress: Float,
    isCompleted: Boolean,
    habitType: HabitType,
    progressText: String,
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
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Сегодняшний прогресс",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Circle progress indicator
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(120.dp)
                    .padding(8.dp)
            ) {
                val animatedProgress = animateFloatAsState(
                    targetValue = progress.coerceIn(0f, 1f),
                    animationSpec = tween(durationMillis = 1000),
                    label = "ProgressAnimation"
                )

                // Background indicator
                CircularProgressIndicator(
                    progress = 1f,
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    strokeWidth = 12.dp
                )

                // Progress indicator
                CircularProgressIndicator(
                    progress = { animatedProgress.value },
                    modifier = Modifier.fillMaxSize(),
                    color = habitColor,
                    strokeWidth = 12.dp
                )

                // Center content
                if (habitType == HabitType.BINARY) {
                    Icon(
                        imageVector = if (isCompleted)
                            Icons.Rounded.Check
                        else
                            Icons.Rounded.Close,
                        contentDescription = null,
                        tint = if (isCompleted) habitColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )
                } else {
                    val displayPercent = (progress * 100).toInt()
                    Text(
                        text = "$displayPercent%",
                        style = MaterialTheme.typography.headlineMedium,
                        color = habitColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = progressText,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )

            // Buttons for quantifiable habits
            if (habitType != HabitType.BINARY) {
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onDecrementClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Rounded.Remove,
                            contentDescription = "Уменьшить"
                        )
                    }

                    Button(
                        onClick = onIncrementClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = habitColor
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Rounded.Add,
                            contentDescription = "Увеличить"
                        )
                    }
                }
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Current streak
            StreakItem(
                value = currentStreak,
                label = "Текущая серия",
                icon = Icons.Rounded.LocalFireDepartment,
                color = habitColor,
                modifier = Modifier.weight(1f)
            )

            // Best streak
            StreakItem(
                value = bestStreak,
                label = "Лучшая серия",
                icon = Icons.Rounded.EmojiEvents,
                color = habitColor,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun StreakItem(
    value: Int,
    label: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(8.dp)
    ) {
        // Animated icon with background
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.1f))
                .padding(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Animated counter
        val animatedValue = remember { Animatable(0f) }
        LaunchedEffect(value) {
            animatedValue.animateTo(
                targetValue = value.toFloat(),
                animationSpec = tween(durationMillis = 1000)
            )
        }

        Text(
            text = animatedValue.value.toInt().toString(),
            style = MaterialTheme.typography.headlineSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun StatisticsSection(
    calendarData: Map<LocalDate, Float>,
    weeklyData: List<Float>,
    monthlyData: List<Float>,
    selectedPeriod: ChartPeriod,
    onPeriodSelected: (ChartPeriod) -> Unit,
    habitColor: Color
) {

    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant.toArgb()
    val habitColorInt = habitColor.toArgb()
    val habitColorAlphaInt = habitColor.copy(alpha = 0.2f).toArgb()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Статистика",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Heat map calendar using MPAndroidChart
            Text(
                text = "Активность",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            // MPAndroidChart Calendar heat map
            AndroidView(
                factory = { context ->
                    LineChart(context).apply {
                        description.isEnabled = false
                        legend.isEnabled = false
                        setTouchEnabled(false)
                        setScaleEnabled(false)
                        setDrawGridBackground(false)

                        // X Axis setup
                        xAxis.position = XAxis.XAxisPosition.BOTTOM
                        xAxis.setDrawGridLines(false)
                        xAxis.textColor = onSurfaceVariantColor
                        xAxis.valueFormatter = object : IndexAxisValueFormatter() {
                            override fun getFormattedValue(value: Float): String {
                                val date = LocalDate.now().minusDays((29 - value.toInt()).toLong())
                                return date.dayOfMonth.toString()
                            }
                        }

                        // Y Axis setup
                        axisLeft.setDrawGridLines(false)
                        axisLeft.setDrawLabels(false)
                        axisLeft.setDrawAxisLine(false)
                        axisRight.isEnabled = false

                        // Appearance
                        setDrawBorders(false)
                        setNoDataText("Нет данных")
                        setNoDataTextColor(onSurfaceVariantColor)
                    }
                },
                update = { chart ->
                    // Convert calendar data to chart entries
                    val entries = mutableListOf<Entry>()
                    for (i in 0..29) {
                        val date = LocalDate.now().minusDays(29 - i.toLong())
                        val value = calendarData[date] ?: 0f
                        entries.add(Entry(i.toFloat(), value))
                    }

                    val dataSet = LineDataSet(entries, "Completion").apply {
                        color = habitColor.toArgb()
                        setDrawCircles(true)
                        setDrawCircleHole(false)
                        circleRadius = 4f
                        circleColors = List(entries.size) { index ->
                            val value = entries[index].y
                            if (value > 0f) habitColor.toArgb() else
                                surfaceVariantColor
                        }
                        lineWidth = 2f
                        mode = LineDataSet.Mode.HORIZONTAL_BEZIER
                        setDrawFilled(true)
                        fillColor = habitColor.copy(alpha = 0.2f).toArgb()
                        setDrawValues(false)
                    }

                    chart.data = LineData(dataSet)
                    chart.invalidate()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Chart period selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Выполнение",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )

                // Tab selector
                TabRow(
                    selectedTabIndex = if (selectedPeriod == ChartPeriod.WEEK) 0 else 1,
                    modifier = Modifier
                        .width(180.dp)
                        .clip(RoundedCornerShape(50)),
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            Modifier.tabIndicatorOffset(
                                tabPositions[if (selectedPeriod == ChartPeriod.WEEK) 0 else 1]
                            )
                        )
                    },
                    divider = {},
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Tab(
                        selected = selectedPeriod == ChartPeriod.WEEK,
                        onClick = { onPeriodSelected(ChartPeriod.WEEK) },
                        text = {
                            Text(
                                text = "Неделя",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    )
                    Tab(
                        selected = selectedPeriod == ChartPeriod.MONTH,
                        onClick = { onPeriodSelected(ChartPeriod.MONTH) },
                        text = {
                            Text(
                                text = "Месяц",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bar chart with MPAndroidChart
            AndroidView(
                factory = { context ->
                    BarChart(context).apply {
                        description.isEnabled = false
                        legend.isEnabled = false
                        setTouchEnabled(false)
                        setScaleEnabled(false)
                        setDrawGridBackground(false)
                        setDrawBorders(false)

                        // X Axis setup
                        xAxis.position = XAxis.XAxisPosition.BOTTOM
                        xAxis.setDrawGridLines(false)
                        xAxis.textColor = onSurfaceVariantColor

                        // Y Axis setup
                        axisLeft.axisMaximum = 1f
                        axisLeft.axisMinimum = 0f
                        axisLeft.setDrawGridLines(false)
                        axisLeft.setDrawLabels(false)
                        axisLeft.setDrawAxisLine(false)
                        axisRight.isEnabled = false

                        // Appearance
                        setNoDataText("Нет данных")
                        setNoDataTextColor(onSurfaceVariantColor )
                    }
                },
                update = { chart ->
                    // Setup data based on selected period
                    val entries = mutableListOf<BarEntry>()
                    val labels = mutableListOf<String>()

                    when (selectedPeriod) {
                        ChartPeriod.WEEK -> {
                            weeklyData.forEachIndexed { index, value ->
                                entries.add(BarEntry(index.toFloat(), value))
                                val day = LocalDate.now().minusDays(6L).plusDays(index.toLong())
                                labels.add(day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()))
                            }
                        }
                        ChartPeriod.MONTH -> {
                            monthlyData.takeLast(30).forEachIndexed { index, value ->
                                entries.add(BarEntry(index.toFloat(), value))
                                labels.add((index + 1).toString())
                            }
                        }
                    }

                    // Setup X Axis labels
                    chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
                    chart.xAxis.labelCount = labels.size

                    // Create dataset
                    val dataSet = BarDataSet(entries, "Completion").apply {
                        color = habitColor.toArgb()
                        setDrawValues(false)
                    }

                    chart.data = BarData(dataSet)
                    chart.invalidate()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            )
        }
    }
}

@Composable
fun HabitDetailsSection(
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
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Детали привычки",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Frequency
            DetailRow(
                icon = Icons.Rounded.Repeat,
                title = "Частота",
                content = frequencyText,
                color = habitColor
            )

            Divider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // Target
            DetailRow(
                icon = Icons.Rounded.Flag,
                title = "Цель",
                content = targetValueText,
                color = habitColor
            )

            // Category
            if (category != null) {
                Divider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                val categoryColor = try {
                    category.color?.let { Color(android.graphics.Color.parseColor(it)) } ?: habitColor
                } catch (e: Exception) {
                    habitColor
                }

                DetailRow(
                    icon = Icons.Rounded.Folder,
                    title = "Категория",
                    content = category.name,
                    color = categoryColor,
                    isChip = true
                )
            }

            // Tags
            if (tags.isNotEmpty()) {
                Divider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                Text(
                    text = "Теги",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tags.forEach { tag ->
                        val tagColor = try {
                            tag.color?.let { Color(android.graphics.Color.parseColor(it)) } ?: habitColor
                        } catch (e: Exception) {
                            habitColor
                        }

                        SuggestionChip(
                            onClick = { },
                            label = { Text(text = tag.name) },
                            icon = {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(tagColor)
                                )
                            },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = tagColor.copy(alpha = 0.1f),
                                labelColor = MaterialTheme.colorScheme.onSurface
                            ),
                            border = null
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(
    icon: ImageVector,
    title: String,
    content: String,
    color: Color,
    isChip: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Icon with background
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.1f))
                .padding(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Content
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            if (isChip) {
                SuggestionChip(
                    onClick = { },
                    label = { Text(text = content) },
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
            } else {
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
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
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Действия",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Share button
            OutlinedButton(
                onClick = onShareClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.Share,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(text = "Поделиться прогрессом")
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Archive/Unarchive button
            OutlinedButton(
                onClick = onArchiveClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (isArchived)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    imageVector = if (isArchived)
                        Icons.Rounded.Unarchive
                    else
                        Icons.Rounded.Archive,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = if (isArchived)
                        "Разархивировать привычку"
                    else
                        "Архивировать привычку"
                )
            }
        }
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
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Error,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Ошибка",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onBackClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = Icons.Rounded.ArrowBack,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(text = "Вернуться назад")
        }
    }
}
