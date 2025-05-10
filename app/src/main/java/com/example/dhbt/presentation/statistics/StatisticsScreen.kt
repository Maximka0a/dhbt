package com.example.dhbt.presentation.statistics

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.dhbt.domain.model.StatisticPeriod
import com.example.dhbt.presentation.components.charts.*
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.PieEntry
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    navController: NavController,
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val viewState by viewModel.viewState.collectAsState()

    val productivityMetrics by viewModel.productivityMetrics.collectAsState()
    val taskMetrics by viewModel.taskMetrics.collectAsState()
    val habitMetrics by viewModel.habitMetrics.collectAsState()
    val pomodoroMetrics by viewModel.pomodoroMetrics.collectAsState()

    val chartData by viewModel.chartData.collectAsState()
    val summaryStats by viewModel.summaryStats.collectAsState()
    val periodsData by viewModel.periodsData.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val isDarkTheme = isSystemInDarkTheme()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Статистика",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    IconButton(onClick = { viewModel.onAction(StatisticsAction.RefreshData) }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Обновить статистику"
                        )
                    }

                    IconButton(
                        onClick = {
                            viewModel.onAction(StatisticsAction.ExportStatistics)
                            scope.launch {
                                snackbarHostState.showSnackbar("Статистика экспортирована")
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Экспортировать статистику"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when {
                viewState.isLoading -> {
                    LoadingState()
                }

                viewState.error != null -> {
                    ErrorState(
                        error = viewState.error!!,
                        onRetry = { viewModel.onAction(StatisticsAction.RefreshData) }
                    )
                }

                else -> {
                    // Main content
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        item {
                            // Секция выбора периода
                            PeriodSelectorSection(
                                selectedPeriod = viewState.selectedPeriod,
                                onPeriodSelected = { viewModel.onAction(StatisticsAction.SetPeriod(it)) },
                                availableMonths = periodsData.availableMonths,
                                availableYears = periodsData.availableYears
                            )

                            // Сводная статистика
                            if (productivityMetrics != null) {
                                SummaryStatsSection(
                                    stats = summaryStats,
                                    productivityMetrics = productivityMetrics!!
                                )
                            } else {
                                EmptyStatePlaceholder(
                                    message = "Нет данных о продуктивности",
                                    icon = Icons.Default.InsertChart
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Секция табов
                            TabsSection(
                                selectedTab = viewState.selectedTab,
                                onTabSelected = { viewModel.onAction(StatisticsAction.SetTab(it)) },
                                isDarkTheme = isDarkTheme
                            )

                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Контент в зависимости от выбранной вкладки
                        item {
                            when (viewState.selectedTab) {
                                StatisticsTab.TASKS -> {
                                    if (taskMetrics != null) {
                                        TasksTabContent(
                                            timelineData = chartData.timelineEntries,
                                            pieCharts = chartData.pieCharts,
                                            xAxisLabels = chartData.xAxisLabels,
                                            taskMetrics = taskMetrics!!,
                                            isDarkTheme = isDarkTheme
                                        )
                                    } else {
                                        EmptyTabContent("задачах")
                                    }
                                }

                                StatisticsTab.HABITS -> {
                                    if (habitMetrics != null) {
                                        HabitsTabContent(
                                            timelineData = chartData.timelineEntries,
                                            pieCharts = chartData.pieCharts,
                                            xAxisLabels = chartData.xAxisLabels,
                                            habitMetrics = habitMetrics!!,
                                            isDarkTheme = isDarkTheme
                                        )
                                    } else {
                                        EmptyTabContent("привычках")
                                    }
                                }

                                StatisticsTab.POMODORO -> {
                                    if (pomodoroMetrics != null) {
                                        PomodoroTabContent(
                                            timelineData = chartData.timelineEntries,
                                            pieCharts = chartData.pieCharts,
                                            xAxisLabels = chartData.xAxisLabels,
                                            pomodoroMetrics = pomodoroMetrics!!,
                                            isDarkTheme = isDarkTheme
                                        )
                                    } else {
                                        EmptyTabContent("Pomodoro-сессиях")
                                    }
                                }
                            }
                        }
                    }

                    // Показываем сообщение пользователю, если оно есть
                    if (viewState.message != null) {
                        MessageOverlay(message = viewState.message!!)
                    }
                }
            }
        }
    }
}

@Composable
fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Загружаем статистику...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun ErrorState(
    error: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(72.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Не удалось загрузить данные",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Попробовать снова")
            }
        }
    }
}

@Composable
fun MessageOverlay(message: String) {
    var visible by remember { mutableStateOf(true) }

    // Корутина для автоматического скрытия сообщения
    LaunchedEffect(message) {
        visible = true
        delay(3000)
        visible = false
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { it },
        exit = fadeOut() + slideOutVertically { it }
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.inverseSurface
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.inverseOnSurface
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.inverseOnSurface
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyStatePlaceholder(
    message: String,
    icon: ImageVector
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun EmptyTabContent(dataTypeName: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.DataExploration,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Нет информации о $dataTypeName",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Начните отслеживать активности, чтобы увидеть статистику",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun PeriodSelectorSection(
    selectedPeriod: StatisticPeriod,
    onPeriodSelected: (StatisticPeriod) -> Unit,
    availableMonths: List<YearMonth>,
    availableYears: List<Int>
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Период анализа",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Используем простые кнопки вместо сегментированной кнопки
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                StatisticPeriod.values().forEach { period ->
                    val isSelected = selectedPeriod == period

                    Button(
                        onClick = { onPeriodSelected(period) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isSelected)
                                MaterialTheme.colorScheme.onPrimary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = period.displayName,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryStatsSection(
    stats: Map<String, String>,
    productivityMetrics: ProductivityMetrics
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Сводная статистика",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Icon(
                    imageVector = Icons.Default.Insights,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Карточки с ключевыми метриками
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    value = productivityMetrics.taskCompletionRate.formatPercentage(),
                    label = "Задачи",
                    icon = Icons.Default.Task,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )

                StatCard(
                    value = productivityMetrics.habitCompletionRate.formatPercentage(),
                    label = "Привычки",
                    icon = Icons.Default.Loop,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )

                StatCard(
                    value = if (productivityMetrics.focusTimeMinutes > 0)
                        "${productivityMetrics.focusTimeMinutes / 60}ч ${productivityMetrics.focusTimeMinutes % 60}м"
                    else "0ч",
                    label = "Фокус",
                    icon = Icons.Default.Timer,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Детальные метрики - показываем не больше 6 строк
            if (stats.isNotEmpty()) {
                stats.entries.take(6).forEach { (key, value) ->
                    StatRow(label = key, value = value)
                }
            }

            if (stats.size > 6) {
                Text(
                    text = "Ещё ${stats.size - 6} метрик",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .clickable { /* Действие для просмотра всех метрик */ }
                )
            }
        }
    }
}

@Composable
fun StatCard(
    value: String,
    label: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.15f),
        contentColor = color
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = color.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun StatRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun TabsSection(
    selectedTab: StatisticsTab,
    onTabSelected: (StatisticsTab) -> Unit,
    isDarkTheme: Boolean
) {
    val tabBgColor = if (isDarkTheme)
        Color(0xFF303030) else
        MaterialTheme.colorScheme.surface

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        color = tabBgColor,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp
    ) {
        ScrollableTabRow(
            selectedTabIndex = selectedTab.ordinal,
            edgePadding = 0.dp,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            divider = { }
        ) {
            StatisticsTab.values().forEach { tab ->
                val isSelected = tab == selectedTab
                val contentColor = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)

                Tab(
                    selected = isSelected,
                    onClick = { onTabSelected(tab) },
                    text = {
                        Text(
                            text = tab.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = when (tab) {
                                StatisticsTab.TASKS -> Icons.Default.Task
                                StatisticsTab.HABITS -> Icons.Default.Loop
                                StatisticsTab.POMODORO -> Icons.Default.Timer
                            },
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = contentColor
                )
            }
        }
    }
}

@Composable
fun TasksTabContent(
    timelineData: List<Entry>,
    pieCharts: Map<String, List<PieEntry>>,
    xAxisLabels: List<String>,
    taskMetrics: TaskMetrics,
    isDarkTheme: Boolean
) {
    val textColor = if (isDarkTheme) Color.White else Color.Black
    val primaryColor = MaterialTheme.colorScheme.primary.toArgb()
    val secondaryColor = MaterialTheme.colorScheme.secondary.toArgb()
    val errorColor = MaterialTheme.colorScheme.error.toArgb()

    // Проверяем наличие данных для графиков
    val hasTimelineData = timelineData.isNotEmpty() && timelineData.any { it.y > 0f }
    val hasPriorityData = pieCharts["priority"]?.isNotEmpty() == true
    val hasStatusData = pieCharts["status"]?.isNotEmpty() == true

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        SectionHeader(
            title = "Анализ задач",
            subtitle = "Завершено ${taskMetrics.completedTasks} из ${taskMetrics.totalTasks} задач",
            icon = Icons.Default.Task
        )

        Spacer(modifier = Modifier.height(16.dp))

        // График распределения задач по дням недели
        ChartCard(
            title = "Распределение по дням недели",
            chartContent = {
                if (hasTimelineData) {
                    BarChartView(
                        data = timelineData.map { BarEntry(it.x, it.y) },
                        barColor = primaryColor,
                        xLabels = xAxisLabels,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .padding(8.dp)
                    )
                } else {
                    EmptyChartPlaceholder("Нет данных о распределении задач")
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Круговые диаграммы приоритетов и статусов - только если есть данные
        if (hasPriorityData || hasStatusData) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    ChartCard(
                        title = "Приоритеты",
                        chartContent = {
                            if (hasPriorityData) {
                                PieChartView(
                                    data = pieCharts["priority"]!!,
                                    colors = listOf(secondaryColor, primaryColor, errorColor),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .padding(8.dp)
                                )
                            } else {
                                EmptyChartPlaceholder("Нет данных")
                            }
                        }
                    )
                }

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    ChartCard(
                        title = "Статус",
                        chartContent = {
                            if (hasStatusData) {
                                PieChartView(
                                    data = pieCharts["status"]!!,
                                    colors = listOf(Color.Green.toArgb(), Color.Gray.toArgb()),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .padding(8.dp)
                                )
                            } else {
                                EmptyChartPlaceholder("Нет данных")
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Карточка со средней статистикой
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Среднее время выполнения",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "${taskMetrics.averageCompletionTimeMinutes / 60} часов ${taskMetrics.averageCompletionTimeMinutes % 60} минут",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        Text(
                            text = "Среднее время от создания до завершения задачи",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HabitsTabContent(
    timelineData: List<Entry>,
    pieCharts: Map<String, List<PieEntry>>,
    xAxisLabels: List<String>,
    habitMetrics: HabitMetrics,
    isDarkTheme: Boolean
) {
    val textColor = if (isDarkTheme) Color.White else Color.Black
    val primaryColor = MaterialTheme.colorScheme.primary.toArgb()
    val secondaryColor = MaterialTheme.colorScheme.secondary.toArgb()
    val tertiaryColor = MaterialTheme.colorScheme.tertiary.toArgb()

    // Проверяем наличие данных для графиков
    val hasTimelineData = timelineData.isNotEmpty() && timelineData.any { it.y > 0f }
    val hasTypeData = pieCharts["type"]?.isNotEmpty() == true
    val hasStatusData = pieCharts["status"]?.isNotEmpty() == true

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        SectionHeader(
            title = "Анализ привычек",
            subtitle = "Активных привычек: ${habitMetrics.activeHabits} из ${habitMetrics.totalHabits}",
            icon = Icons.Default.Loop
        )

        Spacer(modifier = Modifier.height(16.dp))

        // График рейтинга выполнения топ-5 привычек
        ChartCard(
            title = "Рейтинг выполнения привычек",
            subtitle = if (xAxisLabels.isNotEmpty()) "Топ-5 по % выполнения" else null,
            chartContent = {
                if (hasTimelineData) {
                    BarChartView(
                        data = timelineData.map { BarEntry(it.x, it.y) },
                        barColor = primaryColor,
                        xLabels = xAxisLabels,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .padding(8.dp)
                    )
                } else {
                    EmptyChartPlaceholder("Нет данных о выполнении привычек")
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Круговые диаграммы типов привычек и статусов
        if (hasTypeData || hasStatusData) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    ChartCard(
                        title = "Типы привычек",
                        chartContent = {
                            if (hasTypeData) {
                                PieChartView(
                                    data = pieCharts["type"]!!,
                                    colors = listOf(primaryColor, secondaryColor, tertiaryColor),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .padding(8.dp)
                                )
                            } else {
                                EmptyChartPlaceholder("Нет данных")
                            }
                        }
                    )
                }

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    ChartCard(
                        title = "Статус",
                        chartContent = {
                            if (hasStatusData) {
                                PieChartView(
                                    data = pieCharts["status"]!!,
                                    colors = listOf(Color.Green.toArgb(), Color.Gray.toArgb()),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .padding(8.dp)
                                )
                            } else {
                                EmptyChartPlaceholder("Нет данных")
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Карточка со статистикой серий
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Серии привычек",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )

                    Icon(
                        imageVector = Icons.Default.Whatshot,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StreakCard(
                        value = habitMetrics.currentStreak.toString(),
                        label = "Текущая серия",
                        icon = Icons.Default.Today,
                        color = MaterialTheme.colorScheme.primary
                    )

                    StreakCard(
                        value = habitMetrics.longestStreak.toString(),
                        label = "Лучшая серия",
                        icon = Icons.Default.EmojiEvents,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Индикатор прогресса к следующей цели
                val progressToNextGoal = (habitMetrics.currentStreak.toFloat() /
                        (habitMetrics.longestStreak + 1).toFloat()).coerceIn(0f, 1f)

                if (habitMetrics.longestStreak > 0) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Прогресс к новому рекорду",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            )

                            Text(
                                text = "${habitMetrics.currentStreak}/${habitMetrics.longestStreak + 1}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        LinearProgressIndicator(
                            progress = progressToNextGoal,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Общий показатель выполнения
                Text(
                    text = "Общий показатель выполнения: ${String.format("%.1f%%", habitMetrics.completionRate * 100)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun PomodoroTabContent(
    timelineData: List<Entry>,
    pieCharts: Map<String, List<PieEntry>>,
    xAxisLabels: List<String>,
    pomodoroMetrics: PomodoroMetrics,
    isDarkTheme: Boolean
) {
    val textColor = if (isDarkTheme) Color.White else Color.Black
    val primaryColor = MaterialTheme.colorScheme.primary.toArgb()
    val secondaryColor = MaterialTheme.colorScheme.secondary.toArgb()

    // Проверяем наличие данных для графиков
    val hasTimelineData = timelineData.isNotEmpty() && timelineData.any { it.y > 0f }
    val hasSessionData = pieCharts["sessions"]?.isNotEmpty() == true
    val hasAnyData = pomodoroMetrics.totalFocusTimeMinutes > 0 ||
            pomodoroMetrics.completedSessions > 0 ||
            hasTimelineData

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        SectionHeader(
            title = "Анализ техники Pomodoro",
            subtitle = "Общее время фокуса: ${pomodoroMetrics.totalFocusTimeMinutes / 60} ч ${pomodoroMetrics.totalFocusTimeMinutes % 60} мин",
            icon = Icons.Default.Timer
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (!hasAnyData) {
            // Показываем плашку с призывом начать использовать Pomodoro
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "У вас пока нет данных о Pomodoro сессиях",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Начните использовать технику Pomodoro для управления временем и повышения продуктивности",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { /* Действие для перехода к Pomodoro */ },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Начать сессию")
                    }
                }
            }

            return
        }

        // График времени фокуса по дням
        ChartCard(
            title = "Время фокуса по дням",
            subtitle = "В минутах",
            chartContent = {
                if (hasTimelineData) {
                    LineChartView(
                        data = timelineData,
                        lineColor = primaryColor,
                        fillColor = adjustAlpha(primaryColor, 0.3f),
                        xLabels = xAxisLabels,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .padding(8.dp)
                    )
                } else {
                    EmptyChartPlaceholder("Нет данных о времени фокуса")
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Круговая диаграмма завершенных сессий
        ChartCard(
            title = "Статистика сессий",
            chartContent = {
                if (hasSessionData) {
                    PieChartView(
                        data = pieCharts["sessions"]!!,
                        centerText = "Сессии",
                        colors = listOf(Color(0xFF4CAF50).toArgb(), Color(0xFFE0E0E0).toArgb()),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .padding(8.dp)
                    )
                } else {
                    EmptyChartPlaceholder("Нет данных о сессиях")
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Карточки со статистикой
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Статистика по сессиям",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )

                    Icon(
                        imageVector = Icons.Default.Assessment,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    InfoItem(
                        icon = Icons.Default.Check,
                        label = "Завершено",
                        value = pomodoroMetrics.completedSessions.toString(),
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )

                    InfoItem(
                        icon = Icons.Default.Close,
                        label = "Прервано",
                        value = pomodoroMetrics.incompleteSessions.toString(),
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )

                    InfoItem(
                        icon = Icons.Default.Timelapse,
                        label = "В день",
                        value = "${pomodoroMetrics.averageDailyMinutes} мин",
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Если есть достаточно данных для распределения по задачам
        if (pomodoroMetrics.taskDistribution.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Топ задач по времени",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        Icon(
                            imageVector = Icons.Default.FormatListNumbered,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    pomodoroMetrics.taskDistribution
                        .entries
                        .sortedByDescending { it.value }
                        .take(3)
                        .forEachIndexed { index, (taskId, minutes) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f),
                                            shape = CircleShape
                                        )
                                        .wrapContentSize(Alignment.Center)
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = taskId?.let {
                                            if (it.length > 8) "Задача #${it.takeLast(4)}"
                                            else "Задача #$it"
                                        } ?: "Без задачи",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )

                                    Text(
                                        text = formatMinutesToHoursAndMinutes(minutes),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                    )
                                }

                                if (index == 0) {
                                    Icon(
                                        imageVector = Icons.Default.EmojiEvents,
                                        contentDescription = null,
                                        tint = Color(0xFFFFD700),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            if (index < 2) {
                                Divider(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f)
                                )
                            }
                        }
                }
            }
        }
    }
}
@Composable
fun ActivityCalendarSection(
    selectedTab: StatisticsTab,
    habitMetrics: HabitMetrics?,
    isDarkTheme: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        SectionHeader(
            title = "Карта активности",
            subtitle = "За последние 2 месяца",
            icon = Icons.Default.CalendarMonth
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (habitMetrics == null || habitMetrics.habitData.isEmpty()) {
            EmptyStatePlaceholder(
                message = "Нет данных об активности",
                icon = Icons.Default.CalendarToday
            )
            return
        }

        val startDate = LocalDate.now().minusMonths(2).withDayOfMonth(1)
        val endDate = LocalDate.now()

        // Создаем реальные данные на основе отслеживания привычек
        val calendarData = mutableMapOf<LocalDate, Float>()

        // Агрегируем данные по датам из всех привычек
        habitMetrics.habitData.forEach { habitData ->
            habitData.tracking.forEach { tracking ->
                val date = LocalDate.ofEpochDay(tracking.date / (24 * 60 * 60 * 1000))
                // Если дата в нашем диапазоне
                if (!date.isBefore(startDate) && !date.isAfter(endDate)) {
                    val currentValue = calendarData[date] ?: 0f
                    val addedValue = if (tracking.isCompleted) 1f else 0f
                    calendarData[date] = currentValue + addedValue
                }
            }
        }

        // Аналог GitHub contributions calendar
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                if (calendarData.isNotEmpty()) {
                    CalendarHeatmap(
                        data = calendarData,
                        startDate = startDate,
                        endDate = endDate,
                        primaryColor = when(selectedTab) {
                            StatisticsTab.TASKS -> MaterialTheme.colorScheme.primary
                            StatisticsTab.HABITS -> MaterialTheme.colorScheme.secondary
                            StatisticsTab.POMODORO -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Меньше активности",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            repeat(5) { level ->
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .padding(horizontal = 2.dp)
                                        .background(
                                            color = when(selectedTab) {
                                                StatisticsTab.TASKS -> MaterialTheme.colorScheme.primary
                                                StatisticsTab.HABITS -> MaterialTheme.colorScheme.secondary
                                                StatisticsTab.POMODORO -> MaterialTheme.colorScheme.tertiary
                                                else -> MaterialTheme.colorScheme.primary
                                            }.copy(alpha = 0.1f + level * 0.2f),
                                            shape = RoundedCornerShape(2.dp)
                                        )
                                )
                            }
                        }

                        Text(
                            text = "Больше активности",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Нет данных о выполнении в этом периоде",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    action: (@Composable () -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            if (action != null) {
                action()
            }
        }

        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun ChartCard(
    title: String,
    subtitle: String? = null,
    chartContent: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.BarChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            chartContent()
        }
    }
}

@Composable
fun StreakCard(
    value: String,
    label: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(28.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = color,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun InfoItem(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color
        )

        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = color.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun EmptyChartPlaceholder(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.ShowChart,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun CalendarHeatmap(
    data: Map<LocalDate, Float>,
    startDate: LocalDate,
    endDate: LocalDate,
    primaryColor: Color,
    modifier: Modifier = Modifier
) {
    val maxValue = data.values.maxOrNull() ?: 1f
    val weeks = mutableListOf<List<LocalDate?>>()

    // Вычисляем первый день недели для начальной даты
    var current = startDate.minusDays(startDate.dayOfWeek.value.toLong() - 1)

    // Создаем недели
    while (!current.isAfter(endDate)) {
        val week = (0..6).map { dayOffset ->
            val day = current.plusDays(dayOffset.toLong())
            if (day.isBefore(startDate) || day.isAfter(endDate)) null else day
        }
        weeks.add(week)
        current = current.plusWeeks(1)
    }

    // Месяцы для подписей
    val months = weeks.flatten().filterNotNull().distinctBy { it.month }.sortedBy { it }

    Column(modifier = modifier) {
        // Подписи месяцев
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp)
        ) {
            // Отступ для выравнивания с ячейками дней
            Spacer(modifier = Modifier.width(20.dp))

            months.forEach { date ->
                Text(
                    text = date.month.getDisplayName(
                        java.time.format.TextStyle.SHORT,
                        Locale.getDefault()
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                Spacer(modifier = Modifier.weight(1f))
            }
        }

        // Сетка дней
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Подписи дней недели
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(end = 4.dp, top = 16.dp)
            ) {
                val dayNames = listOf("Пн", "Ср", "Пт")
                dayNames.forEach { dayName ->
                    Box(
                        modifier = Modifier.size(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = dayName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            fontSize = 8.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Недели
            weeks.forEach { week ->
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    week.forEach { day ->
                        val activity = if (day != null) (data[day] ?: 0f) else 0f
                        val alpha = if (day != null) {
                            if (activity <= 0f) 0.1f else (0.3f + (activity / maxValue) * 0.7f).coerceIn(0.1f, 1.0f)
                        } else 0f

                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(
                                    color = if (day != null) primaryColor.copy(alpha = alpha)
                                    else Color.Transparent,
                                    shape = RoundedCornerShape(2.dp)
                                )
                                .border(
                                    width = if (day != null && day == LocalDate.now()) 1.dp else 0.dp,
                                    color = if (day == LocalDate.now()) primaryColor else Color.Transparent,
                                    shape = RoundedCornerShape(2.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (day != null && activity > 0) {
                                Text(
                                    text = activity.toInt().toString(),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 7.sp,
                                    color = if (alpha > 0.6f)
                                        MaterialTheme.colorScheme.surface
                                    else
                                        primaryColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Вспомогательные функции
fun formatMinutesToHoursAndMinutes(minutes: Int): String {
    val hours = minutes / 60
    val mins = minutes % 60

    return if (hours > 0) {
        "$hours ч ${if (mins > 0) "$mins мин" else ""}"
    } else {
        "$mins мин"
    }
}

// Вспомогательная функция для настройки прозрачности цвета
fun adjustAlpha(color: Int, alpha: Float): Int {
    val red = android.graphics.Color.red(color)
    val green = android.graphics.Color.green(color)
    val blue = android.graphics.Color.blue(color)
    return android.graphics.Color.argb((alpha * 255).toInt(), red, green, blue)
}

fun Double.formatPercentage(): String {
    if (this.isNaN() || this.isInfinite()) return "0.0%"
    return String.format("%.1f%%", this)
}
