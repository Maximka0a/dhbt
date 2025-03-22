package com.example.dhbt.presentation.habit.list

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.dhbt.R
import com.example.dhbt.domain.model.Category
import com.example.dhbt.presentation.habit.components.*
import com.example.dhbt.presentation.navigation.HabitDetail
import com.example.dhbt.presentation.navigation.HabitEdit
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HabitsScreen(
    navController: NavController,
    viewModel: HabitsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val filterState by viewModel.filterState.collectAsState()
    val habits by viewModel.filteredHabits.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val overallProgress by viewModel.overallProgress.collectAsState()

    // Состояния UI
    var searchQuery by remember { mutableStateOf("") }
    var isSearchBarVisible by remember { mutableStateOf(false) }
    var isSortingListVisible by remember { mutableStateOf(false) }
    var isViewModesVisible by remember { mutableStateOf(false) }
    var areCategoriesVisible by remember { mutableStateOf(true) }

    // Для индикации активных кнопок действий
    var activeAction by remember { mutableStateOf<ActionType?>(null) }

    // Функция для переключения активного действия
    fun toggleAction(action: ActionType) {
        activeAction = if (activeAction == action) null else action

        // Обновляем связанные состояния
        isSearchBarVisible = activeAction == ActionType.SEARCH
        isSortingListVisible = activeAction == ActionType.SORT
        isViewModesVisible = activeAction == ActionType.VIEW_MODE
    }

    // Анимация прогресса
    val animatedProgress by animateFloatAsState(
        targetValue = overallProgress,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "overallProgress"
    )

    // Отслеживаем изменения в запросе поиска
    LaunchedEffect(searchQuery) {
        viewModel.handleIntent(HabitsIntent.SetSearchQuery(searchQuery))
    }

    // TopAppBar scroll behavior
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.habits),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    // Диаграмма общего прогресса
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .size(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 3.dp
                        )

                        Text(
                            text = "${(overallProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(HabitEdit()) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_habit))
            }
        }
    ) { paddingValues ->
        val lazyListState = rememberLazyListState()
        val lazyGridState = rememberLazyGridState()

        Box(modifier = Modifier.padding(paddingValues)) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Календарь для выбора даты
                HabitCalendarView(
                    selectedDate = selectedDate,
                    habits = habits,
                    onDateSelected = { date ->
                        viewModel.handleIntent(HabitsIntent.SetSelectedDate(date))
                    },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )

                // Панель с фильтрами статуса и действиями
                ImprovedFilterStatusBar(
                    statusFilter = filterState.statusFilter,
                    onStatusFilterChanged = { filter ->
                        viewModel.handleIntent(HabitsIntent.SetStatusFilter(filter))
                    },
                    activeAction = activeAction,
                    onActionSelected = { action -> toggleAction(action) },
                    onCategoriesToggle = { areCategoriesVisible = !areCategoriesVisible },
                    areCategoriesVisible = areCategoriesVisible
                )

                // Анимированное поле поиска
                AnimatedVisibility(
                    visible = activeAction == ActionType.SEARCH,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text(stringResource(R.string.search_habits)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Очистить"
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                // Анимированный список выбора режима просмотра
                AnimatedVisibility(
                    visible = activeAction == ActionType.VIEW_MODE,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    ImprovedViewModeSelector(
                        currentMode = uiState.viewMode,
                        onModeSelected = { mode ->
                            viewModel.handleIntent(HabitsIntent.SetViewMode(mode))
                            activeAction = null
                        }
                    )
                }

                // Анимированный список выбора сортировки
                AnimatedVisibility(
                    visible = activeAction == ActionType.SORT,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    SortOrderSelector(
                        currentSortOrder = filterState.sortOrder,
                        onSortSelected = { sortOrder ->
                            viewModel.handleIntent(HabitsIntent.SetSortOrder(sortOrder))
                        }
                    )
                }

                // Категории - показываем, только если включено areCategoriesVisible
                AnimatedVisibility(
                    visible = areCategoriesVisible,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    CategoriesFilter(
                        categories = categories,
                        selectedCategoryId = filterState.selectedCategoryId,
                        onCategorySelected = { categoryId ->
                            viewModel.handleIntent(HabitsIntent.SelectCategory(categoryId))
                        }
                    )
                }

                // Основной контент в зависимости от режима просмотра
                when (uiState.viewMode) {
                    HabitViewMode.LIST -> {
                        // Список привычек
                        if (habits.isEmpty()) {
                            EmptyHabitsView(
                                filterState = filterState,
                                onCreateHabit = { navController.navigate(HabitEdit()) },
                                onClearFilters = { viewModel.handleIntent(HabitsIntent.ClearFilters) }
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                state = lazyListState,
                                contentPadding = PaddingValues(
                                    start = 8.dp,
                                    end = 8.dp,
                                    top = 4.dp,
                                    bottom = 80.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(
                                    items = habits,
                                    key = { it.habit.id }
                                ) { habitWithProgress ->
                                    HabitListItem(
                                        habitWithProgress = habitWithProgress,
                                        onToggleCompletion = {
                                            viewModel.handleIntent(
                                                HabitsIntent.ToggleHabitCompletion(habitWithProgress.habit.id)
                                            )
                                        },
                                        onIncrement = {
                                            viewModel.handleIntent(
                                                HabitsIntent.IncrementHabitProgress(habitWithProgress.habit.id)
                                            )
                                        },
                                        onDecrement = {
                                            viewModel.handleIntent(
                                                HabitsIntent.DecrementHabitProgress(habitWithProgress.habit.id)
                                            )
                                        },
                                        onClick = {
                                            navController.navigate(HabitDetail(habitWithProgress.habit.id))
                                        },
                                        onEdit = {
                                            navController.navigate(HabitEdit(habitWithProgress.habit.id))
                                        },
                                        onArchive = {
                                            viewModel.handleIntent(
                                                HabitsIntent.ArchiveHabit(habitWithProgress.habit.id)
                                            )
                                        },
                                        modifier = Modifier.animateItemPlacement()
                                    )
                                }
                            }
                        }
                    }
                    HabitViewMode.GRID -> {
                        // Сетка привычек
                        if (habits.isEmpty()) {
                            EmptyHabitsView(
                                filterState = filterState,
                                onCreateHabit = { navController.navigate(HabitEdit()) },
                                onClearFilters = { viewModel.handleIntent(HabitsIntent.ClearFilters) }
                            )
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 120.dp),
                                modifier = Modifier.fillMaxSize(),
                                state = lazyGridState,
                                contentPadding = PaddingValues(
                                    start = 8.dp,
                                    end = 8.dp,
                                    top = 4.dp,
                                    bottom = 80.dp
                                ),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(
                                    items = habits,
                                    key = { it.habit.id }
                                ) { habitWithProgress ->
                                    HabitGridItem(
                                        habitWithProgress = habitWithProgress,
                                        onToggleCompletion = {
                                            viewModel.handleIntent(
                                                HabitsIntent.ToggleHabitCompletion(habitWithProgress.habit.id)
                                            )
                                        },
                                        onIncrement = {
                                            viewModel.handleIntent(
                                                HabitsIntent.IncrementHabitProgress(habitWithProgress.habit.id)
                                            )
                                        },
                                        onClick = {
                                            navController.navigate(HabitDetail(habitWithProgress.habit.id))
                                        }
                                    )
                                }
                            }
                        }
                    }
                    HabitViewMode.CATEGORIES -> {
                        // Группировка привычек по категориям
                        CategoryGroupedHabits(
                            habits = habits,
                            categories = categories,
                            filterState = filterState,
                            onToggleCompletion = { habitId ->
                                viewModel.handleIntent(HabitsIntent.ToggleHabitCompletion(habitId))
                            },
                            onIncrement = { habitId ->
                                viewModel.handleIntent(HabitsIntent.IncrementHabitProgress(habitId))
                            },
                            onDecrement = { habitId ->
                                viewModel.handleIntent(HabitsIntent.DecrementHabitProgress(habitId))
                            },
                            onHabitClick = { habitId ->
                                navController.navigate(HabitDetail(habitId))
                            },
                            onCreateHabit = {
                                navController.navigate(HabitEdit())
                            },
                            onClearFilters = {
                                viewModel.handleIntent(HabitsIntent.ClearFilters)
                            }
                        )
                    }
                }
            }
        }
    }
}

// Тип действия для индикации активного состояния
enum class ActionType {
    SEARCH, SORT, VIEW_MODE
}

@Composable
fun ImprovedFilterStatusBar(
    statusFilter: HabitStatusFilter,
    onStatusFilterChanged: (HabitStatusFilter) -> Unit,
    activeAction: ActionType?,
    onActionSelected: (ActionType) -> Unit,
    onCategoriesToggle: () -> Unit,
    areCategoriesVisible: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Верхняя строка с фильтрами и кнопками
            Column {
                // Улучшенная строка фильтров статуса (с вертикальной компоновкой при необходимости)
                ImprovedFilterChips(
                    selectedFilter = statusFilter,
                    onFilterSelected = onStatusFilterChanged
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Кнопки действий с индикацией активного состояния
                ActionButtons(
                    activeAction = activeAction,
                    onActionSelected = onActionSelected,
                    onCategoriesToggle = onCategoriesToggle,
                    areCategoriesVisible = areCategoriesVisible
                )
            }
        }
    }
}

@Composable
fun ImprovedFilterChips(
    selectedFilter: HabitStatusFilter,
    onFilterSelected: (HabitStatusFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Создаем более компактные фильтры-чипы
        StatusChip(
            filter = HabitStatusFilter.ACTIVE,
            selected = selectedFilter == HabitStatusFilter.ACTIVE,
            onSelected = onFilterSelected
        )

        StatusChip(
            filter = HabitStatusFilter.ARCHIVED,
            selected = selectedFilter == HabitStatusFilter.ARCHIVED,
            onSelected = onFilterSelected
        )

        StatusChip(
            filter = HabitStatusFilter.ALL,
            selected = selectedFilter == HabitStatusFilter.ALL,
            onSelected = onFilterSelected
        )
    }
}

@Composable
fun StatusChip(
    filter: HabitStatusFilter,
    selected: Boolean,
    onSelected: (HabitStatusFilter) -> Unit
) {
    val label = when(filter) {
        HabitStatusFilter.ACTIVE -> stringResource(R.string.active)
        HabitStatusFilter.ARCHIVED -> stringResource(R.string.archived)
        HabitStatusFilter.ALL -> stringResource(R.string.all)
    }

    // Более компактный дизайн с вертикальной компоновкой при необходимости
    Surface(
        shape = RoundedCornerShape(100),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        ),
        modifier = Modifier.height(36.dp)
    ) {
        Box(
            modifier = Modifier
                .clickable { onSelected(filter) }
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            // Для длинных слов используем меньший размер шрифта
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = if (label.length > 7) 12.sp else 14.sp
                ),
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ActionButtons(
    activeAction: ActionType?,
    onActionSelected: (ActionType) -> Unit,
    onCategoriesToggle: () -> Unit,
    areCategoriesVisible: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // Кнопка поиска
        ActionButton(
            icon = Icons.Default.Search,
            contentDescription = "Поиск",
            isActive = activeAction == ActionType.SEARCH,
            onClick = { onActionSelected(ActionType.SEARCH) }
        )

        // Кнопка сортировки
        ActionButton(
            icon = Icons.Default.Sort,
            contentDescription = "Сортировка",
            isActive = activeAction == ActionType.SORT,
            onClick = { onActionSelected(ActionType.SORT) }
        )

        // Кнопка режима отображения
        ActionButton(
            icon = Icons.Default.ViewModule,
            contentDescription = "Режим отображения",
            isActive = activeAction == ActionType.VIEW_MODE,
            onClick = { onActionSelected(ActionType.VIEW_MODE) }
        )

        // Кнопка переключения видимости категорий
        ActionButton(
            icon = if (areCategoriesVisible) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = if (areCategoriesVisible) "Скрыть категории" else "Показать категории",
            isActive = false, // Эта кнопка не имеет активного состояния
            onClick = onCategoriesToggle
        )
    }
}

@Composable
fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = if (isActive) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ImprovedViewModeSelector(
    currentMode: HabitViewMode,
    onModeSelected: (HabitViewMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val modes = listOf(
        Triple(HabitViewMode.LIST, stringResource(R.string.view_list), Icons.Default.ViewList),
        Triple(HabitViewMode.GRID, stringResource(R.string.view_grid), Icons.Default.GridView),
        Triple(HabitViewMode.CATEGORIES, stringResource(R.string.view_categories), Icons.Default.Folder)
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Выбор режима отображения",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                modes.forEach { (mode, label, icon) ->
                    val selected = mode == currentMode
                    ImprovedViewModeOption(
                        mode = mode,
                        label = label,
                        icon = icon,
                        selected = selected,
                        onSelected = { onModeSelected(mode) }
                    )
                }
            }
        }
    }
}

@Composable
fun ImprovedViewModeOption(
    mode: HabitViewMode,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onSelected: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .clickable(onClick = onSelected)
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
            border = BorderStroke(
                width = 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            ),
            modifier = Modifier.size(56.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
            fontSize = 11.sp,
            modifier = Modifier.width(60.dp)
        )
    }
}

// Остальные функции без изменений...
@Composable
fun FilterStatusBar(
    statusFilter: HabitStatusFilter,
    onStatusFilterChanged: (HabitStatusFilter) -> Unit,
    onSearchClicked: () -> Unit,
    onSortClicked: () -> Unit,
    onViewModeClicked: () -> Unit,
    onCategoriesToggle: () -> Unit,
    areCategoriesVisible: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Верхняя строка с фильтрами и кнопками
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Фильтры по статусу
                FilterChips(
                    selectedFilter = statusFilter,
                    onFilterSelected = onStatusFilterChanged,
                    modifier = Modifier.weight(1f)
                )

                // Кнопки действий
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Кнопка поиска
                    IconButton(onClick = onSearchClicked) {
                        Icon(Icons.Default.Search, contentDescription = "Поиск")
                    }

                    // Кнопка сортировки
                    IconButton(onClick = onSortClicked) {
                        Icon(Icons.Default.Sort, contentDescription = "Сортировка")
                    }

                    // Кнопка смены режима отображения
                    IconButton(onClick = onViewModeClicked) {
                        Icon(Icons.Default.GridView, contentDescription = "Режим отображения")
                    }

                    // Кнопка переключения видимости категорий
                    IconButton(onClick = onCategoriesToggle) {
                        Icon(
                            imageVector = if (areCategoriesVisible) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (areCategoriesVisible) "Скрыть категории" else "Показать категории"
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FilterChips(
    selectedFilter: HabitStatusFilter,
    onFilterSelected: (HabitStatusFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Список фильтров с более короткими подписями для адаптивности
        val filters = listOf(
            HabitStatusFilter.ACTIVE to stringResource(R.string.active),
            HabitStatusFilter.ARCHIVED to stringResource(R.string.archived),
            HabitStatusFilter.ALL to stringResource(R.string.all)
        )

        filters.forEach { (filter, label) ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = {
                    Text(
                        text = label,
                        // Для длинных слов используем меньший размер шрифта
                        fontSize = if (label.length > 7) 12.sp else 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = when(filter) {
                    HabitStatusFilter.ACTIVE -> {
                        { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    }
                    HabitStatusFilter.ARCHIVED -> {
                        { Icon(Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    }
                    HabitStatusFilter.ALL -> {
                        { Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    }
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selectedFilter == filter,
                    borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    selectedBorderColor = Color.Transparent
                ),
                modifier = Modifier.height(32.dp)
            )
        }
    }
}

@Composable
fun ViewModeSelector(
    currentMode: HabitViewMode,
    onModeSelected: (HabitViewMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val modes = listOf(
        HabitViewMode.LIST to stringResource(R.string.view_list),
        HabitViewMode.GRID to stringResource(R.string.view_grid),
        HabitViewMode.CATEGORIES to stringResource(R.string.view_categories)
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            modes.forEach { (mode, label) ->
                val selected = mode == currentMode
                ViewModeOption(
                    mode = mode,
                    label = label,
                    selected = selected,
                    onSelected = { onModeSelected(mode) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun ViewModeOption(
    mode: HabitViewMode,
    label: String,
    selected: Boolean,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icon = when (mode) {
        HabitViewMode.LIST -> Icons.Default.ViewList
        HabitViewMode.GRID -> Icons.Default.GridView
        HabitViewMode.CATEGORIES -> Icons.Default.Category
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        modifier = modifier
            .padding(horizontal = 4.dp)
            .clickable(onClick = onSelected)
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SortOrderSelector(
    currentSortOrder: SortOrder,
    onSortSelected: (SortOrder) -> Unit,
    modifier: Modifier = Modifier
) {
    val sortGroups = listOf(
        "По имени" to listOf(
            SortOrder.NAME_ASC to stringResource(R.string.name_asc),
            SortOrder.NAME_DESC to stringResource(R.string.name_desc)
        ),
        "По серии" to listOf(
            SortOrder.STREAK_DESC to stringResource(R.string.streak_desc),
            SortOrder.STREAK_ASC to stringResource(R.string.streak_asc)
        ),
        "По прогрессу" to listOf(
            SortOrder.PROGRESS_DESC to stringResource(R.string.progress_desc),
            SortOrder.PROGRESS_ASC to stringResource(R.string.progress_asc)
        ),
        "По дате создания" to listOf(
            SortOrder.CREATION_DATE_DESC to stringResource(R.string.creation_desc),
            SortOrder.CREATION_DATE_ASC to stringResource(R.string.creation_asc)
        )
    )

    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        sortGroups.forEach { (groupName, sortOptions) ->
            item {
                SortOptionGroup(
                    groupName = groupName,
                    options = sortOptions,
                    currentSortOrder = currentSortOrder,
                    onSortSelected = onSortSelected
                )
            }
        }
    }
}

@Composable
fun SortOptionGroup(
    groupName: String,
    options: List<Pair<SortOrder, String>>,
    currentSortOrder: SortOrder,
    onSortSelected: (SortOrder) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                text = groupName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            options.forEach { (sortOrder, label) ->
                val isSelected = sortOrder == currentSortOrder
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSortSelected(sortOrder) }
                        .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = { onSortSelected(sortOrder) },
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun CategoriesFilter(
    categories: List<Category>,
    selectedCategoryId: String?,
    onCategorySelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        // "Все категории" чип
        item {
            FilterChip(
                selected = selectedCategoryId == null,
                onClick = { onCategorySelected(null) },
                label = { Text(stringResource(R.string.all)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier.padding(end = 8.dp)
            )
        }

        // Категории
        items(categories) { category ->
            val color = try {
                category.color?.let { Color(android.graphics.Color.parseColor(it)) }
            } catch (e: Exception) {
                null
            }

            FilterChip(
                selected = selectedCategoryId == category.id,
                onClick = { onCategorySelected(category.id) },
                label = { Text(category.name) },
                leadingIcon = {
                    if (color != null) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                    }
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = color?.copy(alpha = 0.2f)
                        ?: MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier.padding(end = 8.dp)
            )
        }
    }
}

@Composable
fun EmptyHabitsView(
    filterState: HabitsFilterState,
    onCreateHabit: () -> Unit,
    onClearFilters: () -> Unit
) {
    // Отображение когда нет привычек или они были отфильтрованы
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Анимированная иконка
        val infiniteTransition = rememberInfiniteTransition(label = "empty_animation")
        val iconSize by infiniteTransition.animateFloat(
            initialValue = 0.9f,
            targetValue = 1.1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = EaseInOutQuart),
                repeatMode = RepeatMode.Reverse
            ),
            label = "icon_size"
        )

        Icon(
            imageVector = Icons.Outlined.NoteAdd,
            contentDescription = null,
            modifier = Modifier
                .size(80.dp)
                .graphicsLayer {
                    scaleX = iconSize
                    scaleY = iconSize
                },
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (filterState.searchQuery.isNotEmpty() || filterState.selectedCategoryId != null)
                stringResource(R.string.no_habits_found)
            else if (filterState.statusFilter == HabitStatusFilter.ARCHIVED)
                stringResource(R.string.no_archived_habits)
            else
                stringResource(R.string.no_habits_yet),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (filterState.searchQuery.isNotEmpty() || filterState.selectedCategoryId != null)
                stringResource(R.string.try_different_search)
            else if (filterState.statusFilter == HabitStatusFilter.ARCHIVED)
                stringResource(R.string.archive_habits_tip)
            else
                stringResource(R.string.create_habit_tip),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (filterState.searchQuery.isNotEmpty() || filterState.selectedCategoryId != null) {
            Button(
                onClick = onClearFilters,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text(stringResource(R.string.clear_filters))
            }
        } else if (filterState.statusFilter != HabitStatusFilter.ARCHIVED) {
            Button(
                onClick = onCreateHabit,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(stringResource(R.string.create_first_habit))
            }
        }
    }
}

// Easing для анимаций
private val EaseInOutQuart = CubicBezierEasing(0.77f, 0f, 0.175f, 1f)