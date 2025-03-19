package com.example.dhbt.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.dhbt.presentation.MainViewModel

@Composable
fun DHbtNavHost(
    navController: NavHostController,
    startDestination: String = Dashboard.javaClass.simpleName,
    modifier: Modifier = Modifier,
    mainViewModel: MainViewModel? = null  // Добавляем опциональный параметр
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // Основные экраны
        /*
        composable<Dashboard> {
            DashboardScreen(
                onTaskClick = { taskId -> navController.navigate(TaskDetail(taskId)) },
                onHabitClick = { habitId -> navController.navigate(HabitDetail(habitId)) },
                onAddTask = { navController.navigate(TaskEdit()) },
                onAddHabit = { navController.navigate(HabitEdit()) },
                onViewAllTasks = { navController.navigate(Tasks) },
                onViewAllHabits = { navController.navigate(Habits) },
                userName = mainViewModel?.state?.value?.userName  // Передаем имя пользователя
            )


        }
        composable<Tasks> {
            TasksScreen(
                onTaskClick = { taskId -> navController.navigate(TaskDetail(taskId)) },
                onCreateTask = { navController.navigate(TaskEdit()) },
                onNavigateToMatrix = { navController.navigate(EisenhowerMatrix) }
            )
        }

        composable<Habits> {
            HabitsScreen(
                onHabitClick = { habitId -> navController.navigate(HabitDetail(habitId)) },
                onCreateHabit = { navController.navigate(HabitEdit()) }
            )
        }

        composable<Statistics> {
            StatisticsScreen()
        }

        composable<More> {
            MoreScreen(
                onNavigateToSettings = { navController.navigate(Settings) },
                onNavigateToPomodoro = { navController.navigate(Pomodoro) },
                onNavigateToSubscription = { navController.navigate(PremiumSubscription) }
            )
        }

        // Детали задач
        composable<TaskDetail> { backStackEntry ->
            val taskData = backStackEntry.toRoute<TaskDetail>()
            TaskDetailScreen(
                taskId = taskData.taskId,
                onNavigateBack = { navController.popBackStack() },
                onEditTask = { navController.navigate(TaskEdit(it)) }
            )
        }

        composable<TaskEdit> { backStackEntry ->
            val taskData = backStackEntry.toRoute<TaskEdit>()
            TaskEditScreen(
                taskId = taskData.taskId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Детали привычек
        composable<HabitDetail> { backStackEntry ->
            val habitData = backStackEntry.toRoute<HabitDetail>()
            HabitDetailScreen(
                habitId = habitData.habitId,
                onNavigateBack = { navController.popBackStack() },
                onEditHabit = { navController.navigate(HabitEdit(it)) }
            )
        }

        composable<HabitEdit> { backStackEntry ->
            val habitData = backStackEntry.toRoute<HabitEdit>()
            HabitEditScreen(
                habitId = habitData.habitId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Функциональные экраны
        composable<Pomodoro> {
            PomodoroScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<EisenhowerMatrix> {
            EisenhowerMatrixScreen(
                onNavigateBack = { navController.popBackStack() },
                onTaskClick = { taskId -> navController.navigate(TaskDetail(taskId)) }
            )
        }

        // Дополнительные экраны
        composable<Settings> {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<Onboarding> {
            OnboardingScreen(
                onComplete = {
                    mainViewModel?.completeOnboarding()  // Вызываем метод для завершения онбординга
                    navController.navigate(Dashboard) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    }
                },
                onUpdateUserName = { name -> mainViewModel?.updateUserName(name) },
                onUpdateWakeupAndSleepTime = { wakeUp, sleep ->
                    mainViewModel?.updateWakeupAndSleepTime(wakeUp, sleep)
                }
            )
        }

        composable<PremiumSubscription> {
            PremiumSubscriptionScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

         */
    }
}