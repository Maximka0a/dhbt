package com.example.dhbt.presentation.task.list

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
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
import com.example.dhbt.presentation.shared.EmptyStateWithIcon
import com.example.dhbt.presentation.util.toColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.saket.swipe.SwipeAction
import me.saket.swipe.SwipeableActionsBox
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalComposeUiApi::class,
    ExperimentalFoundationApi::class
)
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
    val keyboardController = LocalSoftwareKeyboardController.current

    // Состояния UI
    var isCalendarExpanded by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }
    var expandedFilterSection by remember { mutableStateOf(false) }

    val searchFocusRequester = remember { FocusRequester() }

    // Анимации
    val searchBarHeight by animateFloatAsState(
        targetValue = if (isSearchActive) 1f else 0f,
        label = "searchBarHeight"
    )

    Scaffold(
        topBar = {
            TasksTopAppBar(
                isSearchActive = isSearchActive,
                onSearchActiveChange = { active ->
                    isSearchActive = active
                    if (active) {
                        scope.launch {
                            delay(100) // Небольшая задержка для анимации
                            searchFocusRequester.requestFocus()
                        }
                    } else {
                        keyboardController?.hide()
                        // Если пользователь закрывает поиск, очищаем поисковый запрос
                        if (filterState.searchQuery.isNotEmpty()) {
                            viewModel.onSearchQueryChanged("")
                        }
                    }
                },
                searchQuery = filterState.searchQuery,
                onSearchQueryChanged = { query -> viewModel.onSearchQueryChanged(query) },
                onClearSearch = { viewModel.onSearchQueryChanged("") },
                onSearchSubmit = { keyboardController?.hide() },
                onSortClicked = { showSortMenu = !showSortMenu },
                onExpandFilterClicked = { expandedFilterSection = !expandedFilterSection },
                isFiltersActive = filterState != TaskFilterState(),
                onResetFilters = { viewModel.resetFilters() },
                onToggleEisenhowerMatrix = { viewModel.onToggleEisenhowerMatrix(it) },
                showEisenhowerMatrix = filterState.showEisenhowerMatrix,
                searchFocusRequester = searchFocusRequester,
                filterState = filterState
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTask,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 6.dp,
                    pressedElevation = 8.dp
                )
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.add_task)
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Расширенная панель фильтров (отображается, когда пользователь нажимает на фильтр)
            AnimatedVisibility(
                visible = expandedFilterSection,
                enter = fadeIn() + expandVertically(animationSpec = tween(300)),
                exit = fadeOut() + shrinkVertically(animationSpec = tween(300))
            ) {
                ExpandedFilterSection(
                    selectedStatus = filterState.selectedStatus,
                    onStatusSelected = { viewModel.onStatusSelected(it) },
                    selectedPriority = filterState.selectedPriority,
                    onPrioritySelected = { viewModel.onPrioritySelected(it) },
                    tags = tags,
                    selectedTagIds = filterState.selectedTagIds,
                    onTagSelected = { viewModel.onTagSelected(it) }
                )
            }

            // Календарь (компактный или развернутый)
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                TaskCalendarView(
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
                EmptyTasksState(
                    searchQuery = filterState.searchQuery,
                    onAddTask = onAddTask
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
                            val result = snackbarHostState.showSnackbar(
                                message = "Задача удалена",
                                actionLabel = "Отменить",
                                duration = SnackbarDuration.Short
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                // TODO: Реализовать восстановление задачи
                            }
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
                            val result = snackbarHostState.showSnackbar(
                                message = "Задача удалена",
                                actionLabel = "Отменить",
                                duration = SnackbarDuration.Short
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                // TODO: Реализовать восстановление задачи
                            }
                        }
                    },
                    onTaskArchive = { taskId ->
                        viewModel.onArchiveTask(taskId)
                        scope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = "Задача архивирована",
                                actionLabel = "Отменить",
                                duration = SnackbarDuration.Short
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                // TODO: Реализовать восстановление задачи из архива
                            }
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun TasksTopAppBar(
    isSearchActive: Boolean,
    onSearchActiveChange: (Boolean) -> Unit,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onClearSearch: () -> Unit,
    onSearchSubmit: () -> Unit,
    onSortClicked: () -> Unit,
    onExpandFilterClicked: () -> Unit,
    isFiltersActive: Boolean,
    onResetFilters: () -> Unit,
    onToggleEisenhowerMatrix: (Boolean) -> Unit,
    showEisenhowerMatrix: Boolean,
    searchFocusRequester: FocusRequester,
    filterState: TaskFilterState
) {
    Column {
        // Основной верхний бар
        TopAppBar(
            title = {
                if (!isSearchActive) {
                    Text(stringResource(R.string.tasks))
                }
            },
            actions = {
                // Поиск (значок или поле ввода)
                if (isSearchActive) {
                    // Поле поиска
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChanged,
                        placeholder = { Text(stringResource(R.string.search_tasks)) },
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = onClearSearch) {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = stringResource(R.string.clear)
                                    )
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onSearchSubmit() }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 8.dp)
                            .focusRequester(searchFocusRequester),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            cursorColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                } else {
                    // Кнопка поиска
                    IconButton(onClick = { onSearchActiveChange(true) }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = stringResource(R.string.search)
                        )
                    }

                    // Кнопка фильтров
                    IconButton(onClick = onExpandFilterClicked) {
                        BadgedBox(
                            badge = {
                                if (isFiltersActive) {
                                    Badge { Text("!") }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = stringResource(R.string.filter)
                            )
                        }
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
                            imageVector = Icons.Outlined.GridView,
                            contentDescription = stringResource(R.string.eisenhower_matrix),
                            tint = if (showEisenhowerMatrix)
                                MaterialTheme.colorScheme.primary
                            else
                                LocalContentColor.current
                        )
                    }

                    // Кнопка сброса фильтров (отображается только когда фильтры активны)
                    AnimatedVisibility(visible = isFiltersActive) {
                        IconButton(onClick = onResetFilters) {
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = stringResource(R.string.reset_filters),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            },
            navigationIcon = {
                if (isSearchActive) {
                    IconButton(onClick = { onSearchActiveChange(false) }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.close_search)
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface
            )
        )

        // Индикатор активных фильтров
        if (isFiltersActive && !isSearchActive) {
            ActiveFiltersIndicator(
                filterState = filterState,
                onResetFilters = onResetFilters
            )
        }
    }
}

@Composable
fun ActiveFiltersIndicator(
    filterState: TaskFilterState,
    onResetFilters: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.FilterAlt,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )

        Text(
            text = buildFilterDescription(filterState),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
        )

        TextButton(
            onClick = onResetFilters,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
        ) {
            Text(
                text = stringResource(R.string.clear_all),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun buildFilterDescription(filterState: TaskFilterState): String {
    val filters = mutableListOf<String>()

    filterState.selectedDate?.let {
        filters.add("Дата: ${it.format(DateTimeFormatter.ofPattern("d MMM"))}")
    }

    filterState.selectedCategoryId?.let {
        filters.add("Категория")
    }

    filterState.selectedStatus?.let {
        val statusName = when(it) {
            TaskStatus.ACTIVE -> stringResource(R.string.active)
            TaskStatus.COMPLETED -> stringResource(R.string.completed)
            TaskStatus.ARCHIVED -> stringResource(R.string.archived)
        }
        filters.add("Статус: $statusName")
    }

    filterState.selectedPriority?.let {
        val priorityName = when(it) {
            TaskPriority.HIGH -> stringResource(R.string.high)
            TaskPriority.MEDIUM -> stringResource(R.string.medium)
            TaskPriority.LOW -> stringResource(R.string.low)
        }
        filters.add("Приоритет: $priorityName")
    }

    if (filterState.selectedTagIds.isNotEmpty()) {
        filters.add("Теги: ${filterState.selectedTagIds.size}")
    }

    if (filterState.searchQuery.isNotEmpty()) {
        filters.add("Поиск: \"${filterState.searchQuery}\"")
    }

    return filters.joinToString(" | ")
}

@Composable
fun ExpandedFilterSection(
    selectedStatus: TaskStatus?,
    onStatusSelected: (TaskStatus?) -> Unit,
    selectedPriority: TaskPriority?,
    onPrioritySelected: (TaskPriority?) -> Unit,
    tags: List<Tag>,
    selectedTagIds: List<String>,
    onTagSelected: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Заголовок
            Text(
                text = stringResource(R.string.filter_tasks),
                style = MaterialTheme.typography.titleMedium
            )

            // Статусы
            Text(
                text = stringResource(R.string.status),
                style = MaterialTheme.typography.labelMedium
            )

            StatusFilterChips(
                currentStatus = selectedStatus,
                onStatusSelected = onStatusSelected
            )

            // Приоритеты
            Text(
                text = stringResource(R.string.priority),
                style = MaterialTheme.typography.labelMedium
            )

            PriorityFilterChips(
                currentPriority = selectedPriority,
                onPrioritySelected = onPrioritySelected
            )

            // Теги
            if (tags.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.tags),
                    style = MaterialTheme.typography.labelMedium
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
    }
}

@Composable
fun TaskCalendarView(
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
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column {
            // Заголовок календаря с кнопкой разворачивания
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Отображение текущей или выбранной даты
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DateRange,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = if (selectedDate != null) {
                            selectedDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy"))
                        } else {
                            stringResource(R.string.today) + ": " +
                                    today.format(DateTimeFormatter.ofPattern("d MMMM"))
                        },
                        style = MaterialTheme.typography.titleMedium
                    )
                }

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

    Column(
        modifier = Modifier.padding(bottom = 16.dp)
    ) {
        // Дни недели
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val daysOfWeek = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
            daysOfWeek.forEach { day ->
                Text(
                    text = day,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Сетка календаря
        Column {
            for (weekIndex in 0 until 4) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (dayIndex in 0 until 7) {
                        val date = startDate.plusDays((weekIndex * 7 + dayIndex).toLong())
                        val isSelected = selectedDate == date
                        val hasTask = datesWithTasks.contains(date)
                        val isToday = today == date
                        val isWeekend = date.dayOfWeek.value > 5

                        // Подсветка выходных дней другим цветом
                        val dayColor = when {
                            isSelected -> MaterialTheme.colorScheme.onPrimary
                            isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                            isWeekend -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            else -> MaterialTheme.colorScheme.onSurface
                        }

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
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() },
                                    onClick = { onDateSelected(date) }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = date.dayOfMonth.toString(),
                                style = MaterialTheme.typography.bodySmall,
                                color = dayColor
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
            .padding(vertical = 8.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Кнопка "Все"
        item {
            FilterChip(
                selected = selectedCategoryId == null,
                onClick = { onCategorySelected("") },
                label = { Text(stringResource(R.string.all)) },
                leadingIcon = {
                    Icon(
                        Icons.Default.FilterList,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                shape = RoundedCornerShape(16.dp)
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
                        modifier = Modifier.size(18.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = category.color?.toColor()?.copy(alpha = 0.2f)
                        ?: MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(16.dp)
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
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Column(modifier = Modifier.padding(vertical = 12.dp)) {
            Text(
                text = stringResource(R.string.sort_by),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            SortOptionItem(
                icon = Icons.Outlined.DateRange,
                title = stringResource(R.string.due_date_asc),
                selected = currentSortOption == SortOption.DATE_ASC,
                onClick = { onSortOptionSelected(SortOption.DATE_ASC) }
            )

            SortOptionItem(
                icon = Icons.Outlined.DateRange,
                title = stringResource(R.string.due_date_desc),
                selected = currentSortOption == SortOption.DATE_DESC,
                onClick = { onSortOptionSelected(SortOption.DATE_DESC) }
            )

            SortOptionItem(
                icon = Icons.Outlined.PriorityHigh,
                title = stringResource(R.string.priority_high_to_low),
                selected = currentSortOption == SortOption.PRIORITY_HIGH,
                onClick = { onSortOptionSelected(SortOption.PRIORITY_HIGH) }
            )

            SortOptionItem(
                icon = Icons.Outlined.LowPriority,
                title = stringResource(R.string.priority_low_to_high),
                selected = currentSortOption == SortOption.PRIORITY_LOW,
                onClick = { onSortOptionSelected(SortOption.PRIORITY_LOW) }
            )

            SortOptionItem(
                icon = Icons.Outlined.SortByAlpha,
                title = stringResource(R.string.alphabetical),
                selected = currentSortOption == SortOption.ALPHABETICAL,
                onClick = { onSortOptionSelected(SortOption.ALPHABETICAL) }
            )

            SortOptionItem(
                icon = Icons.Outlined.Update,
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
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.primary else LocalContentColor.current.copy(alpha = 0.7f),
            modifier = Modifier.size(20.dp)
        )

        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = if (selected)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        )

        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun EmptyTasksState(
    searchQuery: String,
    onAddTask: () -> Unit
) {
    if (searchQuery.isNotEmpty()) {
        // Пустой результат поиска
        EmptyStateWithIcon(
            message = stringResource(R.string.no_search_results, searchQuery),
            icon = Icons.Outlined.SearchOff,
        )
    } else {
        // Нет задач вообще
        EmptyStateWithIcon(
            message = stringResource(R.string.no_tasks_found),
            icon = Icons.Outlined.Assignment,
            actionLabel = stringResource(R.string.add_task),
            onActionClicked = onAddTask
        )
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
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 8.dp,
            bottom = 88.dp
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = tasks,
            key = { it.id }
        ) { task ->
            TaskItem(
                task = task,
                onClick = { onTaskClick(task.id) },
                onStatusChange = { isCompleted -> onTaskStatusChange(task.id, isCompleted) },
                onDelete = { onTaskDelete(task.id) },
                onArchive = { onTaskArchive(task.id) },
                modifier = Modifier.animateItemPlacement()
            )
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun TaskItem(
    task: Task,
    onClick: () -> Unit,
    onStatusChange: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onArchive: () -> Unit,
    modifier: Modifier = Modifier
) {
    val deleteAction = SwipeAction(
        icon = {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = stringResource(R.string.delete),
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        },
        background = Color.Red,
        onSwipe = onDelete
    )

    val archiveAction = SwipeAction(
        icon = {
            Icon(
                imageVector = Icons.Default.Archive,
                contentDescription = stringResource(R.string.archive),
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        },
        background = MaterialTheme.colorScheme.tertiary,
        onSwipe = onArchive
    )

    SwipeableActionsBox(
        startActions = listOf(archiveAction),
        endActions = listOf(deleteAction),
        swipeThreshold = 96.dp,
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 2.dp
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Чекбокс для изменения статуса задачи
                Checkbox(
                    checked = task.status == TaskStatus.COMPLETED,
                    onCheckedChange = { isChecked -> onStatusChange(isChecked) },
                    colors = CheckboxDefaults.colors(
                        checkedColor = when (task.priority) {
                            TaskPriority.HIGH -> MaterialTheme.colorScheme.error
                            TaskPriority.MEDIUM -> MaterialTheme.colorScheme.tertiary
                            TaskPriority.LOW -> MaterialTheme.colorScheme.primary
                        }
                    )
                )

                // Основное содержимое задачи
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                ) {
                    // Название задачи
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (task.status == TaskStatus.COMPLETED)
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        else
                            MaterialTheme.colorScheme.onSurface,
                        textDecoration = if (task.status == TaskStatus.COMPLETED)
                            TextDecoration.LineThrough
                        else
                            null,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Дополнительная информация (дата, категория)
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Срок выполнения
                        task.dueDate?.let { dueDate ->
                            val localDate = Instant.ofEpochMilli(dueDate)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()

                            val dateColor = when {
                                task.status == TaskStatus.COMPLETED -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                localDate.isBefore(LocalDate.now()) -> MaterialTheme.colorScheme.error
                                localDate.isEqual(LocalDate.now()) -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Event,
                                    contentDescription = null,
                                    tint = dateColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = formatDueDate(localDate),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = dateColor
                                )
                            }
                        }

                        // Категория (необходимо получить из категорий по ID)
                        task.categoryId?.let { categoryId ->
                            // Получаем цвет из задачи, если он есть
                            val categoryColor = task.color?.toColor() ?: MaterialTheme.colorScheme.primary

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(categoryColor, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = categoryId, // В идеале здесь должно быть имя категории
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Индикатор приоритета
                Box(
                    modifier = Modifier
                        .size(width = 4.dp, height = 40.dp)
                        .background(
                            color = when (task.priority) {
                                TaskPriority.HIGH -> MaterialTheme.colorScheme.error
                                TaskPriority.MEDIUM -> MaterialTheme.colorScheme.tertiary
                                TaskPriority.LOW -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            },
                            shape = RoundedCornerShape(2.dp)
                        )
                )
            }
        }
    }
}

@Composable
fun formatDueDate(date: LocalDate): String {
    val today = LocalDate.now()
    val tomorrow = today.plusDays(1)

    return when {
        date.isEqual(today) -> stringResource(R.string.today)
        date.isEqual(tomorrow) -> stringResource(R.string.tomorrow)
        date.isBefore(today) -> stringResource(R.string.overdue) + ": " +
                date.format(DateTimeFormatter.ofPattern("d MMM"))
        date.year == today.year -> date.format(DateTimeFormatter.ofPattern("d MMM"))
        else -> date.format(DateTimeFormatter.ofPattern("d MMM yyyy"))
    }
}

@Composable
fun EisenhowerMatrix(
    tasks: List<Task>,
    onTaskClick: (String) -> Unit,
    onTaskStatusChange: (String, Boolean) -> Unit,
    onTaskDelete: (String) -> Unit
) {
    // Преобразуем даты задач в LocalDate для сравнения
    val today = LocalDate.now()
    val twoDaysLater = today.plusDays(2)

    // Группируем задачи по срочности и важности
    val urgentImportant = tasks.filter { task ->
        task.priority == TaskPriority.HIGH &&
                (task.dueDate?.let { dueDate ->
                    val taskDate = Instant.ofEpochMilli(dueDate)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                    taskDate.isBefore(twoDaysLater)
                } ?: false)
    }

    val nonUrgentImportant = tasks.filter { task ->
        task.priority == TaskPriority.HIGH &&
                (task.dueDate?.let { dueDate ->
                    val taskDate = Instant.ofEpochMilli(dueDate)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                    taskDate.isAfter(twoDaysLater.minusDays(1))
                } ?: true)
    }

    val urgentNotImportant = tasks.filter { task ->
        task.priority != TaskPriority.HIGH &&
                (task.dueDate?.let { dueDate ->
                    val taskDate = Instant.ofEpochMilli(dueDate)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                    taskDate.isBefore(twoDaysLater)
                } ?: false)
    }

    val nonUrgentNotImportant = tasks.filter { task ->
        task.priority != TaskPriority.HIGH &&
                (task.dueDate?.let { dueDate ->
                    val taskDate = Instant.ofEpochMilli(dueDate)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                    taskDate.isAfter(twoDaysLater.minusDays(1))
                } ?: true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            // Срочные и важные
            EisenhowerQuadrant(
                title = stringResource(R.string.urgent_important),
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                tasks = urgentImportant,
                onTaskClick = onTaskClick,
                onTaskStatusChange = onTaskStatusChange,
                onTaskDelete = onTaskDelete,
                modifier = Modifier.weight(1f)
            )

            // Не срочные, но важные
            EisenhowerQuadrant(
                title = stringResource(R.string.not_urgent_important),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                tasks = nonUrgentImportant,
                onTaskClick = onTaskClick,
                onTaskStatusChange = onTaskStatusChange,
                onTaskDelete = onTaskDelete,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            // Срочные, но не важные
            EisenhowerQuadrant(
                title = stringResource(R.string.urgent_not_important),
                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
                tasks = urgentNotImportant,
                onTaskClick = onTaskClick,
                onTaskStatusChange = onTaskStatusChange,
                onTaskDelete = onTaskDelete,
                modifier = Modifier.weight(1f)
            )

            // Не срочные и не важные
            EisenhowerQuadrant(
                title = stringResource(R.string.not_urgent_not_important),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                tasks = nonUrgentNotImportant,
                onTaskClick = onTaskClick,
                onTaskStatusChange = onTaskStatusChange,
                onTaskDelete = onTaskDelete,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun EisenhowerQuadrant(
    title: String,
    color: Color,
    tasks: List<Task>,
    onTaskClick: (String) -> Unit,
    onTaskStatusChange: (String, Boolean) -> Unit,
    onTaskDelete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .padding(4.dp)
            .fillMaxSize(),
        colors = CardDefaults.cardColors(
            containerColor = color
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Заголовок квадранта
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.3f))
                    .padding(8.dp),
                textAlign = TextAlign.Center
            )

            // Список задач
            if (tasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_tasks),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(tasks) { task ->
                        CompactTaskItem(
                            task = task,
                            onClick = { onTaskClick(task.id) },
                            onStatusChange = { isCompleted -> onTaskStatusChange(task.id, isCompleted) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CompactTaskItem(
    task: Task,
    onClick: () -> Unit,
    onStatusChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.status == TaskStatus.COMPLETED,
                onCheckedChange = { isChecked -> onStatusChange(isChecked) },
                colors = CheckboxDefaults.colors(
                    checkedColor = when (task.priority) {
                        TaskPriority.HIGH -> MaterialTheme.colorScheme.error
                        TaskPriority.MEDIUM -> MaterialTheme.colorScheme.tertiary
                        TaskPriority.LOW -> MaterialTheme.colorScheme.primary
                    }
                )
            )

            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (task.status == TaskStatus.COMPLETED)
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                else
                    MaterialTheme.colorScheme.onSurface,
                textDecoration = if (task.status == TaskStatus.COMPLETED)
                    TextDecoration.LineThrough
                else
                    null,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun StatusFilterChips(
    currentStatus: TaskStatus?,
    onStatusSelected: (TaskStatus?) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Все задачи
        item {
            FilterChip(
                selected = currentStatus == null,
                onClick = { onStatusSelected(null) },
                label = { Text(stringResource(R.string.all)) }
            )
        }

        // Активные задачи
        item {
            FilterChip(
                selected = currentStatus == TaskStatus.ACTIVE,
                onClick = { onStatusSelected(TaskStatus.ACTIVE) },
                label = { Text(stringResource(R.string.active)) }
            )
        }

        // Завершенные задачи
        item {
            FilterChip(
                selected = currentStatus == TaskStatus.COMPLETED,
                onClick = { onStatusSelected(TaskStatus.COMPLETED) },
                label = { Text(stringResource(R.string.completed)) }
            )
        }

        // Архивные задачи
        item {
            FilterChip(
                selected = currentStatus == TaskStatus.ARCHIVED,
                onClick = { onStatusSelected(TaskStatus.ARCHIVED) },
                label = { Text(stringResource(R.string.archived)) }
            )
        }
    }
}

@Composable
fun PriorityFilterChips(
    currentPriority: TaskPriority?,
    onPrioritySelected: (TaskPriority?) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Все приоритеты
        item {
            FilterChip(
                selected = currentPriority == null,
                onClick = { onPrioritySelected(null) },
                label = { Text(stringResource(R.string.all)) }
            )
        }

        // Высокий приоритет
        item {
            FilterChip(
                selected = currentPriority == TaskPriority.HIGH,
                onClick = { onPrioritySelected(TaskPriority.HIGH) },
                label = { Text(stringResource(R.string.high)) },
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(MaterialTheme.colorScheme.error, CircleShape)
                    )
                }
            )
        }

        // Средний приоритет
        item {
            FilterChip(
                selected = currentPriority == TaskPriority.MEDIUM,
                onClick = { onPrioritySelected(TaskPriority.MEDIUM) },
                label = { Text(stringResource(R.string.medium)) },
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(MaterialTheme.colorScheme.tertiary, CircleShape)
                    )
                }
            )
        }

        // Низкий приоритет
        item {
            FilterChip(
                selected = currentPriority == TaskPriority.LOW,
                onClick = { onPrioritySelected(TaskPriority.LOW) },
                label = { Text(stringResource(R.string.low)) },
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    )
                }
            )
        }
    }
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
        val sequences = mutableListOf<List<Measurable>>()
        val crossAxisSizes = mutableListOf<Int>()
        val crossAxisPositions = mutableListOf<Int>()

        var mainAxisSpace = 0
        var crossAxisSpace = 0

        val currentSequence = mutableListOf<Measurable>()
        var currentMainAxisSize = 0
        var currentCrossAxisSize = 0

        // Разбиваем на строки
        for (i in measurables.indices) {
            val measurable = measurables[i]

            // Измеряем элемент
            val placeable = measurable.measure(constraints)

            // Если не помещается в текущую строку - начинаем новую
            if (currentSequence.isNotEmpty() && currentMainAxisSize + mainAxisSpacing.roundToPx() + placeable.width > constraints.maxWidth) {
                sequences.add(currentSequence.toList())
                crossAxisSizes.add(currentCrossAxisSize)
                crossAxisPositions.add(crossAxisSpace)

                crossAxisSpace += currentCrossAxisSize + crossAxisSpacing.roundToPx()

                currentSequence.clear()
                currentMainAxisSize = 0
                currentCrossAxisSize = 0
            }

            // Добавляем в текущую строку
            currentSequence.add(measurable)
            currentMainAxisSize += placeable.width + if (currentSequence.size > 1) mainAxisSpacing.roundToPx() else 0
            currentCrossAxisSize = maxOf(currentCrossAxisSize, placeable.height)
        }

        // Добавляем последнюю строку, если она не пуста
        if (currentSequence.isNotEmpty()) {
            sequences.add(currentSequence.toList())
            crossAxisSizes.add(currentCrossAxisSize)
            crossAxisPositions.add(crossAxisSpace)
            crossAxisSpace += currentCrossAxisSize
        }

        val layoutHeight = crossAxisSpace

        // Размещаем элементы
        layout(constraints.maxWidth, layoutHeight) {
            sequences.forEachIndexed { i, seq ->
                var mainAxisPos = 0

                seq.forEach { measurable ->
                    val placeable = measurable.measure(constraints)
                    placeable.place(
                        x = mainAxisPos,
                        y = crossAxisPositions[i]
                    )
                    mainAxisPos += placeable.width + mainAxisSpacing.roundToPx()
                }
            }
        }
    }
}