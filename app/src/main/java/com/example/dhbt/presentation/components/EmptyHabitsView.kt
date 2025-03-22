package com.example.dhbt.presentation.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.dhbt.R
import com.example.dhbt.domain.model.Category
import com.example.dhbt.domain.model.HabitStatus
import com.example.dhbt.presentation.habit.components.*
import com.example.dhbt.presentation.habit.list.HabitStatusFilter
import com.example.dhbt.presentation.habit.list.HabitsFilterState
import com.example.dhbt.presentation.navigation.HabitDetail
import com.example.dhbt.presentation.navigation.HabitEdit
import java.time.LocalDate

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