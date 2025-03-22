package com.example.dhbt.presentation.habit.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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

    // Состояние поиска и сортировки
    var isSearchVisible by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isSortDialogVisible by remember { mutableStateOf(false) }

    // Анимация прогресса
    val animatedProgress by animateFloatAsState(
        targetValue = overallProgress,
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
            if (isSearchVisible) {
                // Поисковая строка с правильными параметрами
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { newQuery -> searchQuery = newQuery },
                    onSearch = { newQuery -> searchQuery = newQuery },
                    active = true,
                    onActiveChange = { isActive ->
                        if (!isActive) isSearchVisible = false
                    },
                    placeholder = { Text(stringResource(R.string.search_habits)) },
                    leadingIcon = {
                        IconButton(onClick = { isSearchVisible = false }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                        }
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Очистить")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Пустое содержимое поискового бара, если нет предложений
                }
            } else {
                // Обычная верхняя панель
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
            }
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

                // Панель фильтров и сортировки
                HabitsFilterBar(
                    statusFilter = filterState.statusFilter,
                    viewMode = uiState.viewMode,
                    categories = categories,
                    selectedCategoryId = filterState.selectedCategoryId,
                    onStatusFilterChanged = { filter ->
                        viewModel.handleIntent(HabitsIntent.SetStatusFilter(filter))
                    },
                    onViewModeChanged = { mode ->
                        viewModel.handleIntent(HabitsIntent.SetViewMode(mode))
                    },
                    onCategorySelected = { categoryId ->
                        viewModel.handleIntent(HabitsIntent.SelectCategory(categoryId))
                    },
                    onSearchClicked = { isSearchVisible = true },
                    onSortClicked = { isSortDialogVisible = true }
                )

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
                            }
                        )
                    }
                }
            }

            // Диалог выбора сортировки
            if (isSortDialogVisible) {
                SortOrderDialog(
                    currentSortOrder = filterState.sortOrder,
                    onSortOrderSelected = { sortOrder ->
                        viewModel.handleIntent(HabitsIntent.SetSortOrder(sortOrder))
                        isSortDialogVisible = false
                    },
                    onDismiss = { isSortDialogVisible = false }
                )
            }
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
        Icon(
            imageVector = Icons.Outlined.NoteAdd,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
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

@Composable
fun SortOption(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun SortOrderDialog(
    currentSortOrder: SortOrder,
    onSortOrderSelected: (SortOrder) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.sort_by)) },
        text = {
            Column {
                SortOption(
                    title = stringResource(R.string.name_asc),
                    selected = currentSortOrder == SortOrder.NAME_ASC,
                    onClick = { onSortOrderSelected(SortOrder.NAME_ASC) }
                )
                SortOption(
                    title = stringResource(R.string.name_desc),
                    selected = currentSortOrder == SortOrder.NAME_DESC,
                    onClick = { onSortOrderSelected(SortOrder.NAME_DESC) }
                )
                SortOption(
                    title = stringResource(R.string.streak_asc),
                    selected = currentSortOrder == SortOrder.STREAK_ASC,
                    onClick = { onSortOrderSelected(SortOrder.STREAK_ASC) }
                )
                SortOption(
                    title = stringResource(R.string.streak_desc),
                    selected = currentSortOrder == SortOrder.STREAK_DESC,
                    onClick = { onSortOrderSelected(SortOrder.STREAK_DESC) }
                )
                SortOption(
                    title = stringResource(R.string.progress_asc),
                    selected = currentSortOrder == SortOrder.PROGRESS_ASC,
                    onClick = { onSortOrderSelected(SortOrder.PROGRESS_ASC) }
                )
                SortOption(
                    title = stringResource(R.string.progress_desc),
                    selected = currentSortOrder == SortOrder.PROGRESS_DESC,
                    onClick = { onSortOrderSelected(SortOrder.PROGRESS_DESC) }
                )
                SortOption(
                    title = stringResource(R.string.creation_asc),
                    selected = currentSortOrder == SortOrder.CREATION_DATE_ASC,
                    onClick = { onSortOrderSelected(SortOrder.CREATION_DATE_ASC) }
                )
                SortOption(
                    title = stringResource(R.string.creation_desc),
                    selected = currentSortOrder == SortOrder.CREATION_DATE_DESC,
                    onClick = { onSortOrderSelected(SortOrder.CREATION_DATE_DESC) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        }
    )
}

@Composable
fun CategoryGroupedHabits(
    habits: List<HabitWithProgress>,
    categories: List<Category>,
    onToggleCompletion: (String) -> Unit,
    onIncrement: (String) -> Unit,
    onDecrement: (String) -> Unit,
    onHabitClick: (String) -> Unit,
    onCreateHabit: () -> Unit
) {
    // Группируем привычки по категориям
    val habitsByCategory = habits.groupBy { it.habit.categoryId }

    // Создаем список категорий и их привычек
    val groupedItems = mutableListOf<Pair<Category?, List<HabitWithProgress>>>()

    // Сначала добавляем привычки без категории
    val habitsWithoutCategory = habitsByCategory[null] ?: emptyList()
    if (habitsWithoutCategory.isNotEmpty()) {
        groupedItems.add(Pair(null, habitsWithoutCategory))
    }

    // Затем добавляем привычки с категориями
    categories.forEach { category ->
        val habitsInCategory = habitsByCategory[category.id] ?: emptyList()
        if (habitsInCategory.isNotEmpty()) {
            groupedItems.add(Pair(category, habitsInCategory))
        }
    }

    if (groupedItems.isEmpty()) {
        EmptyHabitsView(
            filterState = HabitsFilterState(),
            onCreateHabit = onCreateHabit,
            onClearFilters = {}
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 8.dp,
                end = 8.dp,
                top = 4.dp,
                bottom = 80.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            groupedItems.forEach { (category, habitsList) ->
                item(key = category?.id ?: "no-category") {
                    CategorySection(
                        category = category,
                        habits = habitsList,
                        onToggleCompletion = onToggleCompletion,
                        onIncrement = onIncrement,
                        onDecrement = onDecrement,
                        onHabitClick = onHabitClick
                    )
                }
            }
        }
    }
}

@Composable
fun CategorySection(
    category: Category?,
    habits: List<HabitWithProgress>,
    onToggleCompletion: (String) -> Unit,
    onIncrement: (String) -> Unit,
    onDecrement: (String) -> Unit,
    onHabitClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(8.dp)
    ) {
        // Заголовок категории
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (category != null) {
                // Цветной индикатор категории
                val categoryColor = getHabitColor(category.color, MaterialTheme.colorScheme.primary)

                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(categoryColor)
                )

                Spacer(modifier = Modifier.width(8.dp))
            }

            Text(
                text = category?.name ?: stringResource(R.string.uncategorized),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Список привычек в категории
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            habits.forEach { habitWithProgress ->
                HabitListItem(
                    habitWithProgress = habitWithProgress,
                    onToggleCompletion = { onToggleCompletion(habitWithProgress.habit.id) },
                    onIncrement = { onIncrement(habitWithProgress.habit.id) },
                    onDecrement = { onDecrement(habitWithProgress.habit.id) },
                    onClick = { onHabitClick(habitWithProgress.habit.id) },
                    onEdit = { /* Handled in detail screen */ },
                    onArchive = { /* Handled in detail screen */ }
                )
            }
        }
    }
}

/**
 * Безопасно извлекает цвет из строки цветового кода
 */
@Composable
fun getHabitColor(colorString: String?, defaultColor: Color): Color {
    return if (colorString != null) {
        try {
            Color(android.graphics.Color.parseColor(colorString))
        } catch (e: Exception) {
            defaultColor
        }
    } else {
        defaultColor
    }
}