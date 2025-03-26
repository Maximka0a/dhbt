package com.example.dhbt.presentation.dashboard.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.dhbt.R
import com.example.dhbt.presentation.dashboard.DashboardState
import com.example.dhbt.presentation.shared.EmptyStateMessage

@Composable
fun DashboardContent(
    state: DashboardState,
    listState: LazyListState,
    onTaskClick: (String) -> Unit,
    onHabitClick: (String) -> Unit,
    onTaskCompleteChange: (String, Boolean) -> Unit,
    onToggleTaskStatus: (String) -> Unit, // Новый параметр для простого переключения статуса
    onDeleteTask: (String) -> Unit,
    onHabitProgressIncrement: (String) -> Unit,
    onViewAllTasks: () -> Unit,
    onViewAllHabits: () -> Unit
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 88.dp), // Добавляем отступ снизу для FAB
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Статистика
        item(key = "statistics") {
            StatisticsCard(
                completedTasks = state.completedTasks,
                totalTasks = state.totalTasks,
                completedHabits = state.completedHabits,
                totalHabits = state.totalHabits
            )
        }

        // Заголовок секции привычек
        item(key = "habits_header") {
            SectionHeader(
                title = stringResource(R.string.habits_for_today),
                onSeeAllClick = onViewAllHabits
            )
        }

        // Привычки на сегодня
        item(key = "habits_content") {
            if (state.todayHabits.isEmpty()) {
                EmptyStateMessage(
                    message = stringResource(R.string.no_habits_for_today),
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                HabitsRow(
                    habits = state.todayHabits,
                    onHabitClick = onHabitClick,
                    onHabitProgressIncrement = onHabitProgressIncrement
                )
            }
        }

        // Заголовок секции задач
        item(key = "tasks_header") {
            SectionHeader(
                title = stringResource(R.string.tasks_for_today),
                onSeeAllClick = onViewAllTasks
            )
        }

        // Задачи на сегодня
        if (state.todayTasks.isEmpty()) {
            item(key = "tasks_empty") {
                EmptyStateMessage(
                    message = stringResource(R.string.no_tasks_for_today),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            }
        } else {
            // Список задач
            items(
                items = state.todayTasks,
                key = { it.id }
            ) { task ->
                SwipeableTaskItem(
                    task = task,
                    onTaskClick = { onTaskClick(task.id) },
                    onTaskCompleteChange = { isCompleted ->
                        // Передаем новое состояние
                        onTaskCompleteChange(task.id, isCompleted)
                    },
                    onToggleTaskStatus = {
                        // Простое переключение без указания целевого статуса
                        onToggleTaskStatus(task.id)
                    },
                    onDeleteTask = { onDeleteTask(task.id) }
                )
            }
            // Дополнительное пространство внизу
            item(key = "bottom_spacer") {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    @Composable
    fun LoadingState() {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    strokeWidth = 4.dp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.loading_data),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    @Composable
    fun EmptyDashboardState(
        onAddTask: () -> Unit,
        onAddHabit: () -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.empty_dashboard_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.empty_dashboard_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = onAddTask,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = stringResource(R.string.add_task))
                }

                OutlinedButton(
                    onClick = onAddHabit,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = stringResource(R.string.add_habit))
                }
            }
        }
    }
}