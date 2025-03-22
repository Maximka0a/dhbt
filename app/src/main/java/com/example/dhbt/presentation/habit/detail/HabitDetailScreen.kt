package com.example.dhbt.presentation.habit.detail

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.dhbt.domain.model.Category
import com.example.dhbt.domain.model.Habit
import com.example.dhbt.domain.model.HabitStatus
import com.example.dhbt.domain.model.HabitType
import com.example.dhbt.domain.model.Tag
import com.example.dhbt.presentation.components.ConfirmDeleteDialog
import com.example.dhbt.presentation.habit.components.CalendarHeatMap
import com.example.dhbt.presentation.habit.components.StatsBarGraph
import com.example.dhbt.presentation.navigation.HabitEdit
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
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
    val showMenu by viewModel.showMenu.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Получение цвета привычки
    val habitColor = calculateHabitColor(habit)

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
            TopAppBar(
                title = {
                    Text(
                        text = habit?.title ?: "Привычка",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.onAction(HabitDetailAction.ShowMenu(true)) }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Меню")
                    }

                    // Выпадающее меню
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { viewModel.onAction(HabitDetailAction.ShowMenu(false)) }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Редактировать") },
                            onClick = {
                                viewModel.onAction(HabitDetailAction.ShowMenu(false))
                                navController.navigate(HabitEdit(habit?.id ?: ""))
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Edit, contentDescription = null)
                            }
                        )

                        val isArchived = habit?.status == HabitStatus.ARCHIVED
                        DropdownMenuItem(
                            text = { Text(if (isArchived) "Восстановить из архива" else "Архивировать") },
                            onClick = {
                                viewModel.onAction(HabitDetailAction.ShowMenu(false))
                                viewModel.onAction(HabitDetailAction.ArchiveHabit)
                            },
                            leadingIcon = {
                                Icon(
                                    if (isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                                    contentDescription = null
                                )
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Поделиться") },
                            onClick = {
                                viewModel.onAction(HabitDetailAction.ShowMenu(false))
                                viewModel.onAction(HabitDetailAction.ShareHabit)
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Share, contentDescription = null)
                            }
                        )

                        Divider()

                        DropdownMenuItem(
                            text = { Text("Удалить", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                viewModel.onAction(HabitDetailAction.ShowMenu(false))
                                viewModel.onAction(HabitDetailAction.ShowDeleteDialog(true))
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    // Исправление: безопасная обработка errorMessage
                    uiState.error?.let { errorMessage ->
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { navController.popBackStack() }) {
                        Text("Вернуться назад")
                    }
                }
            } else {
                // Основной контент экрана
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(bottom = 80.dp) // Учитываем размер FAB
                ) {
                    // Секция общего вида
                    HabitGeneralSection(
                        habitColor = habitColor,
                        emoji = habit?.iconEmoji ?: "📝",
                        description = habit?.description,
                        progress = todayProgress,
                        isCompleted = todayIsCompleted,
                        progressText = viewModel.getCurrentProgressText(),
                        habitType = habit?.type ?: HabitType.BINARY,
                        onIncrementClick = { viewModel.onAction(HabitDetailAction.IncrementProgress) },
                        onDecrementClick = { viewModel.onAction(HabitDetailAction.DecrementProgress) }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Секция статистики
                    HabitStatsSection(
                        currentStreak = habit?.currentStreak ?: 0,
                        bestStreak = habit?.bestStreak ?: 0,
                        calendarData = calendarData,
                        weeklyData = weeklyCompletion,
                        monthlyData = monthlyCompletion,
                        selectedPeriod = selectedChartPeriod,
                        onPeriodSelected = { viewModel.onAction(HabitDetailAction.SetChartPeriod(it)) },
                        habitColor = habitColor
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Секция деталей настроек
                    HabitDetailsSection(
                        frequencyText = viewModel.getFrequencyText(),
                        targetValueText = viewModel.getTargetValueText(),
                        category = category,
                        tags = tags,
                        habitColor = habitColor
                    )
                }

                // Нижняя кнопка действия
                FloatingActionButton(
                    onClick = { viewModel.onAction(HabitDetailAction.ToggleCompletion) },
                    containerColor = habitColor,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    Icon(
                        imageVector = if (todayIsCompleted) Icons.Default.CheckCircle else Icons.Default.CheckCircleOutline,
                        contentDescription = "Отметить выполнение"
                    )
                }
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

/**
 * Функция для вычисления цвета привычки на основе её настроек
 */

@Composable
private fun calculateHabitColor(habit: Habit?): Color {
    return remember(habit) {
        try {
            habit?.color?.let { Color(android.graphics.Color.parseColor(it)) }
        } catch (e: Exception) {
            Color.Red
        } ?: Color.Blue
    }
}

@Composable
fun HabitGeneralSection(
    habitColor: Color,
    emoji: String,
    description: String?,
    progress: Float,
    isCompleted: Boolean,
    progressText: String,
    habitType: HabitType,
    onIncrementClick: () -> Unit,
    onDecrementClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Верхняя часть с эмоджи и описанием
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Эмоджи в цветном круге
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(habitColor.copy(alpha = 0.2f))
                        .border(2.dp, habitColor, CircleShape)
                ) {
                    Text(
                        text = emoji,
                        fontSize = 28.sp
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Описание
                if (!description.isNullOrBlank()) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Text(
                        text = "Нет описания",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Прогресс
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Круговой индикатор прогресса
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(120.dp)
                ) {
                    // Фоновый круг
                    CircularProgressIndicator(
                        progress = 1f,
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        strokeWidth = 12.dp,
                        strokeCap = StrokeCap.Round
                    )

                    // Прогресс
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxSize(),
                        color = habitColor,
                        strokeWidth = 12.dp,
                        strokeCap = StrokeCap.Round
                    )

                    // Текст в центре
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (habitType == HabitType.BINARY) {
                            Icon(
                                imageVector = if (isCompleted) Icons.Default.Check else Icons.Default.Close,
                                contentDescription = null,
                                tint = if (isCompleted) habitColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(32.dp)
                            )
                        } else {
                            Text(
                                text = "${(progress * 100).toInt()}%",
                                style = MaterialTheme.typography.headlineMedium,
                                color = habitColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Текст прогресса
                Text(
                    text = progressText,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Кнопки для изменения прогресса (+/-)
                if (habitType != HabitType.BINARY) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = onDecrementClick,
                            border = BorderStroke(1.dp, habitColor),
                            contentPadding = PaddingValues(horizontal = 24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Уменьшить"
                            )
                        }

                        OutlinedButton(
                            onClick = onIncrementClick,
                            border = BorderStroke(1.dp, habitColor),
                            contentPadding = PaddingValues(horizontal = 24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Увеличить"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HabitStatsSection(
    currentStreak: Int,
    bestStreak: Int,
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Заголовок секции
            Text(
                text = "Статистика",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Карточки с текущей серией и рекордной серией
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Текущая серия
                StreakCard(
                    label = "Текущая серия",
                    value = currentStreak.toString(),
                    icon = Icons.Default.Whatshot,
                    color = habitColor,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(16.dp))

                // Рекордная серия
                StreakCard(
                    label = "Рекордная серия",
                    value = bestStreak.toString(),
                    icon = Icons.Default.EmojiEvents,
                    color = habitColor,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Календарь активности (тепловая карта)
            Text(
                text = "История отметок",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

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

            // График прогресса
            Text(
                text = "Прогресс по дням",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Переключатели периодов
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Переключатель для "Неделя"
                FilterChip(
                    selected = selectedPeriod == ChartPeriod.WEEK,
                    onClick = { onPeriodSelected(ChartPeriod.WEEK) },
                    label = { Text("Неделя") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = habitColor.copy(alpha = 0.2f),
                        selectedLabelColor = habitColor
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Переключатель для "Месяц"
                FilterChip(
                    selected = selectedPeriod == ChartPeriod.MONTH,
                    onClick = { onPeriodSelected(ChartPeriod.MONTH) },
                    label = { Text("Месяц") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = habitColor.copy(alpha = 0.2f),
                        selectedLabelColor = habitColor
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // График в зависимости от выбранного периода
            AnimatedVisibility(visible = selectedPeriod == ChartPeriod.WEEK) {
                StatsBarGraph(
                    data = weeklyData,
                    maxValue = 1f,
                    barColor = habitColor,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    labels = getWeekLabels(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .padding(vertical = 8.dp)
                )
            }

            AnimatedVisibility(visible = selectedPeriod == ChartPeriod.MONTH) {
                StatsBarGraph(
                    data = monthlyData,
                    maxValue = 1f,
                    barColor = habitColor,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    labels = getMonthLabels(30),
                    showAllLabels = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .padding(vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun StreakCard(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Заголовок секции
            Text(
                text = "Настройки",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Частота
            DetailItem(
                icon = Icons.Default.Repeat,
                label = "Частота",
                value = frequencyText,
                color = habitColor
            )

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            // Целевое значение
            DetailItem(
                icon = Icons.Default.Flag,
                label = "Цель",
                value = targetValueText,
                color = habitColor
            )

            // Категория, если есть
            if (category != null) {
                Divider(modifier = Modifier.padding(vertical = 12.dp))

                DetailItem(
                    icon = Icons.Default.Folder,
                    label = "Категория",
                    value = category.name,
                    color = try {
                        category.color?.let { Color(android.graphics.Color.parseColor(it)) } ?: habitColor
                    } catch (e: Exception) {
                        habitColor
                    }
                )
            }

            // Теги, если есть
            if (tags.isNotEmpty()) {
                Divider(modifier = Modifier.padding(vertical = 12.dp))

                Column {
                    Text(
                        text = "Теги",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Отображаем теги в виде чипов
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        tags.forEach { tag ->
                            val tagColor = try {
                                tag.color?.let { Color(android.graphics.Color.parseColor(it)) } ?: habitColor
                            } catch (e: Exception) {
                                habitColor
                            }

                            TagChip(
                                text = tag.name,
                                color = tagColor,
                                modifier = Modifier.padding(end = 8.dp)
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
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier
                .size(24.dp)
                .padding(end = 4.dp)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun TagChip(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// Вспомогательные функции для получения подписей для графика
@Composable
fun getWeekLabels(): List<String> {
    val formatter = DateTimeFormatter.ofPattern("EE", Locale.getDefault())
    return List(7) { i ->
        LocalDate.now().minusDays(6 - i.toLong()).format(formatter)
    }
}

@Composable
fun getMonthLabels(days: Int): List<String> {
    val formatter = DateTimeFormatter.ofPattern("d", Locale.getDefault())
    return List(days) { i ->
        LocalDate.now().minusDays((days - 1 - i).toLong()).format(formatter)
    }
}