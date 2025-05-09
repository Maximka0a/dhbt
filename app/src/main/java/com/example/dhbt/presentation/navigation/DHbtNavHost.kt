package com.example.dhbt.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.example.dhbt.presentation.dashboard.DashboardScreen
import com.example.dhbt.presentation.eisenhower.EisenhowerScreen
import com.example.dhbt.presentation.habit.detail.HabitDetailScreen
import com.example.dhbt.presentation.habit.edit.EditHabitScreen
import com.example.dhbt.presentation.habit.list.HabitsScreen
import com.example.dhbt.presentation.onboarding.OnboardingScreen
import com.example.dhbt.presentation.pomodoro.PomodoroScreen
import com.example.dhbt.presentation.settings.SettingsScreen
import com.example.dhbt.presentation.statistics.StatisticsScreen
import com.example.dhbt.presentation.subscription.SubscriptionScreen
import com.example.dhbt.presentation.task.detail.TaskDetailScreen
import com.example.dhbt.presentation.task.edit.EditTaskScreen
import com.example.dhbt.presentation.task.list.TasksScreen

@Composable
fun DHbtNavHost(
    navController: NavHostController,
    startDestination: Any = Dashboard,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController, startDestination = startDestination, modifier = modifier
    ) {
        // Основные экраны
        composable<Onboarding> {
            OnboardingScreen(
                navController = navController
            )
        }

        // Экраны задач
        composable<TaskDetail> { backStackEntry ->
            val taskData = backStackEntry.toRoute<TaskDetail>()
            TaskDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { taskId ->
                    navController.navigate(TaskEdit(taskId))
                },
                onNavigateToPomodoro = { taskId ->
                    navController.navigate(Pomodoro(taskId = taskId))
                }
            )
        }

        composable<TaskEdit> { backStackEntry ->
            val taskData = backStackEntry.toRoute<TaskEdit>()
            EditTaskScreen(
                taskId = taskData.taskId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Экраны привычек
        composable<HabitDetail> { backStackEntry ->
            val habitData = backStackEntry.toRoute<HabitDetail>()
            HabitDetailScreen(
                navController = navController
            )
        }

        composable<HabitEdit> { backStackEntry ->
            val habitData = backStackEntry.toRoute<HabitEdit>()
            EditHabitScreen(
                navController = navController
                // habitId is now automatically passed as null for creation
            )
        }

        // Экран Pomodoro
        composable<Pomodoro> { backStackEntry ->
            val pomodoroData = backStackEntry.toRoute<Pomodoro>()
            PomodoroScreen(
                taskId = pomodoroData.taskId, // Передаем ID задачи, если есть
                navController = navController
            )
        }

        composable<EisenhowerMatrix> {
            EisenhowerScreen(navController = navController)
        }

        // Основные экраны навигации
            composable<Dashboard> {
                DashboardScreen(
                    onTaskClick = { taskId -> navController.navigate(TaskDetail(taskId)) },
                    onHabitClick = { habitId -> navController.navigate(HabitDetail(habitId)) },
                    onAddTask = { navController.navigate(TaskEdit()) },
                    onAddHabit = { navController.navigate(HabitEdit()) },
                    onViewAllTasks = { navController.navigate(Tasks) },
                    onViewAllHabits = { navController.navigate(Habits) },
                    onSettings = { navController.navigate(Settings) },
                    onPremiumClicked = { navController.navigate(PremiumSubscription) },
                    onStatisticsClick = { navController.navigate(Statistics) }
                )
            }


        composable<Tasks> {
            TasksScreen(
                onTaskClick = { taskId ->
                    navController.navigate(TaskDetail(taskId))
                },
                onAddTask = {
                    navController.navigate(TaskEdit())
                }
            )
        }

        composable<PremiumSubscription> {
            SubscriptionScreen(navController = navController)
        }

        composable<Habits> {
            HabitsScreen(
                navController = navController
            )
        }

        composable<Settings> {
            SettingsScreen(
                navController = navController
            )
        }

        composable<Statistics> {
            StatisticsScreen(
                navController = navController
            )
        }
        // Остальные маршруты...
    }
}