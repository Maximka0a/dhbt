package com.example.dhbt.presentation.task.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.LowPriority
import androidx.compose.material.icons.outlined.PriorityHigh
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.SortByAlpha
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalHapticFeedback
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
import com.example.dhbt.presentation.dashboard.components.SwipeableTaskItem
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

    // Track which UI section is currently expanded
    var expandedSection by remember { mutableStateOf<ExpandedSection?>(null) }
    val searchFocusRequester = remember { FocusRequester() }

    // Helper functions to manage expanded sections
    fun toggleSection(section: ExpandedSection) {
        expandedSection = if (expandedSection == section) null else section

        // If switching to search section, request focus after a short delay
        if (expandedSection == ExpandedSection.SEARCH) {
            scope.launch {
                delay(100)
                searchFocusRequester.requestFocus()
            }
        } else {
            // If not in search, hide keyboard
            keyboardController?.hide()
        }
    }

    Scaffold(
        topBar = {
            TasksTopAppBar(
                expandedSection = expandedSection,
                onSectionToggle = { section -> toggleSection(section) },
                isFiltersActive = filterState != TaskFilterState(),
                onResetFilters = { viewModel.resetFilters() },
                showEisenhowerMatrix = filterState.showEisenhowerMatrix,
                onToggleEisenhowerMatrix = { show ->
                    viewModel.onToggleEisenhowerMatrix(show)
                },
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
            // Expanded filter section
            AnimatedVisibility(
                visible = expandedSection == ExpandedSection.FILTER,
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

            // Calendar (compact or expanded)
            TaskCalendarView(
                isExpanded = expandedSection == ExpandedSection.CALENDAR,
                onExpandToggle = { toggleSection(ExpandedSection.CALENDAR) },
                selectedDate = filterState.selectedDate,
                onDateSelected = { date ->
                    viewModel.onDateSelected(
                        if (date == filterState.selectedDate) null else date
                    )
                },
                datesWithTasks = datesWithTasks
            )

            // Category filter
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

            // Search bar
            AnimatedVisibility(
                visible = expandedSection == ExpandedSection.SEARCH,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                SearchBar(
                    searchQuery = filterState.searchQuery,
                    onSearchQueryChanged = { query -> viewModel.onSearchQueryChanged(query) },
                    onClearSearch = {
                        viewModel.onSearchQueryChanged("")
                        toggleSection(ExpandedSection.SEARCH) // Close search
                    },
                    onSearchSubmit = {
                        keyboardController?.hide()
                    },
                    searchFocusRequester = searchFocusRequester,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Sort menu
            AnimatedVisibility(
                visible = expandedSection == ExpandedSection.SORT,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                SortOptionsMenu(
                    currentSortOption = filterState.sortOption,
                    onSortOptionSelected = {
                        viewModel.onSortOptionSelected(it)
                        expandedSection = null // Close sort menu
                    }
                )
            }

            // Task list or Eisenhower matrix
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
                    onTaskToggle = { taskId ->
                        viewModel.toggleTaskStatus(taskId)
                    },
                    onTaskDelete = { taskId ->
                        viewModel.onDeleteTask(taskId)
                    }
                )
            } else {
                TasksList(
                    tasks = state.tasks,
                    onTaskClick = onTaskClick,
                    onTaskStatusChange = { taskId, isCompleted ->
                        viewModel.onTaskStatusChanged(taskId, isCompleted)
                    },
                    onTaskToggle = { taskId ->  // Add this new parameter
                        viewModel.toggleTaskStatus(taskId)
                    },
                    onTaskDelete = { taskId ->
                        viewModel.onDeleteTask(taskId)
                    },
                    onTaskArchive = { taskId ->
                        viewModel.onArchiveTask(taskId)
                    }
                )
            }
        }
    }
}

// Enum to track which section is expanded
enum class ExpandedSection {
    SEARCH, FILTER, SORT, CALENDAR
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksTopAppBar(
    expandedSection: ExpandedSection?,
    onSectionToggle: (ExpandedSection) -> Unit,
    isFiltersActive: Boolean,
    onResetFilters: () -> Unit,
    showEisenhowerMatrix: Boolean,
    onToggleEisenhowerMatrix: (Boolean) -> Unit,
    filterState: TaskFilterState
) {
    Column {
        // Main top bar
        TopAppBar(
            title = {
                Text(stringResource(R.string.tasks))
            },
            actions = {
                // Search button
                IconButton(onClick = { onSectionToggle(ExpandedSection.SEARCH) }) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = stringResource(R.string.search),
                        tint = if (expandedSection == ExpandedSection.SEARCH)
                            MaterialTheme.colorScheme.primary
                        else
                            LocalContentColor.current
                    )
                }

                // Filter button
                IconButton(onClick = { onSectionToggle(ExpandedSection.FILTER) }) {
                    BadgedBox(
                        badge = {
                            if (isFiltersActive) {
                                Badge { Text("!") }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = stringResource(R.string.filter),
                            tint = if (expandedSection == ExpandedSection.FILTER)
                                MaterialTheme.colorScheme.primary
                            else
                                LocalContentColor.current
                        )
                    }
                }

                // Sort button
                IconButton(onClick = { onSectionToggle(ExpandedSection.SORT) }) {
                    Icon(
                        imageVector = Icons.Default.Sort,
                        contentDescription = stringResource(R.string.sort),
                        tint = if (expandedSection == ExpandedSection.SORT)
                            MaterialTheme.colorScheme.primary
                        else
                            LocalContentColor.current
                    )
                }

                // Eisenhower matrix button
                IconButton(
                    onClick = {
                        onToggleEisenhowerMatrix(!showEisenhowerMatrix)  // Use callback
                    }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.GridView,
                        contentDescription = stringResource(R.string.eisenhower_matrix),
                        tint = if (showEisenhowerMatrix)
                            MaterialTheme.colorScheme.primary
                        else
                            LocalContentColor.current
                    )
                }

                // Reset filters button (only shown when filters are active)
                AnimatedVisibility(visible = isFiltersActive) {
                    IconButton(onClick = onResetFilters) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = stringResource(R.string.reset_filters),
                            tint = MaterialTheme.colorScheme.primary
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
            // Calendar header with expand button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Current or selected date display
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

                // Button to expand/collapse calendar
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

            // Show simplified calendar when expanded
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

// Search Bar component
@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onClearSearch: () -> Unit,
    onSearchSubmit: () -> Unit,
    searchFocusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                    IconButton(onClick = onClearSearch) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.close_search)
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearchSubmit() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(searchFocusRequester),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    unfocusedBorderColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(24.dp)
            )
        }
    }
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
    onTaskToggle: (String) -> Unit,
    onTaskDelete: (String) -> Unit,
    onTaskArchive: (String) -> Unit
) {
    // 1. Group tasks by date
    val today = LocalDate.now()
    val yesterday = today.minusDays(1)

    // Create a map to group tasks by their date
    val groupedTasks = tasks.groupBy { task ->
        task.dueDate?.let { millis ->
            Instant.ofEpochMilli(millis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
        } ?: LocalDate.MAX // Tasks with no due date will be grouped separately
    }.toSortedMap() // Sort the map by date

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // For tasks with no due date - show at the top
        groupedTasks[LocalDate.MAX]?.let { tasksWithNoDate ->
            if (tasksWithNoDate.isNotEmpty()) {
                stickyHeader {
                    DateHeader(
                        date = null,
                        text = stringResource(R.string.no_date)
                    )
                }

                items(
                    items = tasksWithNoDate,
                    key = { it.id }
                ) { task ->
                    SwipeableTaskItem(
                        task = task,
                        onTaskClick = onTaskClick,
                        onTaskCompleteChange = { isCompleted ->
                            onTaskStatusChange(task.id, isCompleted)
                        },
                        onToggleTaskStatus = {
                            onTaskToggle(task.id)
                        },
                        onDeleteTask = { onTaskDelete(task.id) },
                        modifier = Modifier.animateContentSize()
                    )
                }
            }
        }

        // For each date group (excluding MAX)
        groupedTasks.entries
            .filter { it.key != LocalDate.MAX }
            .forEach { (date, tasksForDate) ->
                // Display a sticky header for this date group
                stickyHeader {
                    DateHeader(
                        date = date,
                        text = when {
                            date.isEqual(today) -> stringResource(R.string.today)
                            date.isEqual(yesterday) -> stringResource(R.string.yesterday)
                            date.year == today.year -> date.format(DateTimeFormatter.ofPattern("d MMMM"))
                            else -> date.format(DateTimeFormatter.ofPattern("d MMMM yyyy"))
                        }
                    )
                }

                // Display the tasks for this date
                items(
                    items = tasksForDate,
                    key = { it.id }
                ) { task ->
                    SwipeableTaskItem(
                        task = task,
                        onTaskClick = onTaskClick,
                        onTaskCompleteChange = { isCompleted ->
                            onTaskStatusChange(task.id, isCompleted)
                        },
                        onToggleTaskStatus = {
                            onTaskToggle(task.id)
                        },
                        onDeleteTask = { onTaskDelete(task.id) },
                        modifier = Modifier.animateContentSize()
                    )
                }
            }
    }
}

@Composable
fun DateHeader(date: LocalDate?, text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Calendar icon
            Icon(
                imageVector = Icons.Outlined.Event,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Date text
            Text(
                text = text,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.weight(1f))

            // Count of tasks for this date
            date?.let {
                val count = 0 // You might want to calculate or pass this
                if (count > 0) {
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun EisenhowerMatrix(
    tasks: List<Task>,
    onTaskClick: (String) -> Unit,
    onTaskStatusChange: (String, Boolean) -> Unit,
    onTaskToggle: (String) -> Unit, // Добавляем новый параметр
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
                onTaskToggle = onTaskToggle, // Передаем параметр
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
                onTaskToggle = onTaskToggle, // Передаем параметр
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
                onTaskToggle = onTaskToggle, // Передаем параметр
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
                onTaskToggle = onTaskToggle, // Передаем параметр
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
    onTaskToggle: (String) -> Unit,
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
                            onStatusChange = { isCompleted ->
                                onTaskStatusChange(task.id, isCompleted)
                            },
                            onTaskToggle = {
                                onTaskToggle(task.id)
                            }
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
    onStatusChange: (Boolean) -> Unit,
    onTaskToggle: () -> Unit, // Новый параметр
    modifier: Modifier = Modifier
) {
    // Для обнаружения долгого нажатия
    var isPressed by remember { mutableStateOf(false) }
    val hapticFeedback = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    // Анимация масштабирования
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 1.05f else 1.0f,
        animationSpec = tween(200),
        label = "scaleAnimation"
    )

    // Анимация подъема
    val elevation by animateFloatAsState(
        targetValue = if (isPressed) 4f else 1f,
        animationSpec = tween(200),
        label = "elevationAnimation"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale) // Применяем анимацию масштаба
            .clickable(onClick = onClick)
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        isPressed = true

                        // Используем onTaskToggle вместо onStatusChange
                        onTaskToggle()

                        scope.launch {
                            delay(300) // Увеличиваем задержку для лучшего эффекта
                            isPressed = false
                        }
                    }
                )
            },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = elevation.dp // Применяем анимацию подъема
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
        // Structure to hold information about each row
        data class RowInfo(
            val placeables: List<Placeable>,
            val height: Int
        )

        val rows = mutableListOf<RowInfo>()
        val currentRowPlaceables = mutableListOf<Placeable>()
        var currentRowWidth = 0
        var currentRowHeight = 0
        var totalHeight = 0

        // Measure and organize into rows
        for (measurable in measurables) {
            val placeable = measurable.measure(constraints)

            // Check if adding this item would exceed the max width
            if (currentRowPlaceables.isNotEmpty() &&
                currentRowWidth + mainAxisSpacing.roundToPx() + placeable.width > constraints.maxWidth) {
                // Add the current row and start a new one
                rows.add(RowInfo(currentRowPlaceables.toList(), currentRowHeight))
                totalHeight += currentRowHeight
                if (rows.size > 1) {
                    totalHeight += crossAxisSpacing.roundToPx()
                }

                // Reset for next row
                currentRowPlaceables.clear()
                currentRowWidth = 0
                currentRowHeight = 0
            }

            // Add to current row
            currentRowPlaceables.add(placeable)
            currentRowWidth += placeable.width +
                    if (currentRowPlaceables.size > 1) mainAxisSpacing.roundToPx() else 0
            currentRowHeight = maxOf(currentRowHeight, placeable.height)
        }

        // Add the last row if not empty
        if (currentRowPlaceables.isNotEmpty()) {
            rows.add(RowInfo(currentRowPlaceables.toList(), currentRowHeight))
            totalHeight += currentRowHeight
            if (rows.size > 1) {
                totalHeight += crossAxisSpacing.roundToPx()
            }
        }

        // Layout the children
        layout(constraints.maxWidth, totalHeight) {
            var yPosition = 0

            rows.forEach { row ->
                var xPosition = 0

                row.placeables.forEach { placeable ->
                    placeable.place(x = xPosition, y = yPosition)
                    xPosition += placeable.width + mainAxisSpacing.roundToPx()
                }

                yPosition += row.height + crossAxisSpacing.roundToPx()
            }
        }
    }
}