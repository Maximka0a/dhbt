package com.example.dhbt.presentation.statistics

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.example.dhbt.domain.model.StatisticPeriod
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.dhbt.presentation.components.charts.*
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.PieEntry
import kotlinx.coroutines.launch
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    navController: NavController,
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedPeriod by viewModel.selectedPeriod.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()

    val productivityMetrics by viewModel.productivityMetrics.collectAsState()
    val taskMetrics by viewModel.taskMetrics.collectAsState()
    val habitMetrics by viewModel.habitMetrics.collectAsState()
    val pomodoroMetrics by viewModel.pomodoroMetrics.collectAsState()

    val timelineChartData by viewModel.timelineChartData.collectAsState()
    val pieChartData by viewModel.pieChartData.collectAsState()
    val summaryStats by viewModel.summaryStats.collectAsState()

    val availableMonths by viewModel.availableMonths.collectAsState()
    val availableYears by viewModel.availableYears.collectAsState()

    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    val isDarkTheme = isSystemInDarkTheme()

    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Статистика") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    IconButton(onClick = { viewModel.onAction(StatisticsAction.RefreshData) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Обновить")
                    }

                    IconButton(onClick = { viewModel.onAction(StatisticsAction.ExportStatistics) }) {
                        Icon(Icons.Default.Save, contentDescription = "Экспорт")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.error != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Ошибка загрузки данных",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = uiState.error!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(onClick = { viewModel.onAction(StatisticsAction.RefreshData) }) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Повторить")
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                ) {
                    // Секция выбора периода
                    PeriodSelectorSection(
                        selectedPeriod = selectedPeriod,
                        onPeriodSelected = { viewModel.onAction(StatisticsAction.SetPeriod(it)) },
                        availableMonths = availableMonths,
                        availableYears = availableYears
                    )

                    // Сводная статистика
                    SummaryStatsSection(
                        stats = summaryStats,
                        productivityMetrics = productivityMetrics
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Секция табов
                    TabsSection(
                        selectedTab = selectedTab,
                        onTabSelected = { viewModel.onAction(StatisticsAction.SetTab(it)) },
                        isDarkTheme = isDarkTheme
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Секция с графиками
                    when (selectedTab) {
                        StatisticsTab.PRODUCTIVITY -> ProductivityTabContent(
                            timelineData = timelineChartData,
                            pieData = pieChartData["productivity"] ?: emptyList(),
                            selectedPeriod = selectedPeriod,
                            productivityMetrics = productivityMetrics,
                            isDarkTheme = isDarkTheme
                        )

                        StatisticsTab.TASKS -> TasksTabContent(
                            timelineData = timelineChartData,
                            priorityPieData = pieChartData["priority"] ?: emptyList(),
                            statusPieData = pieChartData["status"] ?: emptyList(),
                            taskMetrics = taskMetrics,
                            isDarkTheme = isDarkTheme
                        )

                        StatisticsTab.HABITS -> HabitsTabContent(
                            timelineData = timelineChartData,
                            typePieData = pieChartData["type"] ?: emptyList(),
                            statusPieData = pieChartData["status"] ?: emptyList(),
                            habitMetrics = habitMetrics,
                            isDarkTheme = isDarkTheme
                        )

                        StatisticsTab.POMODORO -> PomodoroTabContent(
                            timelineData = timelineChartData,
                            sessionsPieData = pieChartData["sessions"] ?: emptyList(),
                            pomodoroMetrics = pomodoroMetrics,
                            isDarkTheme = isDarkTheme
                        )
                    }

                    // Календарь активности
                    if (selectedTab != StatisticsTab.PRODUCTIVITY) {
                        ActivityCalendarSection(
                            selectedTab = selectedTab,
                            habitMetrics = habitMetrics,
                            isDarkTheme = isDarkTheme
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun PeriodSelectorSection(
    selectedPeriod: StatisticPeriod,
    onPeriodSelected: (StatisticPeriod) -> Unit,
    availableMonths: List<org.threeten.bp.YearMonth>,
    availableYears: List<Int>
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Период анализа",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatisticPeriod.values().forEach { period ->
                    FilterChip(
                        selected = selectedPeriod == period,
                        onClick = { onPeriodSelected(period) },
                        label = { Text(period.displayName) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun SummaryStatsSection(
    stats: Map<String, String>,
    productivityMetrics: ProductivityMetrics?
) {
    if (productivityMetrics == null) return

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Сводная статистика",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Карточки с ключевыми метриками
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatCard(
                    value = productivityMetrics.taskCompletionRate.formatPercentage(),
                    label = "Задачи",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )

                StatCard(
                    value = productivityMetrics.habitCompletionRate.formatPercentage(),
                    label = "Привычки",
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                StatCard(
                    value = if (productivityMetrics.focusTimeMinutes > 0)
                        "${productivityMetrics.focusTimeMinutes / 60}ч"
                    else "0ч",
                    label = "Фокус",
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Детальные метрики
            stats.entries.take(6).forEach { (key, value) ->
                StatRow(label = key, value = value)
            }
        }
    }
}

@Composable
fun StatCard(
    value: String,
    label: String,
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
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = color.copy(alpha = 0.8f)
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
    val tabTextColor = if (isDarkTheme) Color.White else Color.Black
    val tabBgColor = if (isDarkTheme) Color(0xFF333333) else Color(0xFFF0F0F0)
    val selectedBgColor = if (isDarkTheme) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = tabBgColor,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(4.dp)
                .fillMaxWidth()
        ) {
            StatisticsTab.values().forEach { tab ->
                val isSelected = selectedTab == tab
                TabButton(
                    text = tab.title,
                    isSelected = isSelected,
                    onSelected = { onTabSelected(tab) },
                    color = if (isSelected) selectedBgColor else tabBgColor,
                    textColor = if (isSelected) Color.White else tabTextColor.copy(alpha = 0.7f),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun TabButton(
    text: String,
    isSelected: Boolean,
    onSelected: () -> Unit,
    color: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(color)
            .clickable { onSelected() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = textColor
        )
    }
}

@Composable
fun ProductivityTabContent(
    timelineData: List<Entry>,
    pieData: List<PieEntry>,
    selectedPeriod: StatisticPeriod,
    productivityMetrics: ProductivityMetrics?,
    isDarkTheme: Boolean
) {
    if (productivityMetrics == null) return

    val textColor = if (isDarkTheme) Color.White else Color.Black
    val primaryColor = MaterialTheme.colorScheme.primary.toArgb()
    val secondaryColor = MaterialTheme.colorScheme.secondary.toArgb()
    val tertiaryColor = MaterialTheme.colorScheme.tertiary.toArgb()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // Заголовок
        Text(
            text = "Общая продуктивность",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "За последний ${selectedPeriod.displayName.lowercase()}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Круговая диаграмма с распределением
        Text(
            text = "Распределение активностей",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        PieChartView(
            data = pieData,
            centerText = "Активностей",
            colors = listOf(primaryColor, secondaryColor, tertiaryColor),
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // График изменения продуктивности
        Text(
            text = "Динамика продуктивности",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        val xLabels = getLabelsForPeriod(selectedPeriod)

        LineChartView(
            data = timelineData,
            lineColor = primaryColor,
            fillColor = primaryColor.adjustAlpha(0.3f),
            xLabels = xLabels,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Карточка продуктивных серий
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Серии продуктивности",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StreakCard(
                        value = productivityMetrics.productiveStreak.toString(),
                        label = "Лучшая серия",
                        icon = Icons.Default.Whatshot,
                        color = MaterialTheme.colorScheme.primary
                    )

                    StreakCard(
                        value = productivityMetrics.daysAnalyzed.toString(),
                        label = "Дней анализа",
                        icon = Icons.Default.CalendarToday,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun TasksTabContent(
    timelineData: List<Entry>,
    priorityPieData: List<PieEntry>,
    statusPieData: List<PieEntry>,
    taskMetrics: TaskMetrics?,
    isDarkTheme: Boolean
) {
    if (taskMetrics == null) return

    val textColor = if (isDarkTheme) Color.White else Color.Black
    val primaryColor = MaterialTheme.colorScheme.primary.toArgb()
    val secondaryColor = MaterialTheme.colorScheme.secondary.toArgb()
    val errorColor = MaterialTheme.colorScheme.error.toArgb()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // Заголовок
        Text(
            text = "Анализ задач",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Завершено ${taskMetrics.completedTasks} из ${taskMetrics.totalTasks} задач",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // График распределения задач по дням недели
        Text(
            text = "Распределение по дням недели",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        val dayLabels = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

        BarChartView(
            data = timelineData.map { BarEntry(it.x, it.y) },
            barColor = primaryColor,
            xLabels = dayLabels,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Круговые диаграммы приоритетов и статусов
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Приоритеты",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                PieChartView(
                    data = priorityPieData,
                    colors = listOf(secondaryColor, primaryColor, errorColor),
                    modifier = Modifier
                        .height(160.dp)
                        .fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Статус",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                PieChartView(
                    data = statusPieData,
                    colors = listOf(Color.Green.toArgb(), Color.Gray.toArgb()),
                    modifier = Modifier
                        .height(160.dp)
                        .fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Карточка со средней статистикой
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
                Text(
                    text = "Среднее время выполнения",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "${taskMetrics.averageCompletionTimeMinutes / 60} часов ${taskMetrics.averageCompletionTimeMinutes % 60} минут",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun HabitsTabContent(
    timelineData: List<Entry>,
    typePieData: List<PieEntry>,
    statusPieData: List<PieEntry>,
    habitMetrics: HabitMetrics?,
    isDarkTheme: Boolean
) {
    if (habitMetrics == null) return

    val textColor = if (isDarkTheme) Color.White else Color.Black
    val primaryColor = MaterialTheme.colorScheme.primary.toArgb()
    val secondaryColor = MaterialTheme.colorScheme.secondary.toArgb()
    val tertiaryColor = MaterialTheme.colorScheme.tertiary.toArgb()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // Заголовок
        Text(
            text = "Анализ привычек",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Активных привычек: ${habitMetrics.activeHabits} из ${habitMetrics.totalHabits}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // График рейтинга выполнения топ-5 привычек
        Text(
            text = "Рейтинг выполнения привычек (Топ-5)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        val habitNames = habitMetrics.habitData
            .sortedByDescending { it.completionRate }
            .take(5)
            .map { it.habit.title.take(6) + if (it.habit.title.length > 6) "..." else "" }

        BarChartView(
            data = timelineData.map { BarEntry(it.x, it.y) },
            barColor = primaryColor,
            xLabels = habitNames,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Круговые диаграммы типов привычек и статусов
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Типы привычек",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                PieChartView(
                    data = typePieData,
                    colors = listOf(primaryColor, secondaryColor, tertiaryColor),
                    modifier = Modifier
                        .height(160.dp)
                        .fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Статус",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                PieChartView(
                    data = statusPieData,
                    colors = listOf(Color.Green.toArgb(), Color.Gray.toArgb()),
                    modifier = Modifier
                        .height(160.dp)
                        .fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Карточка со статистикой серий
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Серии привычек",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StreakCard(
                        value = habitMetrics.currentStreak.toString(),
                        label = "Текущая серия",
                        icon = Icons.Default.Whatshot,
                        color = MaterialTheme.colorScheme.primary
                    )

                    StreakCard(
                        value = habitMetrics.longestStreak.toString(),
                        label = "Лучшая серия",
                        icon = Icons.Default.EmojiEvents,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Общий показатель выполнения: ${String.format("%.1f%%", habitMetrics.completionRate * 100)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun PomodoroTabContent(
    timelineData: List<Entry>,
    sessionsPieData: List<PieEntry>,
    pomodoroMetrics: PomodoroMetrics?,
    isDarkTheme: Boolean
) {
    if (pomodoroMetrics == null) return

    val textColor = if (isDarkTheme) Color.White else Color.Black
    val primaryColor = MaterialTheme.colorScheme.primary.toArgb()
    val secondaryColor = MaterialTheme.colorScheme.secondary.toArgb()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // Заголовок
        Text(
            text = "Анализ техники Pomodoro",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Общее время фокуса: ${pomodoroMetrics.totalFocusTimeMinutes / 60} часов ${pomodoroMetrics.totalFocusTimeMinutes % 60} минут",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // График времени фокуса по дням
        Text(
            text = "Время фокуса по дням (минуты)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        val dayLabels = getDayLabelsForWeek()

        LineChartView(
            data = timelineData,
            lineColor = primaryColor,
            fillColor = primaryColor.adjustAlpha(0.3f),
            xLabels = dayLabels,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Круговая диаграмма завершенных сессий
        Text(
            text = "Статистика сессий",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        PieChartView(
            data = sessionsPieData,
            centerText = "Сессии",
            colors = listOf(Color.Green.toArgb(), Color.Gray.toArgb()),
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Карточки со статистикой
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Статистика по сессиям",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        InfoItem(
                            icon = Icons.Default.Check,
                            label = "Завершено",
                            value = pomodoroMetrics.completedSessions.toString()
                        )

                        InfoItem(
                            icon = Icons.Default.Close,
                            label = "Прервано",
                            value = pomodoroMetrics.incompleteSessions.toString()
                        )

                        InfoItem(
                            icon = Icons.Default.Timelapse,
                            label = "В среднем/день",
                            value = "${pomodoroMetrics.averageDailyMinutes} мин"
                        )
                    }
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
                    Text(
                        text = "Топ задач по времени",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    pomodoroMetrics.taskDistribution
                        .entries
                        .sortedByDescending { it.value }
                        .take(3)
                        .forEach { (taskId, minutes) ->
                            Text(
                                text = "Задача #${taskId?.takeLast(4) ?: "Без задачи"}: ${minutes} мин",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
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
    if (habitMetrics == null) return

    val startDate = LocalDate.now().minusMonths(2).withDayOfMonth(1)  // Только 2 месяца вместо 5
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // Заголовок
        Text(
            text = "Карта активности",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Аналог GitHub contributions calendar
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                if (calendarData.isNotEmpty()) {
                    CalendarHeatmap(
                        data = calendarData,
                        startDate = startDate,
                        endDate = endDate,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Легенда: светлее - меньше активности, темнее - больше активности",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Нет данных для отображения",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun StreakCard(
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
                style = MaterialTheme.typography.headlineMedium,
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
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onTertiaryContainer
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onTertiaryContainer
        )

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
        )
    }
}

// Вспомогательные функции
fun getLabelsForPeriod(period: StatisticPeriod): List<String> {
    val formatter = DateTimeFormatter.ofPattern("dd.MM")
    val today = LocalDate.now()

    return when (period) {
        StatisticPeriod.DAY -> listOf("Сегодня")
        StatisticPeriod.WEEK -> (0..6).map {
            today.minusDays((6 - it).toLong()).format(DateTimeFormatter.ofPattern("EE"))
        }
        StatisticPeriod.MONTH -> (0..29 step 5).map {
            today.minusDays((29 - it).toLong()).format(formatter)
        }
        StatisticPeriod.YEAR -> (0..11).map {
            today.minusMonths((11 - it).toLong()).format(DateTimeFormatter.ofPattern("MMM"))
        }
    }
}

fun getDayLabelsForWeek(): List<String> {
    val formatter = DateTimeFormatter.ofPattern("EE", Locale("ru"))
    val today = LocalDate.now()

    return (0..6).map {
        today.minusDays((6 - it).toLong()).format(formatter)
    }
}

// Вспомогательная функция для настройки прозрачности цвета
fun Int.adjustAlpha(alpha: Float): Int {
    val red = android.graphics.Color.red(this)
    val green = android.graphics.Color.green(this)
    val blue = android.graphics.Color.blue(this)
    return android.graphics.Color.argb((alpha * 255).toInt(), red, green, blue)
}

fun Double.formatPercentage(): String {
    if (this.isNaN()) return "0.0%"
    return String.format("%.1f%%", this)
}