package com.example.dhbt.presentation.habit.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.dhbt.R
import com.example.dhbt.domain.model.Category
import com.example.dhbt.presentation.habit.list.HabitStatusFilter
import com.example.dhbt.presentation.habit.list.HabitViewMode
import com.example.dhbt.presentation.habit.list.SortOrder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitsFilterBar(
    statusFilter: HabitStatusFilter,
    viewMode: HabitViewMode,
    categories: List<Category>,
    selectedCategoryId: String?,
    onStatusFilterChanged: (HabitStatusFilter) -> Unit,
    onViewModeChanged: (HabitViewMode) -> Unit,
    onCategorySelected: (String?) -> Unit,
    onSearchClicked: () -> Unit,
    onSortClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        // Верхняя панель: фильтры по статусу, режим просмотра, поиск, сортировка
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Фильтры по статусу
            FilterChips(
                selectedFilter = statusFilter,
                onFilterSelected = onStatusFilterChanged,
                modifier = Modifier.weight(1f)
            )

            // Режимы просмотра и кнопки действий
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ViewModeButton(
                    viewMode = viewMode,
                    onViewModeChanged = onViewModeChanged
                )

                IconButton(onClick = onSearchClicked) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = stringResource(R.string.search)
                    )
                }

                IconButton(onClick = onSortClicked) {
                    Icon(
                        imageVector = Icons.Default.Sort,
                        contentDescription = stringResource(R.string.sort)
                    )
                }
            }
        }

        // Горизонтальный список категорий в виде чипов
        if (categories.isNotEmpty()) {
            val scrollState = rememberScrollState()

            Row(
                modifier = Modifier
                    .horizontalScroll(scrollState)
                    .padding(vertical = 4.dp)
            ) {
                // Первый "чип" - все категории
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

                // Категории
                categories.forEach { category ->
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
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val filters = listOf(
            HabitStatusFilter.ACTIVE to stringResource(R.string.active),
            HabitStatusFilter.ARCHIVED to stringResource(R.string.archived),
            HabitStatusFilter.ALL to stringResource(R.string.all)
        )

        filters.forEach { (filter, label) ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = { Text(label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}

@Composable
fun ViewModeButton(
    viewMode: HabitViewMode,
    onViewModeChanged: (HabitViewMode) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = when (viewMode) {
                    HabitViewMode.LIST -> Icons.Default.ViewList
                    HabitViewMode.GRID -> Icons.Default.GridView
                    HabitViewMode.CATEGORIES -> Icons.Default.Category
                },
                contentDescription = "Режим просмотра"
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.view_list)) },
                leadingIcon = { Icon(Icons.Default.ViewList, contentDescription = null) },
                onClick = {
                    onViewModeChanged(HabitViewMode.LIST)
                    expanded = false
                }
            )

            DropdownMenuItem(
                text = { Text(stringResource(R.string.view_grid)) },
                leadingIcon = { Icon(Icons.Default.GridView, contentDescription = null) },
                onClick = {
                    onViewModeChanged(HabitViewMode.GRID)
                    expanded = false
                }
            )

            DropdownMenuItem(
                text = { Text(stringResource(R.string.view_categories)) },
                leadingIcon = { Icon(Icons.Default.Category, contentDescription = null) },
                onClick = {
                    onViewModeChanged(HabitViewMode.CATEGORIES)
                    expanded = false
                }
            )
        }
    }
}