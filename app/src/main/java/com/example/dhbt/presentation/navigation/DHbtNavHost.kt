package com.example.dhbt.presentation.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.example.dhbt.presentation.MainViewModel
import com.example.dhbt.presentation.dashboard.DashboardScreen
import com.example.dhbt.presentation.task.detail.TaskDetailScreen
import com.example.dhbt.presentation.task.edit.TaskEditScreen
import com.example.dhbt.presentation.task.list.TasksScreen

@Composable
fun DHbtNavHost(
    navController: NavHostController,
    startDestination: Any = Dashboard,
    modifier: Modifier = Modifier,
    mainViewModel: MainViewModel? = null  // Добавляем опциональный параметр
) {
    NavHost(
        navController = navController, startDestination = startDestination, modifier = modifier
    ) {
        // Основные экраны
        composable<Onboarding> {
            Surface(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Экран онбординга")
                    Button(onClick = {
                        mainViewModel?.completeOnboarding()
                        navController.navigate(Dashboard) {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        }
                    }) {
                        Text("Перейти к приложению")
                    }
                }
            }
            /*
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
             */
        }

        // Переместили маршрут TaskDetail на уровень других маршрутов
        composable<TaskDetail> { backStackEntry ->
            val taskData = backStackEntry.toRoute<TaskDetail>()
            TaskDetailScreen(
                taskId = taskData.taskId,
                onNavigateBack = { navController.popBackStack() },
                onEditTask = { navController.navigate(TaskEdit(it)) }
            )
        }

        composable<Dashboard> {
            DashboardScreen(
                onTaskClick = { taskId -> navController.navigate(TaskDetail(taskId)) },
                onHabitClick = { habitId -> navController.navigate(HabitDetail(habitId)) },
                onAddTask = { navController.navigate(TaskEdit()) },
                onAddHabit = { navController.navigate(HabitEdit()) },
                onViewAllTasks = { navController.navigate(Tasks) },
                onViewAllHabits = { navController.navigate(Habits) },
            )
        }

        composable<TaskEdit> { backStackEntry ->
            val taskData = backStackEntry.toRoute<TaskEdit>()
            TaskEditScreen(
                taskId =  taskData.taskId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<Tasks> {
            TasksScreen(onTaskClick = { taskId ->
                navController.navigate(TaskDetail(taskId))
            }, onAddTask = {
                navController.navigate(TaskEdit())
            })
        }

        // Остальные маршруты (закомментированные)...
    }
}