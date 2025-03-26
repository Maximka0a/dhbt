package com.example.dhbt.presentation.task.list

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.dhbt.R
import com.example.dhbt.domain.model.Category
import com.example.dhbt.domain.model.Tag
import com.example.dhbt.domain.model.Task
import com.example.dhbt.domain.model.TaskPriority
import com.example.dhbt.domain.model.TaskStatus
import com.example.dhbt.presentation.shared.EmojiIcon
import com.example.dhbt.presentation.shared.EmptyStateMessage
import com.example.dhbt.presentation.shared.EmptyStateWithIcon
import com.example.dhbt.presentation.util.toColor
import kotlinx.coroutines.launch
import me.saket.swipe.SwipeAction
import me.saket.swipe.SwipeableActionsBox
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    onTaskClick: (String) -> Unit,
    onAddTask: () -> Unit,
    viewModel: TasksViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val filterState by viewModel.filterState.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val tags by viewModel.tags.collectAsState()
    val datesWithTasks by viewModel.datesWithTasks.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var isCalendarExpanded by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TasksTopAppBar(
                onSearchClicked = { showFilterDialog = true },
                onSortClicked = { showSortMenu = !showSortMenu },
                isFiltersActive = filterState != TaskFilterState(),
                onResetFilters = { viewModel.resetFilters() },
                onToggleEisenhowerMatrix = { viewModel.onToggleEisenhowerMatrix(it) },
                showEisenhowerMatrix = filterState.showEisenhowerMatrix
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTask) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_task))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Календарь (компактный или развернутый)
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                TaskCalendarSimple(
                    isExpanded = isCalendarExpanded,
                    onExpandToggle = { isCalendarExpanded = !isCalendarExpanded },
                    selectedDate = filterState.selectedDate,
                    onDateSelected = { date ->
                        viewModel.onDateSelected(
                            // Если выбрана та же дата, сбрасываем фильтрацию
                            if (date == filterState.selectedDate) null else date
                        )
                    },
                    datesWithTasks = datesWithTasks
                )
            }

            // Фильтр по категориям
            if (categories.isNotEmpty()) {
                CategoryFilterRow(
                    categories = categories,
                    selectedCategoryId = filterState.selectedCategoryId,
                    onCategorySelected = { categoryId ->
                        viewModel.onCategorySelected(
                            if (categoryId == filterState.selectedCategoryId) null else categoryId
                        )
                    }
                )
            }

            // Меню сортировки (отображается при нажатии на кнопку сортировки)
            AnimatedVisibility(
                visible = showSortMenu,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                SortOptionsMenu(
                    currentSortOption = filterState.sortOption,
                    onSortOptionSelected = {
                        viewModel.onSortOptionSelected(it)
                        showSortMenu = false
                    }
                )
            }

            // Список задач или матрица Эйзенхауэра
            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (state.tasks.isEmpty()) {
                EmptyStateWithIcon(
                    message = stringResource(R.string.no_tasks_found),
                    icon = Icons.Default.Assignment,
                    actionLabel = stringResource(R.string.add_task),
                    onActionClicked = onAddTask
                )
            } else if (filterState.showEisenhowerMatrix) {
                EisenhowerMatrix(
                    tasks = state.tasks,
                    onTaskClick = onTaskClick,
                    onTaskStatusChange = { taskId, isCompleted ->
                        viewModel.onTaskStatusChanged(taskId, isCompleted)
                    },
                    onTaskDelete = { taskId ->
                        viewModel.onDeleteTask(taskId)
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = "Задача удалена"
                            )
                        }
                    }
                )
            } else {
                TasksList(
                    tasks = state.tasks,
                    onTaskClick = onTaskClick,
                    onTaskStatusChange = { taskId, isCompleted ->
                        viewModel.onTaskStatusChanged(taskId, isCompleted)
                    },
                    onTaskDelete = { taskId ->
                        viewModel.onDeleteTask(taskId)
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = "Задача удалена"
                            )
                        }
                    },
                    onTaskArchive = { taskId ->
                        viewModel.onArchiveTask(taskId)
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = "Задача архивирована"
                            )
                        }
                    }
                )
            }
        }
    }

    // Диалог фильтрации
    if (showFilterDialog) {
        FilterDialog(
            onDismiss = { showFilterDialog = false },
            currentStatus = filterState.selectedStatus,
            onStatusSelected = { viewModel.onStatusSelected(it) },
            currentPriority = filterState.selectedPriority,
            onPrioritySelected = { viewModel.onPrioritySelected(it) },
            tags = tags,
            selectedTagIds = filterState.selectedTagIds,
            onTagSelected = { viewModel.onTagSelected(it) },
            searchQuery = filterState.searchQuery,
            onSearchQueryChanged = { viewModel.onSearchQueryChanged(it) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksTopAppBar(
    onSearchClicked: () -> Unit,
    onSortClicked: () -> Unit,
    isFiltersActive: Boolean,
    onResetFilters: () -> Unit,
    onToggleEisenhowerMatrix: (Boolean) -> Unit,
    showEisenhowerMatrix: Boolean
) {
    TopAppBar(
        title = { Text(stringResource(R.string.tasks)) },
        actions = {
            // Кнопка поиска
            IconButton(onClick = onSearchClicked) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = stringResource(R.string.search)
                )
            }

            // Кнопка сортировки
            IconButton(onClick = onSortClicked) {
                Icon(
                    imageVector = Icons.Default.Sort,
                    contentDescription = stringResource(R.string.sort)
                )
            }

            // Кнопка матрицы Эйзенхауэра
            IconButton(onClick = { onToggleEisenhowerMatrix(!showEisenhowerMatrix) }) {
                Icon(
                    imageVector = Icons.Default.GridView,
                    contentDescription = stringResource(R.string.eisenhower_matrix),
                    tint = if (showEisenhowerMatrix) MaterialTheme.colorScheme.primary else LocalContentColor.current
                )
            }

            // Кнопка сброса фильтров (отображается только когда фильтры активны)
            AnimatedVisibility(visible = isFiltersActive) {
                IconButton(onClick = onResetFilters) {
                    Icon(
                        imageVector = Icons.Default.FilterAlt,
                        contentDescription = stringResource(R.string.reset_filters),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    )
}

@Composable
fun TaskCalendarSimple(
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    datesWithTasks: Set<LocalDate>
) {
    val today = LocalDate.now()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column {
            // Заголовок календаря с кнопкой разворачивания
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Отображение текущей или выбранной даты
                Text(
                    text = if (selectedDate != null) {
                        selectedDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy"))
                    } else {
                        "Календарь"
                    },
                    style = MaterialTheme.typography.titleMedium
                )

                // Кнопка для сворачивания/разворачивания календаря
                val rotationAngle by animateFloatAsState(
                    targetValue = if (isExpanded) 180f else 0f,
                    label = "calendarExpand"
                )

                IconButton(onClick = onExpandToggle) {
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded)
                            stringResource(R.string.collapse)
                        else
                            stringResource(R.string.expand),
                        modifier = Modifier.rotate(rotationAngle)
                    )
                }
            }

            // Отображение упрощенного календаря, если он развернут
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                SimpleCalendarView(
                    selectedDate = selectedDate,
                    onDateSelected = onDateSelected,
                    datesWithTasks = datesWithTasks
                )
            }
        }
    }
}

@Composable
fun SimpleCalendarView(
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    datesWithTasks: Set<LocalDate>
) {
    val today = LocalDate.now()
    val daysToShow = 28 // Показываем 4 недели
    val startDate = today.minusWeeks(1)

    Column {
        // Дни недели
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val daysOfWeek = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
            daysOfWeek.forEach { day ->
                Text(
                    text = day,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Сетка календаря
        Column {
            for (weekIndex in 0 until 4) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (dayIndex in 0 until 7) {
                        val date = startDate.plusDays((weekIndex * 7 + dayIndex).toLong())
                        val isSelected = selectedDate == date
                        val hasTask = datesWithTasks.contains(date)
                        val isToday = today == date

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isSelected -> MaterialTheme.colorScheme.primary
                                        isToday -> MaterialTheme.colorScheme.primaryContainer
                                        else -> Color.Transparent
                                    }
                                )
                                .then(
                                    if (hasTask && !isSelected) {
                                        Modifier.border(
                                            width = 1.dp,
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = CircleShape
                                        )
                                    } else {
                                        Modifier
                                    }
                                )
                                .clickable { onDateSelected(date) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = date.dayOfMonth.toString(),
                                style = MaterialTheme.typography.bodySmall,
                                color = when {
                                    isSelected -> MaterialTheme.colorScheme.onPrimary
                                    isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryFilterRow(
    categories: List<Category>,
    selectedCategoryId: String?,
    onCategorySelected: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Кнопка "Все"
        item {
            FilterChip(
                selected = selectedCategoryId == null,
                onClick = { onCategorySelected("") },
                label = { Text(stringResource(R.string.all)) },
                leadingIcon = { Icon(Icons.Default.FilterList, null) }
            )
        }

        // Категории
        items(categories) { category ->
            val isSelected = selectedCategoryId == category.id

            FilterChip(
                selected = isSelected,
                onClick = { onCategorySelected(category.id) },
                label = { Text(category.name) },
                leadingIcon = {
                    EmojiIcon(
                        emoji = category.iconEmoji ?: "📝",
                        backgroundColor = category.color?.toColor() ?: MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = category.color?.toColor()?.copy(alpha = 0.2f)
                        ?: MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}

@Composable
fun SortOptionsMenu(
    currentSortOption: SortOption,
    onSortOptionSelected: (SortOption) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = stringResource(R.string.sort_by),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(8.dp)
            )

            SortOptionItem(
                icon = Icons.Default.DateRange,
                title = stringResource(R.string.due_date_asc),
                selected = currentSortOption == SortOption.DATE_ASC,
                onClick = { onSortOptionSelected(SortOption.DATE_ASC) }
            )

            SortOptionItem(
                icon = Icons.Default.DateRange,
                title = stringResource(R.string.due_date_desc),
                selected = currentSortOption == SortOption.DATE_DESC,
                onClick = { onSortOptionSelected(SortOption.DATE_DESC) }
            )

            SortOptionItem(
                icon = Icons.Default.PriorityHigh,
                title = stringResource(R.string.priority_high_to_low),
                selected = currentSortOption == SortOption.PRIORITY_HIGH,
                onClick = { onSortOptionSelected(SortOption.PRIORITY_HIGH) }
            )

            SortOptionItem(
                icon = Icons.Default.LowPriority,
                title = stringResource(R.string.priority_low_to_high),
                selected = currentSortOption == SortOption.PRIORITY_LOW,
                onClick = { onSortOptionSelected(SortOption.PRIORITY_LOW) }
            )

            SortOptionItem(
                icon = Icons.Default.SortByAlpha,
                title = stringResource(R.string.alphabetical),
                selected = currentSortOption == SortOption.ALPHABETICAL,
                onClick = { onSortOptionSelected(SortOption.ALPHABETICAL) }
            )

            SortOptionItem(
                icon = Icons.Default.Update,
                title = stringResource(R.string.creation_date),
                selected = currentSortOption == SortOption.CREATION_DATE,
                onClick = { onSortOptionSelected(SortOption.CREATION_DATE) }
            )
        }
    }
}

@Composable
fun SortOptionItem(
    icon: ImageVector,
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.primary else LocalContentColor.current,
            modifier = Modifier.size(24.dp)
        )

        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        )

        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TasksList(
    tasks: List<Task>,
    onTaskClick: (String) -> Unit,
    onTaskStatusChange: (String, Boolean) -> Unit,
    onTaskDelete: (String) -> Unit,
    onTaskArchive: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 72.dp) // Оставляем место для FAB
    ) {
        // Группировка задач по дате если есть дата
        val tasksByDate = tasks.groupBy { task ->
            task.dueDate?.let { dueDate ->
                LocalDate.ofEpochDay(dueDate / (24 * 60 * 60 * 1000))
            } ?: LocalDate.MAX
        }.toSortedMap()

        // Сначала задачи без даты
        if (tasksByDate.containsKey(LocalDate.MAX)) {
            item {
                Text(
                    text = stringResource(R.string.no_due_date),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            items(
                items = tasksByDate[LocalDate.MAX] ?: emptyList(),
                key = { it.id }
            ) { task ->
                TaskItem(
                    task = task,
                    onTaskClick = onTaskClick,
                    onTaskStatusChange = onTaskStatusChange,
                    onTaskDelete = onTaskDelete,
                    onTaskArchive = onTaskArchive,
                    modifier = Modifier.animateItemPlacement()
                )
            }
        }

        // Затем задачи с датами, сгруппированные по дням
        tasksByDate.entries
            .filter { it.key != LocalDate.MAX }
            .forEach { (date, tasksForDate) ->
                item {
                    Text(
                        text = formatDateHeader(date),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                items(
                    items = tasksForDate,
                    key = { it.id }
                ) { task ->
                    TaskItem(
                        task = task,
                        onTaskClick = onTaskClick,
                        onTaskStatusChange = onTaskStatusChange,
                        onTaskDelete = onTaskDelete,
                        onTaskArchive = onTaskArchive,
                        modifier = Modifier.animateItemPlacement()
                    )
                }
            }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun TaskItem(
    task: Task,
    onTaskClick: (String) -> Unit,
    onTaskStatusChange: (String, Boolean) -> Unit,
    onTaskDelete: (String) -> Unit,
    onTaskArchive: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val completeAction = SwipeAction(
        onSwipe = { onTaskStatusChange(task.id, task.status != TaskStatus.COMPLETED) },
        icon = {
            Icon(
                imageVector = if (task.status == TaskStatus.COMPLETED)
                    Icons.Default.Clear
                else
                    Icons.Default.Check,
                contentDescription = if (task.status == TaskStatus.COMPLETED)
                    stringResource(R.string.mark_incomplete)
                else
                    stringResource(R.string.mark_complete),
                tint = Color.White
            )
        },
        background = if (task.status == TaskStatus.COMPLETED)
            MaterialTheme.colorScheme.error
        else
            Color(0xFF4CAF50)
    )

    val archiveAction = SwipeAction(
        onSwipe = { onTaskArchive(task.id) },
        icon = {
            Icon(
                imageVector = Icons.Default.Archive,
                contentDescription = stringResource(R.string.archive),
                tint = Color.White
            )
        },
        background = MaterialTheme.colorScheme.tertiary
    )

    val deleteAction = SwipeAction(
        onSwipe = { onTaskDelete(task.id) },
        icon = {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = stringResource(R.string.delete),
                tint = Color.White
            )
        },
        background = MaterialTheme.colorScheme.error
    )

    SwipeableActionsBox(
        startActions = listOf(completeAction),
        endActions = listOf(archiveAction, deleteAction),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clickable { onTaskClick(task.id) },
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = if (task.status == TaskStatus.COMPLETED)
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                else
                    MaterialTheme.colorScheme.onSurface
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Чекбокс для статуса
                Checkbox(
                    checked = task.status == TaskStatus.COMPLETED,
                    onCheckedChange = { isChecked ->
                        onTaskStatusChange(task.id, isChecked)
                    },
                    colors = CheckboxDefaults.colors(
                        checkedColor = getPriorityColor(task.priority)
                    )
                )

                // Цветовая метка приоритета
                Box(
                    modifier = Modifier
                        .size(width = 4.dp, height = 36.dp)
                        .background(
                            color = getPriorityColor(task.priority),
                            shape = RoundedCornerShape(2.dp)
                        )
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Основное содержимое задачи
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    // Заголовок задачи
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.alpha(
                            if (task.status == TaskStatus.COMPLETED) 0.6f else 1f
                        )
                    )

                    // Описание (если есть)
                    task.description?.let {
                        if (it.isNotEmpty()) {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.alpha(
                                    if (task.status == TaskStatus.COMPLETED) 0.6f else 1f
                                )
                            )
                        }
                    }

                    // Показываем время, если оно есть
                    task.dueTime?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary.copy(
                                alpha = if (task.status == TaskStatus.COMPLETED) 0.6f else 1f
                            )
                        )
                    }
                }

                // Если есть подзадачи, показываем индикатор прогресса
                if (task.subtasks.isNotEmpty()) {
                    val completed = task.subtasks.count { it.isCompleted }
                    val total = task.subtasks.size

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(
                            text = "$completed/$total",
                            style = MaterialTheme.typography.bodySmall
                        )

                        LinearProgressIndicator(
                            progress = { if (total > 0) completed.toFloat() / total else 0f },
                            modifier = Modifier
                                .width(32.dp)
                                .padding(top = 4.dp),
                            color = getPriorityColor(task.priority),
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }

            // Если есть теги, показываем их внизу карточки
            if (task.tags.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 52.dp, end = 12.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (tag in task.tags.take(3)) {
                        TaskTag(tag = tag)
                    }

                    // Если тегов больше трех, показываем счетчик
                    if (task.tags.size > 3) {
                        Text(
                            text = "+${task.tags.size - 3}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TaskTag(tag: Tag) {
    Surface(
        color = tag.color?.toColor()?.copy(alpha = 0.2f) ?: MaterialTheme.colorScheme.surfaceVariant,
        contentColor = tag.color?.toColor() ?: MaterialTheme.colorScheme.onSurfaceVariant,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = tag.name,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun EisenhowerMatrix(
    tasks: List<Task>,
    onTaskClick: (String) -> Unit,
    onTaskStatusChange: (String, Boolean) -> Unit,
    onTaskDelete: (String) -> Unit
) {
    // Группируем задачи по квадрантам Эйзенхауэра
    val quadrant1Tasks = tasks.filter { it.eisenhowerQuadrant == 1 } // Важные и срочные
    val quadrant2Tasks = tasks.filter { it.eisenhowerQuadrant == 2 } // Важные, но несрочные
    val quadrant3Tasks = tasks.filter { it.eisenhowerQuadrant == 3 } // Неважные, но срочные
    val quadrant4Tasks = tasks.filter { it.eisenhowerQuadrant == 4 } // Неважные и несрочные
    val unassignedTasks = tasks.filter { it.eisenhowerQuadrant == null } // Без квадранта

    Column(modifier = Modifier.fillMaxSize()) {
        // Заголовок
        Text(
            text = stringResource(R.string.eisenhower_matrix),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp)
        )

        // Матрица 2x2
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // Левая колонка (срочные)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                // Квадрант 1: Важно и срочно
                EisenhowerQuadrant(
                    title = stringResource(R.string.important_urgent),
                    tasks = quadrant1Tasks,
                    backgroundColor = Color(0xFFFFCDD2), // Легкий красный
                    onTaskClick = onTaskClick,
                    onTaskStatusChange = onTaskStatusChange,
                    onTaskDelete = onTaskDelete,
                    modifier = Modifier.weight(1f)
                )

                // Квадрант 3: Неважно, но срочно
                EisenhowerQuadrant(
                    title = stringResource(R.string.not_important_urgent),
                    tasks = quadrant3Tasks,
                    backgroundColor = Color(0xFFFFE0B2), // Легкий оранжевый
                    onTaskClick = onTaskClick,
                    onTaskStatusChange = onTaskStatusChange,
                    onTaskDelete = onTaskDelete,
                    modifier = Modifier.weight(1f)
                )
            }

            // Правая колонка (несрочные)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                // Квадрант 2: Важно, но несрочно
                EisenhowerQuadrant(
                    title = stringResource(R.string.important_not_urgent),
                    tasks = quadrant2Tasks,
                    backgroundColor = Color(0xFFE8F5E9), // Легкий зеленый
                    onTaskClick = onTaskClick,
                    onTaskStatusChange = onTaskStatusChange,
                    onTaskDelete = onTaskDelete,
                    modifier = Modifier.weight(1f)
                )

                // Квадрант 4: Неважно и несрочно
                EisenhowerQuadrant(
                    title = stringResource(R.string.not_important_not_urgent),
                    tasks = quadrant4Tasks,
                    backgroundColor = Color(0xFFE1F5FE), // Легкий синий
                    onTaskClick = onTaskClick,
                    onTaskStatusChange = onTaskStatusChange,
                    onTaskDelete = onTaskDelete,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Задачи без назначенного квадранта
        if (unassignedTasks.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = stringResource(R.string.unassigned_tasks),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(8.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                ) {
                    items(unassignedTasks) { task ->
                        TaskItemCompact(
                            task = task,
                            onTaskClick = onTaskClick,
                            onTaskStatusChange = onTaskStatusChange
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EisenhowerQuadrant(
    title: String,
    tasks: List<Task>,
    backgroundColor: Color,
    onTaskClick: (String) -> Unit,
    onTaskStatusChange: (String, Boolean) -> Unit,
    onTaskDelete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxSize()
            .padding(4.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(8.dp)
            )

            if (tasks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_tasks),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(4.dp)
                ) {
                    items(tasks) { task ->
                        TaskItemCompact(
                            task = task,
                            onTaskClick = onTaskClick,
                            onTaskStatusChange = onTaskStatusChange
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TaskItemCompact(
    task: Task,
    onTaskClick: (String) -> Unit,
    onTaskStatusChange: (String, Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTaskClick(task.id) }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = task.status == TaskStatus.COMPLETED,
            onCheckedChange = { isChecked ->
                onTaskStatusChange(task.id, isChecked)
            },
            modifier = Modifier.padding(end = 8.dp)
        )

        Text(
            text = task.title,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .alpha(if (task.status == TaskStatus.COMPLETED) 0.6f else 1f)
        )
    }
}

@Composable
fun FilterDialog(
    onDismiss: () -> Unit,
    currentStatus: TaskStatus?,
    onStatusSelected: (TaskStatus?) -> Unit,
    currentPriority: TaskPriority?,
    onPrioritySelected: (TaskPriority?) -> Unit,
    tags: List<Tag>,
    selectedTagIds: List<String>,
    onTagSelected: (String) -> Unit,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.filter_tasks))
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                // Поле поиска
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChanged,
                    label = { Text(stringResource(R.string.search)) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    singleLine = true
                )

                // Фильтр по статусу
                Text(
                    text = stringResource(R.string.status),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                StatusFilterChips(
                    currentStatus = currentStatus,
                    onStatusSelected = onStatusSelected
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Фильтр по приоритету
                Text(
                    text = stringResource(R.string.priority),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                PriorityFilterChips(
                    currentPriority = currentPriority,
                    onPrioritySelected = onPrioritySelected
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Фильтр по тегам
                if (tags.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.tags),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    FlowRow(
                        mainAxisSpacing = 8.dp,
                        crossAxisSpacing = 8.dp,
                    ) {
                        tags.forEach { tag ->
                            val isSelected = selectedTagIds.contains(tag.id)

                            FilterChip(
                                selected = isSelected,
                                onClick = { onTagSelected(tag.id) },
                                label = { Text(tag.name) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = tag.color?.toColor()?.copy(alpha = 0.2f)
                                        ?: MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.done))
            }
        },
        dismissButton = {
            TextButton(onClick = {
                onStatusSelected(null)
                onPrioritySelected(null)
                onSearchQueryChanged("")
            }) {
                Text(stringResource(R.string.reset))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusFilterChips(
    currentStatus: TaskStatus?,
    onStatusSelected: (TaskStatus?) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = currentStatus == TaskStatus.ACTIVE,
            onClick = { onStatusSelected(
                if (currentStatus == TaskStatus.ACTIVE) null else TaskStatus.ACTIVE
            ) },
            label = { Text(stringResource(R.string.active)) },
            leadingIcon = if (currentStatus == TaskStatus.ACTIVE) {
                { Icon(Icons.Default.Check, contentDescription = null) }
            } else null
        )

        FilterChip(
            selected = currentStatus == TaskStatus.COMPLETED,
            onClick = { onStatusSelected(
                if (currentStatus == TaskStatus.COMPLETED) null else TaskStatus.COMPLETED
            ) },
            label = { Text(stringResource(R.string.completed)) },
            leadingIcon = if (currentStatus == TaskStatus.COMPLETED) {
                { Icon(Icons.Default.Check, contentDescription = null) }
            } else null
        )

        FilterChip(
            selected = currentStatus == TaskStatus.ARCHIVED,
            onClick = { onStatusSelected(
                if (currentStatus == TaskStatus.ARCHIVED) null else TaskStatus.ARCHIVED
            ) },
            label = { Text(stringResource(R.string.archived)) },
            leadingIcon = if (currentStatus == TaskStatus.ARCHIVED) {
                { Icon(Icons.Default.Check, contentDescription = null) }
            } else null
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriorityFilterChips(
    currentPriority: TaskPriority?,
    onPrioritySelected: (TaskPriority?) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = currentPriority == TaskPriority.HIGH,
            onClick = { onPrioritySelected(
                if (currentPriority == TaskPriority.HIGH) null else TaskPriority.HIGH
            ) },
            label = { Text(stringResource(R.string.high)) },
            leadingIcon = if (currentPriority == TaskPriority.HIGH) {
                { Icon(Icons.Default.Check, contentDescription = null) }
            } else null
        )

        FilterChip(
            selected = currentPriority == TaskPriority.MEDIUM,
            onClick = { onPrioritySelected(
                if (currentPriority == TaskPriority.MEDIUM) null else TaskPriority.MEDIUM
            ) },
            label = { Text(stringResource(R.string.medium)) },
            leadingIcon = if (currentPriority == TaskPriority.MEDIUM) {
                { Icon(Icons.Default.Check, contentDescription = null) }
            } else null
        )

        FilterChip(
            selected = currentPriority == TaskPriority.LOW,
            onClick = { onPrioritySelected(
                if (currentPriority == TaskPriority.LOW) null else TaskPriority.LOW
            ) },
            label = { Text(stringResource(R.string.low)) },
            leadingIcon = if (currentPriority == TaskPriority.LOW) {
                { Icon(Icons.Default.Check, contentDescription = null) }
            } else null
        )
    }
}

// Вспомогательные функции

@Composable
fun getPriorityColor(priority: TaskPriority): Color {
    return when (priority) {
        TaskPriority.HIGH -> Color(0xFFF44336) // Красный
        TaskPriority.MEDIUM -> Color(0xFFFF9800) // Оранжевый
        TaskPriority.LOW -> Color(0xFF4CAF50) // Зеленый
    }
}

@Composable
fun formatDateHeader(date: LocalDate): String {
    val today = LocalDate.now()
    val tomorrow = today.plusDays(1)
    val yesterday = today.minusDays(1)

    return when (date) {
        today -> stringResource(R.string.today)
        tomorrow -> stringResource(R.string.tomorrow)
        yesterday -> stringResource(R.string.yesterday)
        else -> date.format(DateTimeFormatter.ofPattern("d MMMM"))
    }
}

@Composable
fun FlowRow(
    mainAxisSpacing: Dp = 0.dp,
    crossAxisSpacing: Dp = 0.dp,
    content: @Composable () -> Unit
) {
    Layout(
        content = content
    ) { measurables, constraints ->
        val rows = mutableListOf<MeasuredRow>()
        val maxWidth = constraints.maxWidth

        var rowCurrentWidth = 0
        var rowItems = mutableListOf<Pair<Measurable, androidx.compose.ui.layout.Placeable>>()

        // Измеряем каждый элемент
        measurables.forEach { measurable ->
            val placeable = measurable.measure(constraints.copy(maxWidth = constraints.maxWidth))

            val wouldExceedMaxWidth = rowCurrentWidth + placeable.width +
                    if (rowItems.isEmpty()) 0 else mainAxisSpacing.roundToPx()

            if (wouldExceedMaxWidth > maxWidth) {
                // Не хватает места в текущем ряду, создаем новый ряд
                rows.add(MeasuredRow(rowItems.toList(), rowCurrentWidth))
                rowItems = mutableListOf()
                rowCurrentWidth = 0
            }

            // Добавляем элемент в текущий ряд
            rowItems.add(measurable to placeable)
            rowCurrentWidth += placeable.width + if (rowCurrentWidth == 0) 0 else mainAxisSpacing.roundToPx()
        }

        // Добавляем последний ряд, если он не пустой
        if (rowItems.isNotEmpty()) {
            rows.add(MeasuredRow(rowItems.toList(), rowCurrentWidth))
        }

        // Вычисляем общую высоту
        val height = rows.sumOf { row ->
            row.items.maxOfOrNull { it.second.height } ?: 0
        } + (rows.size - 1).coerceAtLeast(0) * crossAxisSpacing.roundToPx()

        // Устанавливаем размер layout
        layout(maxWidth, height.coerceAtMost(constraints.maxHeight)) {
            var yPosition = 0

            // Размещаем каждый ряд
            rows.forEach { row ->
                var xPosition = 0
                val rowHeight = row.items.maxOfOrNull { it.second.height } ?: 0

                // Размещаем элементы внутри ряда
                row.items.forEach { (_, placeable) ->
                    placeable.placeRelative(xPosition, yPosition)
                    xPosition += placeable.width + mainAxisSpacing.roundToPx()
                }

                yPosition += rowHeight + crossAxisSpacing.roundToPx()
            }
        }
    }
}

private class MeasuredRow(
    val items: List<Pair<Measurable, androidx.compose.ui.layout.Placeable>>,
    val width: Int
)