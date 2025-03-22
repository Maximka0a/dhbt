package com.example.dhbt.presentation.habit.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.dhbt.R
import com.example.dhbt.domain.model.Category
import com.example.dhbt.presentation.habit.components.*
import com.example.dhbt.presentation.habit.list.HabitWithProgress
import com.example.dhbt.presentation.components.EmptyHabitsView
import com.example.dhbt.presentation.habit.list.HabitsFilterState // Добавьте этот импорт

@Composable
fun CategoryGroupedHabits(
    habits: List<HabitWithProgress>,
    categories: List<Category>,
    filterState: HabitsFilterState,
    onToggleCompletion: (String) -> Unit,
    onIncrement: (String) -> Unit,
    onDecrement: (String) -> Unit,
    onHabitClick: (String) -> Unit,
    onCreateHabit: () -> Unit,
    onClearFilters: () -> Unit
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
        // Используем переданный filterState вместо создания нового
        EmptyHabitsView(
            filterState = filterState,
            onCreateHabit = onCreateHabit,
            onClearFilters = onClearFilters
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

