package com.example.dhbt.presentation.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.dhbt.R
import com.example.dhbt.domain.model.*
import com.example.dhbt.presentation.components.TaskCard
import com.example.dhbt.presentation.theme.DHbtTheme
import java.util.*
import java.util.concurrent.TimeUnit

@Preview(showBackground = true)
@Composable
fun DashboardScreenPreview() {
    val mockTasks = listOf(
        Task(
            id = "1",
            title = "Завершить отчет по проекту",
            description = "Завершить квартальный отчет для руководства",
            creationDate = System.currentTimeMillis(),
            dueDate = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(5),
            dueTime = "17:30",
            priority = TaskPriority.HIGH,
            status = TaskStatus.ACTIVE
        ),
        Task(
            id = "2",
            title = "Позвонить клиенту",
            description = "Обсудить детали нового проекта",
            creationDate = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1),
            dueDate = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(3),
            dueTime = "15:00",
            priority = TaskPriority.MEDIUM,
            status = TaskStatus.ACTIVE
        ),
        Task(
            id = "3",
            title = "Купить продукты",
            description = "Молоко, хлеб, яйца, фрукты",
            creationDate = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(2),
            dueDate = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(2),
            priority = TaskPriority.LOW,
            status = TaskStatus.ACTIVE,
            subtasks = listOf(
                Subtask("sub1", "3", "Молоко", true),
                Subtask("sub2", "3", "Хлеб", false),
                Subtask("sub3", "3", "Яйца", false)
            )
        )
    )

    val mockHabits = listOf(
        Habit(
            id = "1",
            title = "Медитация",
            description = "10 минут медитации ежедневно",
            iconEmoji = "🧘",
            color = "#9C27B0",
            creationDate = System.currentTimeMillis(),
            type = HabitType.BINARY,
            currentStreak = 3,
            bestStreak = 5,
            status = HabitStatus.ACTIVE,
            frequency = HabitFrequency(
                id = "f1",
                habitId = "1",
                type = FrequencyType.DAILY
            )
        ),
        Habit(
            id = "2",
            title = "Выпивать воду",
            description = "8 стаканов воды ежедневно",
            iconEmoji = "💧",
            color = "#2196F3",
            creationDate = System.currentTimeMillis(),
            type = HabitType.QUANTITY,
            targetValue = 8f,
            unitOfMeasurement = "стаканов",
            currentStreak = 5,
            bestStreak = 8,
            status = HabitStatus.ACTIVE,
            frequency = HabitFrequency(
                id = "f2",
                habitId = "2",
                type = FrequencyType.DAILY
            )
        ),
        Habit(
            id = "3",
            title = "Бег",
            description = "30 минут бега",
            iconEmoji = "🏃",
            color = "#4CAF50",
            creationDate = System.currentTimeMillis(),
            type = HabitType.TIME,
            targetValue = 30f,
            currentStreak = 15,
            bestStreak = 20,
            status = HabitStatus.ACTIVE,
            frequency = HabitFrequency(
                id = "f3",
                habitId = "3",
                type = FrequencyType.SPECIFIC_DAYS,
                daysOfWeek = listOf(1, 3, 5)
            )
        )
    )

    val mockState = DashboardState(
        isLoading = false,
        userData = UserData(name = "Максим"),
        todayTasks = mockTasks,
        todayHabits = mockHabits,
        completedTasks = 1,
        totalTasks = mockTasks.size,
        completedHabits = 1,
        totalHabits = mockHabits.size
    )

    DHbtTheme {
        DashboardScreenPreviewContent(
            state = mockState,
            onTaskClick = {},
            onHabitClick = {},
            onAddTask = {},
            onAddHabit = {},
            onViewAllTasks = {},
            onViewAllHabits = {},
            onTaskCheckedChange = { _, _ -> },
            onDeleteTask = {},
            onHabitProgressIncrement = {}
        )
    }
}

@Composable
fun DashboardScreenPreviewContent(
    state: DashboardState,
    onTaskClick: (String) -> Unit,
    onHabitClick: (String) -> Unit,
    onAddTask: () -> Unit,
    onAddHabit: () -> Unit,
    onViewAllTasks: () -> Unit,
    onViewAllHabits: () -> Unit,
    onTaskCheckedChange: (String, Boolean) -> Unit,
    onDeleteTask: (String) -> Unit,
    onHabitProgressIncrement: (String) -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            DashboardTopAppBar(
                userName = state.userData?.name ?: "Пользователь",
                formattedDate = "19 марта" // Хардкодим для превью
            )
        },
        floatingActionButton = {
            DashboardFab(
                onAddTask = onAddTask,
                onAddHabit = onAddHabit
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Статистика
            item {
                StatisticsCard(
                    completedTasks = state.completedTasks,
                    totalTasks = state.totalTasks,
                    completedHabits = state.completedHabits,
                    totalHabits = state.totalHabits
                )
            }

            // Привычки
            item {
                SectionHeader(
                    title = stringResource(R.string.habits_for_today),
                    onSeeAllClick = onViewAllHabits
                )
            }

            item {
                if (state.todayHabits.isEmpty()) {
                    EmptyStateMessage(message = stringResource(R.string.no_habits_for_today))
                } else {
                    HabitsRow(
                        habits = state.todayHabits,
                        onHabitClick = onHabitClick,
                        onHabitProgressIncrement = onHabitProgressIncrement
                    )
                }
            }

            // Задачи
            item {
                SectionHeader(
                    title = stringResource(R.string.tasks_for_today),
                    onSeeAllClick = onViewAllTasks
                )
            }

            if (state.todayTasks.isEmpty()) {
                item {
                    EmptyStateMessage(message = stringResource(R.string.no_tasks_for_today))
                }
            } else {
                items(state.todayTasks) { task ->
                    TaskCard(
                        task = task,
                        onTaskClick = { onTaskClick(task.id) },
                        onCompleteToggle = { _, isCompleted ->
                            onTaskCheckedChange(task.id, isCompleted)
                        }
                    )
                }
            }

            // Доп. пространство для FAB
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}