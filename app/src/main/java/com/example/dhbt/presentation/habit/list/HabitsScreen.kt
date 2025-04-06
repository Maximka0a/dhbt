@file:OptIn(ExperimentalLayoutApi::class)

package com.example.dhbt.presentation.habit.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FilterListOff
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.SentimentDissatisfied
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.dhbt.R
import com.example.dhbt.domain.model.Category
import com.example.dhbt.domain.model.Habit
import com.example.dhbt.domain.model.HabitType
import com.example.dhbt.presentation.navigation.HabitDetail
import com.example.dhbt.presentation.navigation.HabitEdit
import com.example.dhbt.presentation.util.DayOfWeekLocalization
import com.example.dhbt.presentation.util.MonthLocalization
import com.example.dhbt.presentation.util.parseColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Month

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class,
    ExperimentalAnimationApi::class)
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
    val weekStartDate by viewModel.weekStartDate.collectAsState()

    // State for UI interactions
    var expandedSection by remember { mutableStateOf<ExpandableSection?>(null) }
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var areCategoriesVisible by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    // Animation values - use derivedStateOf to avoid unnecessary animations
    val animatedProgress by animateFloatAsState(
        targetValue = overallProgress,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "progressAnimation"
    )

    // Update search query
    LaunchedEffect(searchQuery) {
        if (isSearchActive) {
            viewModel.handleIntent(HabitsIntent.SetSearchQuery(searchQuery))
        }
    }

    // Scroll behavior for the top app bar
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                LargeTopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.habits),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            // Progress indicator
                            ProgressIndicator(
                                progress = animatedProgress,
                                size = 38.dp
                            )
                        }
                    },
                    actions = {
                        // Search button
                        IconButton(
                            onClick = {
                                isSearchActive = !isSearchActive
                                if (!isSearchActive) {
                                    searchQuery = ""
                                    viewModel.handleIntent(HabitsIntent.SetSearchQuery(""))
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (isSearchActive) Icons.Filled.Close else Icons.Filled.Search,
                                contentDescription = stringResource(R.string.search),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Filter button
                        IconButton(
                            onClick = {
                                expandedSection = if (expandedSection == ExpandableSection.FILTERS)
                                    null else ExpandableSection.FILTERS
                            }
                        ) {
                            Badge(
                                modifier = Modifier.offset(x = 14.dp, y = (-10).dp),
                                containerColor = MaterialTheme.colorScheme.tertiary,
                                contentColor = MaterialTheme.colorScheme.onTertiary
                            ) {
                                val filterCount = countActiveFilters(filterState)
                                if (filterCount > 0) Text(filterCount.toString())
                            }

                            Icon(
                                imageVector = Icons.Filled.FilterList,
                                contentDescription = "Filters",
                                tint = if (expandedSection == ExpandableSection.FILTERS)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // View mode button
                        IconButton(
                            onClick = {
                                expandedSection = if (expandedSection == ExpandableSection.VIEW_MODES)
                                    null else ExpandableSection.VIEW_MODES
                            }
                        ) {
                            Icon(
                                imageVector = when (uiState.viewMode) {
                                    HabitViewMode.LIST -> Icons.Filled.ViewList
                                    HabitViewMode.GRID -> Icons.Filled.GridView
                                    HabitViewMode.CATEGORIES -> Icons.Filled.Category
                                },
                                contentDescription = stringResource(R.string.view_mode),
                                tint = if (expandedSection == ExpandableSection.VIEW_MODES)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                    )
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { navController.navigate(HabitEdit()) },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null
                        )
                    },
                    text = {
                        Text(
                            text = stringResource(R.string.new_habit),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    expanded = true,
                    elevation = FloatingActionButtonDefaults.elevation(8.dp)
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Search bar
                    AnimatedVisibility(
                        visible = isSearchActive,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        SearchBar(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            onClear = {
                                searchQuery = ""
                                viewModel.handleIntent(HabitsIntent.SetSearchQuery(""))
                            },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    // Expandable filter section
                    AnimatedVisibility(
                        visible = expandedSection == ExpandableSection.FILTERS,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        FilterPanel(
                            filterState = filterState,
                            onStatusFilterChanged = { filter ->
                                viewModel.handleIntent(HabitsIntent.SetStatusFilter(filter))
                            },
                            onSortOrderChanged = { sortOrder ->
                                viewModel.handleIntent(HabitsIntent.SetSortOrder(sortOrder))
                            },
                            onClearFilters = {
                                viewModel.handleIntent(HabitsIntent.ClearFilters)
                                expandedSection = null
                            },
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                        )
                    }

                    // Expandable view mode section
                    AnimatedVisibility(
                        visible = expandedSection == ExpandableSection.VIEW_MODES,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        ViewModePanel(
                            currentMode = uiState.viewMode,
                            onModeSelected = { mode ->
                                viewModel.handleIntent(HabitsIntent.SetViewMode(mode))
                                expandedSection = null
                                scope.launch {
                                    delay(300) // Small delay for better UX
                                }
                            },
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                        )
                    }

                    // Date selector - enhanced with week navigation
                    EnhancedDateSelector(
                        selectedDate = selectedDate,
                        weekStartDate = weekStartDate,
                        onDateSelected = { date ->
                            viewModel.handleIntent(HabitsIntent.SetSelectedDate(date))
                        },
                        onPreviousWeek = {
                            viewModel.handleIntent(HabitsIntent.NavigateToPreviousWeek)
                        },
                        onNextWeek = {
                            viewModel.handleIntent(HabitsIntent.NavigateToNextWeek)
                        },
                        onJumpToToday = {
                            viewModel.handleIntent(HabitsIntent.JumpToCurrentWeek)
                        },
                        habits = habits,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    // Category tabs - only if there are categories and the feature is enabled
                    if (categories.isNotEmpty() && areCategoriesVisible) {
                        CategoryTabs(
                            categories = categories,
                            selectedCategoryId = filterState.selectedCategoryId,
                            onCategorySelected = { categoryId ->
                                viewModel.handleIntent(HabitsIntent.SelectCategory(categoryId))
                            },
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    // Main content area
                    Box(modifier = Modifier.weight(1f)) {
                        // Show appropriate content based on view mode
                        when {
                            habits.isEmpty() -> {
                                EmptyStateView(
                                    filterState = filterState,
                                    onCreateHabit = { navController.navigate(HabitEdit()) },
                                    onClearFilters = {
                                        viewModel.handleIntent(HabitsIntent.ClearFilters)
                                        searchQuery = ""
                                    }
                                )
                            }
                            uiState.viewMode == HabitViewMode.LIST -> {
                                OptimizedHabitListView(
                                    habits = habits,
                                    onHabitClick = { habitId ->
                                        navController.navigate(HabitDetail(habitId))
                                    },
                                    onToggleCompletion = { habitId ->
                                        viewModel.handleIntent(HabitsIntent.ToggleHabitCompletion(habitId))
                                    },
                                    onIncrement = { habitId ->
                                        viewModel.handleIntent(HabitsIntent.IncrementHabitProgress(habitId))
                                    },
                                    onDecrement = { habitId ->
                                        viewModel.handleIntent(HabitsIntent.DecrementHabitProgress(habitId))
                                    }
                                )
                            }
                            uiState.viewMode == HabitViewMode.GRID -> {
                                OptimizedHabitGridView(
                                    habits = habits,
                                    onHabitClick = { habitId ->
                                        navController.navigate(HabitDetail(habitId))
                                    },
                                    onToggleCompletion = { habitId ->
                                        viewModel.handleIntent(HabitsIntent.ToggleHabitCompletion(habitId))
                                    }
                                )
                            }
                            uiState.viewMode == HabitViewMode.CATEGORIES -> {
                                OptimizedHabitCategoriesView(
                                    habits = habits,
                                    categories = categories,
                                    onHabitClick = { habitId ->
                                        navController.navigate(HabitDetail(habitId))
                                    },
                                    onToggleCompletion = { habitId ->
                                        viewModel.handleIntent(HabitsIntent.ToggleHabitCompletion(habitId))
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun OptimizedHabitListView(
    habits: List<HabitWithProgress>,
    onHabitClick: (String) -> Unit,
    onToggleCompletion: (String) -> Unit,
    onIncrement: (String) -> Unit,
    onDecrement: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val defaultColor = MaterialTheme.colorScheme.primary
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxSize()
    ) {
        items(
            items = habits,
            key = { it.habit.id } // Use the habit ID as key for stable item identity
        ) { habitWithProgress ->
            // Using key to avoid unnecessary recompositions
            key(habitWithProgress.habit.id) {
                val habit = habitWithProgress.habit
                val progress = habitWithProgress.currentProgress

                // Only calculate color once per item
                val habitColor = remember(habit.id, habit.color) {
                    parseColor(habit.color, defaultColor)
                }

                ModernHabitListItem(
                    habit = habit,
                    progress = progress,
                    color = habitColor,
                    onToggleCompletion = { onToggleCompletion(habit.id) },
                    onIncrement = { onIncrement(habit.id) },
                    onDecrement = { onDecrement(habit.id) },
                    onClick = { onHabitClick(habit.id) },
                    modifier = Modifier.animateItemPlacement()
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun OptimizedHabitGridView(
    habits: List<HabitWithProgress>,
    onHabitClick: (String) -> Unit,
    onToggleCompletion: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val gridState = rememberLazyGridState()
    val defaultColor =MaterialTheme.colorScheme.primary
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        state = gridState,
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxSize()
    ) {
        items(
            items = habits,
            key = { it.habit.id }
        ) { habitWithProgress ->
            // Using key to avoid unnecessary recompositions
            key(habitWithProgress.habit.id) {
                val habit = habitWithProgress.habit
                val progress = habitWithProgress.currentProgress

                // Only calculate color once per item
                val habitColor = remember(habit.id, habit.color) {
                    parseColor(habit.color, defaultColor)
                }

                ModernHabitGridItem(
                    habit = habit,
                    progress = progress,
                    color = habitColor,
                    onToggleCompletion = { onToggleCompletion(habit.id) },
                    onClick = { onHabitClick(habit.id) },
                    modifier = Modifier.animateItemPlacement()
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun OptimizedHabitCategoriesView(
    habits: List<HabitWithProgress>,
    categories: List<Category>,
    onHabitClick: (String) -> Unit,
    onToggleCompletion: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val defaultColor = MaterialTheme.colorScheme.primary
    val uncategorizedText = stringResource(R.string.uncategorized)

    // Group habits by category - using derivedStateOf to prevent recomposition when not needed
    val habitsByCategory = remember(habits, categories) {
        val result = mutableMapOf<String?, MutableList<HabitWithProgress>>()
        // Initialize with all categories (even empty ones)
        result[null] = mutableListOf() // For habits without category
        categories.forEach { category ->
            result[category.id] = mutableListOf()
        }

        // Fill with habits
        habits.forEach { habitWithProgress ->
            val categoryId = habitWithProgress.habit.categoryId
            result[categoryId]?.add(habitWithProgress)
        }

        // Sort by category name and filter out empty categories
        result.filterValues { it.isNotEmpty() }
            .toSortedMap(compareBy { categoryId ->
                if (categoryId == null) ""
                else categories.find { it.id == categoryId }?.name ?: ""
            })
    }

    // Create map of category colors for quick lookup
    val categoryColorMap = remember(categories) {
        categories.associateBy(
            { it.id },
            { parseColor(it.color, defaultColor) }
        )
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp
        ),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = modifier.fillMaxSize()
    ) {
        habitsByCategory.forEach { (categoryId, habitsInCategory) ->
            if (habitsInCategory.isNotEmpty()) {
                // Get category information
                val category = categories.find { it.id == categoryId }
                val categoryName = category?.name ?: uncategorizedText
                val categoryColor = categoryColorMap[categoryId] ?: defaultColor

                item(key = "category_header_$categoryId") {
                    // Category header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .animateItemPlacement()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(categoryColor)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = categoryName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Divider(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Text(
                            text = "${habitsInCategory.size}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                // Habits in this category
                items(
                    items = habitsInCategory,
                    key = { "habit_${it.habit.id}" }
                ) { habitWithProgress ->
                    key(habitWithProgress.habit.id) {
                        val habit = habitWithProgress.habit
                        val progress = habitWithProgress.currentProgress

                        // Only calculate color once per item
                        val habitColor = remember(habit.id, habit.color) {
                            parseColor(habit.color, defaultColor)
                        }

                        ModernHabitListItem(
                            habit = habit,
                            progress = progress,
                            color = habitColor,
                            onToggleCompletion = { onToggleCompletion(habit.id) },
                            onIncrement = { /* Not needed in this view */ },
                            onDecrement = { /* Not needed in this view */ },
                            onClick = { onHabitClick(habit.id) },
                            showControls = false,
                            modifier = Modifier
                                .animateItemPlacement()
                                .padding(start = 20.dp) // Indentation to show hierarchy
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun ModernHabitListItem(
    habit: Habit,
    progress: Float,
    color: Color,
    onToggleCompletion: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onClick: () -> Unit,
    showControls: Boolean = true,
    modifier: Modifier = Modifier
) {
    // Animation for completion state
    val completionAlpha by animateFloatAsState(
        targetValue = if (progress >= 1f) 1f else 0f,
        label = "completionAlpha"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Completion overlay
            if (completionAlpha > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = color.copy(alpha = 0.1f * completionAlpha),
                            shape = RoundedCornerShape(16.dp)
                        )
                )
            }

            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Emoji and color circle
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = color.copy(alpha = 0.15f),
                            shape = CircleShape
                        )
                        .border(
                            width = 1.dp,
                            color = color.copy(alpha = 0.3f),
                            shape = CircleShape
                        )
                ) {
                    Text(
                        text = habit.iconEmoji ?: "📝",
                        fontSize = 20.sp
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Habit details
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    // Title
                    Text(
                        text = habit.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Progress bar and streaks
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {

                        Column {
                            // Progress bar
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = color,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))

                        // Current streak
                        if (habit.currentStreak > 0) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = color,
                                    modifier = Modifier.size(16.dp)
                                )

                                Spacer(modifier = Modifier.width(2.dp))

                                Text(
                                    text = habit.currentStreak.toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = color
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Binary habit: simple toggle button
                if (habit.type == HabitType.BINARY) {
                    IconButton(
                        onClick = onToggleCompletion,
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                color = if (progress >= 1f)
                                    color.copy(alpha = 0.15f)
                                else
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = if (progress >= 1f)
                                Icons.Filled.CheckCircle
                            else
                                Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = if (progress >= 1f) color else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                // Measurable habit: controls for incrementing/decrementing
                else if (showControls) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Decrement button
                        IconButton(
                            onClick = onDecrement,
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Remove,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Increment button
                        IconButton(
                            onClick = onIncrement,
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    color = color.copy(alpha = 0.15f),
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = null,
                                tint = color,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModernHabitGridItem(
    habit: Habit,
    progress: Float,
    color: Color,
    onToggleCompletion: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Animation for completion
    val completionAlpha by animateFloatAsState(
        targetValue = if (progress >= 1f) 1f else 0f,
        label = "completionAlpha"
    )

    Card(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Completion overlay
            if (completionAlpha > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = color.copy(alpha = 0.1f * completionAlpha),
                            shape = RoundedCornerShape(16.dp)
                        )
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                // Emoji
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            color = color.copy(alpha = 0.15f),
                            shape = CircleShape
                        )
                        .border(
                            width = 1.dp,
                            color = color.copy(alpha = 0.3f),
                            shape = CircleShape
                        )
                ) {
                    Text(
                        text = habit.iconEmoji ?: "📝",
                        fontSize = 24.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Title
                Text(
                    text = habit.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // Streaks (if any)
                if (habit.currentStreak > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocalFireDepartment,
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(14.dp)
                        )

                        Spacer(modifier = Modifier.width(2.dp))

                        Text(
                            text = habit.currentStreak.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = color
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Progress bar
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = color,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Completion toggle
                IconButton(
                    onClick = onToggleCompletion,
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            color = if (progress >= 1f)
                                color.copy(alpha = 0.15f)
                            else
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = if (progress >= 1f)
                            Icons.Filled.CheckCircle
                        else
                            Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = if (progress >= 1f) color else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}


@Composable
private fun ProgressIndicator(
    progress: Float,
    size: Dp = 40.dp,
    strokeWidth: Dp = 3.dp,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size)
    ) {
        // Background circle
        CircularProgressIndicator(
            progress = { 1f },
            color = MaterialTheme.colorScheme.surfaceVariant,
            strokeWidth = strokeWidth,
            modifier = Modifier.size(size)
        )

        // Foreground progress
        CircularProgressIndicator(
            progress = { progress },
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = strokeWidth,
            modifier = Modifier.size(size)
        )

        // Percentage text
        Text(
            text = "${(progress * 100).toInt()}%",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = {
            Text(
                text = stringResource(R.string.search_habits),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Clear",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        shape = RoundedCornerShape(24.dp),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun FilterPanel(
    filterState: HabitsFilterState,
    onStatusFilterChanged: (HabitStatusFilter) -> Unit,
    onSortOrderChanged: (SortOrder) -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp)
    ) {
        // Header with title and clear button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.filters),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            TextButton(
                onClick = onClearFilters,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = stringResource(R.string.clear_all),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Status filters
        Text(
            text = stringResource(R.string.status),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Status filter chips in a Flow layout
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatusFilterChip(
                status = HabitStatusFilter.ACTIVE,
                selected = filterState.statusFilter == HabitStatusFilter.ACTIVE,
                onSelected = onStatusFilterChanged
            )

            StatusFilterChip(
                status = HabitStatusFilter.ARCHIVED,
                selected = filterState.statusFilter == HabitStatusFilter.ARCHIVED,
                onSelected = onStatusFilterChanged
            )

            StatusFilterChip(
                status = HabitStatusFilter.ALL,
                selected = filterState.statusFilter == HabitStatusFilter.ALL,
                onSelected = onStatusFilterChanged
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sort order
        Text(
            text = stringResource(R.string.sort_by),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Sort options in a scrollable row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(end = 8.dp)
        ) {
            item {
                SortOptionChip(
                    order = SortOrder.NAME_ASC,
                    selected = filterState.sortOrder == SortOrder.NAME_ASC,
                    onSelected = onSortOrderChanged
                )
            }

            item {
                SortOptionChip(
                    order = SortOrder.NAME_DESC,
                    selected = filterState.sortOrder == SortOrder.NAME_DESC,
                    onSelected = onSortOrderChanged
                )
            }

            item {
                SortOptionChip(
                    order = SortOrder.STREAK_DESC,
                    selected = filterState.sortOrder == SortOrder.STREAK_DESC,
                    onSelected = onSortOrderChanged
                )
            }

            item {
                SortOptionChip(
                    order = SortOrder.PROGRESS_DESC,
                    selected = filterState.sortOrder == SortOrder.PROGRESS_DESC,
                    onSelected = onSortOrderChanged
                )
            }

            item {
                SortOptionChip(
                    order = SortOrder.CREATION_DATE_DESC,
                    selected = filterState.sortOrder == SortOrder.CREATION_DATE_DESC,
                    onSelected = onSortOrderChanged
                )
            }

            item {
                SortOptionChip(
                    order = SortOrder.CREATION_DATE_ASC,
                    selected = filterState.sortOrder == SortOrder.CREATION_DATE_ASC,
                    onSelected = onSortOrderChanged
                )
            }
        }
    }
}

@Composable
private fun StatusFilterChip(
    status: HabitStatusFilter,
    selected: Boolean,
    onSelected: (HabitStatusFilter) -> Unit
) {
    val (icon, label) = when (status) {
        HabitStatusFilter.ACTIVE -> Pair(
            Icons.Filled.CheckCircle,
            stringResource(R.string.active)
        )
        HabitStatusFilter.ARCHIVED -> Pair(
            Icons.Filled.Archive,
            stringResource(R.string.archived)
        )
        HabitStatusFilter.ALL -> Pair(
            Icons.Filled.List,
            stringResource(R.string.all)
        )
    }

    FilterChip(
        selected = selected,
        onClick = { onSelected(status) },
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium
            )
        },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

@Composable
private fun SortOptionChip(
    order: SortOrder,
    selected: Boolean,
    onSelected: (SortOrder) -> Unit
) {
    val (icon, label) = when (order) {
        SortOrder.NAME_ASC -> Pair(
            Icons.Filled.SortByAlpha,
            stringResource(R.string.name_asc)
        )
        SortOrder.NAME_DESC -> Pair(
            Icons.Filled.SortByAlpha,
            stringResource(R.string.name_desc)
        )
        SortOrder.STREAK_DESC -> Pair(
            Icons.Filled.Whatshot,
            stringResource(R.string.streak_desc)
        )
        SortOrder.STREAK_ASC -> Pair(
            Icons.Filled.Whatshot,
            stringResource(R.string.streak_asc)
        )
        SortOrder.PROGRESS_DESC -> Pair(
            Icons.Filled.TrendingUp,
            stringResource(R.string.progress_desc)
        )
        SortOrder.PROGRESS_ASC -> Pair(
            Icons.Filled.TrendingDown,
            stringResource(R.string.progress_asc)
        )
        SortOrder.CREATION_DATE_DESC -> Pair(
            Icons.Filled.DateRange,
            stringResource(R.string.creation_desc)
        )
        SortOrder.CREATION_DATE_ASC -> Pair(
            Icons.Filled.DateRange,
            stringResource(R.string.creation_asc)
        )
    }

    ElevatedFilterChip(
        selected = selected,
        onClick = { onSelected(order) },
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium
            )
        },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        },
        colors = FilterChipDefaults.elevatedFilterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    )
}

@Composable
private fun ViewModePanel(
    currentMode: HabitViewMode,
    onModeSelected: (HabitViewMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.view_mode),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ViewModeOption(
                mode = HabitViewMode.LIST,
                icon = Icons.Filled.ViewList,
                label = stringResource(R.string.view_list),
                selected = currentMode == HabitViewMode.LIST,
                onSelected = { onModeSelected(HabitViewMode.LIST) }
            )

            ViewModeOption(
                mode = HabitViewMode.GRID,
                icon = Icons.Filled.GridView,
                label = stringResource(R.string.view_grid),
                selected = currentMode == HabitViewMode.GRID,
                onSelected = { onModeSelected(HabitViewMode.GRID) }
            )

            ViewModeOption(
                mode = HabitViewMode.CATEGORIES,
                icon = Icons.Filled.Category,
                label = stringResource(R.string.view_categories),
                selected = currentMode == HabitViewMode.CATEGORIES,
                onSelected = { onModeSelected(HabitViewMode.CATEGORIES) }
            )
        }
    }
}

@Composable
private fun ViewModeOption(
    mode: HabitViewMode,
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onSelected: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(90.dp)
            .clickable(onClick = onSelected)
            .padding(8.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (selected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .size(64.dp)
                .shadow(
                    elevation = if (selected) 4.dp else 0.dp,
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (selected)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun EnhancedDateSelector(
    selectedDate: LocalDate,
    weekStartDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onJumpToToday: () -> Unit,
    habits: List<HabitWithProgress>,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()

    // Create localization helpers
    val monthLocalization = MonthLocalization(
        january = stringResource(R.string.january),
        february = stringResource(R.string.february),
        march = stringResource(R.string.march),
        april = stringResource(R.string.april),
        may = stringResource(R.string.may),
        june = stringResource(R.string.june),
        july = stringResource(R.string.july),
        august = stringResource(R.string.august),
        september = stringResource(R.string.september),
        october = stringResource(R.string.october),
        november = stringResource(R.string.november),
        december = stringResource(R.string.december)
    )

    val dayLocalization = DayOfWeekLocalization(
        monday = stringResource(R.string.monday_short),
        tuesday = stringResource(R.string.tuesday_short),
        wednesday = stringResource(R.string.wednesday_short),
        thursday = stringResource(R.string.thursday_short),
        friday = stringResource(R.string.friday_short),
        saturday = stringResource(R.string.saturday_short),
        sunday = stringResource(R.string.sunday_short)
    )

    // Calculate date range for the week
    val dateRange = remember(weekStartDate) {
        (0..6).map { weekStartDate.plusDays(it.toLong()) }
    }

    // Format month year range for display
    val monthYearText = remember(dateRange.first(), dateRange.last()) {
        val startMonth = monthLocalization.getLocalizedMonth(dateRange.first().month)
        val endMonth = monthLocalization.getLocalizedMonth(dateRange.last().month)
        val startYear = dateRange.first().year
        val endYear = dateRange.last().year

        if (dateRange.first().month == dateRange.last().month && startYear == endYear) {
            "$startMonth $startYear"
        } else if (startYear == endYear) {
            "$startMonth - $endMonth $startYear"
        } else {
            "$startMonth $startYear - $endMonth $endYear"
        }
    }

    // Calculate if the current week contains today
    val isCurrentWeek = today in dateRange

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header with month/year and navigation controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Month and year header
                Text(
                    text = monthYearText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                // Week navigation controls
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Today button
                    if (!isCurrentWeek) {
                        IconButton(
                            onClick = onJumpToToday,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Today,
                                contentDescription = stringResource(R.string.today),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Previous week button
                    IconButton(
                        onClick = onPreviousWeek,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ChevronLeft,
                            contentDescription = stringResource(R.string.previous_week),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Next week button
                    IconButton(
                        onClick = onNextWeek,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ChevronRight,
                            contentDescription = stringResource(R.string.next_week),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Date selector row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                dateRange.forEach { date ->
                    DateItem(
                        date = date,
                        selected = date.isEqual(selectedDate),
                        isToday = date.isEqual(today),
                        hasCompletedHabits = hasCompletedHabitsForDate(date, habits),
                        onDateSelected = { onDateSelected(date) },
                        dayLocalization = dayLocalization
                    )
                }
            }
        }
    }
}

@Composable
private fun DateItem(
    date: LocalDate,
    selected: Boolean,
    isToday: Boolean,
    hasCompletedHabits: Boolean,
    onDateSelected: () -> Unit,
    dayLocalization: DayOfWeekLocalization
) {
    val dayOfWeek = remember(date) {
        dayLocalization.getLocalizedDayOfWeek(date.dayOfWeek)
    }
    val dayOfMonth = remember(date) { date.dayOfMonth.toString() }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .widthIn(min = 40.dp)
            .clickable(onClick = onDateSelected)
            .padding(vertical = 4.dp, horizontal = 4.dp)
    ) {
        // Day of week (Mon, Tue, etc)
        Text(
            text = dayOfWeek,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Date circle
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .background(
                    color = when {
                        selected -> MaterialTheme.colorScheme.primary
                        isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        else -> Color.Transparent
                    },
                    shape = CircleShape
                )
        ) {
            Text(
                text = dayOfMonth,
                style = MaterialTheme.typography.titleMedium,
                color = when {
                    selected -> MaterialTheme.colorScheme.onPrimary
                    isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                    else -> MaterialTheme.colorScheme.onSurface
                },
                fontWeight = if (isToday || selected) FontWeight.Bold else FontWeight.Normal
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Indicator dot for completed habits
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(
                    color = if (hasCompletedHabits)
                        MaterialTheme.colorScheme.tertiary
                    else
                        Color.Transparent,
                    shape = CircleShape
                )
        )
    }
}


@Composable
private fun CategoryTabs(
    categories: List<Category>,
    selectedCategoryId: String?,
    onCategorySelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // "All" category chip
        CategoryChip(
            name = stringResource(R.string.all_categories),
            color = MaterialTheme.colorScheme.primary,
            selected = selectedCategoryId == null,
            onSelected = { onCategorySelected(null) }
        )

        // Custom category chips
        categories.forEach { category ->
            val categoryColor = parseColor(category.color, MaterialTheme.colorScheme.primary)

            CategoryChip(
                name = category.name,
                color = categoryColor,
                selected = selectedCategoryId == category.id,
                onSelected = { onCategorySelected(category.id) }
            )
        }
    }
}

@Composable
private fun CategoryChip(
    name: String,
    color: Color,
    selected: Boolean,
    onSelected: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (selected) color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) color else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        ),
        modifier = Modifier.clickable(onClick = onSelected)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color, CircleShape)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = name,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) color else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}




@Composable
private fun EmptyStateView(
    filterState: HabitsFilterState,
    onCreateHabit: () -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasActiveFilters = countActiveFilters(filterState) > 0 || filterState.searchQuery.isNotEmpty()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        // Illustration
        Icon(
            imageVector = if (hasActiveFilters)
                Icons.Outlined.FilterAlt
            else
                Icons.Outlined.SentimentDissatisfied,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
            modifier = Modifier.size(80.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Title
        Text(
            text = if (hasActiveFilters)
                stringResource(R.string.no_habits_match_filters)
            else
                stringResource(R.string.no_habits_yet),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Description
        Text(
            text = if (hasActiveFilters)
                stringResource(R.string.try_changing_filters)
            else
                stringResource(R.string.create_your_first_habit),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Action button
        Button(
            onClick = if (hasActiveFilters) onClearFilters else onCreateHabit,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = if (hasActiveFilters)
                    Icons.Filled.FilterListOff
                else
                    Icons.Filled.Add,
                contentDescription = null
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = if (hasActiveFilters)
                    stringResource(R.string.clear_filters)
                else
                    stringResource(R.string.create_habit)
            )
        }
    }
}


// Helper methods
private fun countActiveFilters(filterState: HabitsFilterState): Int {
    var count = 0
    if (filterState.statusFilter != HabitStatusFilter.ACTIVE) count++
    if (filterState.sortOrder != SortOrder.NAME_ASC) count++
    if (filterState.selectedCategoryId != null) count++
    return count
}

private fun formatMonthYear(date: LocalDate): String {
    val month = date.month.toString().lowercase().capitalize()
    return "$month ${date.year}"
}

private fun hasCompletedHabitsForDate(date: LocalDate, habits: List<HabitWithProgress>): Boolean {
    return habits.any { it.completedDates.contains(date) }
}

// Enum classes for UI state management
private enum class ExpandableSection {
    FILTERS, VIEW_MODES
}

// Extension function for capitalizing strings
private fun String.capitalize(): String {
    return this.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase() else it.toString()
    }
}