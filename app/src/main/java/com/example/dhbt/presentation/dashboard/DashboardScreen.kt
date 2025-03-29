package com.example.dhbt.presentation.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material3.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.dhbt.R
import com.example.dhbt.presentation.dashboard.components.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onTaskClick: (String) -> Unit,
    onHabitClick: (String) -> Unit,
    onAddTask: () -> Unit,
    onAddHabit: () -> Unit,
    onViewAllTasks: () -> Unit,
    onViewAllHabits: () -> Unit,
    onSettings: () -> Unit,
    onPremiumClicked: () -> Unit,
    onStatisticsClick: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Для эффекта сворачивающегося тулбара
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    // Обработка ошибок
    LaunchedEffect(state.error) {
        state.error?.let { errorMessage ->
            snackbarHostState.showSnackbar(
                message = errorMessage,
                withDismissAction = true,
                duration = SnackbarDuration.Short
            )
            viewModel.dismissError()
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            DashboardTopAppBar(
                userName = state.userData?.name ?: "Пользователь",
                formattedDate = viewModel.getTodayFormatted(),
                onSettings = onSettings,
                onPremiumClicked = onPremiumClicked,
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            DashboardFab(
                onAddTask = onAddTask,
                onAddHabit = onAddHabit
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(16.dp),
                snackbar = { snackbarData ->
                    Snackbar(
                        snackbarData = snackbarData,
                        containerColor = MaterialTheme.colorScheme.inverseSurface,
                        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                        actionColor = MaterialTheme.colorScheme.inversePrimary
                    )
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (state.isLoading) {
                LoadingState()
            } else if (state.todayTasks.isEmpty() && state.todayHabits.isEmpty()) {
                EmptyDashboardState(
                    onAddTask = onAddTask,
                    onAddHabit = onAddHabit
                )
            } else {
                // Основное содержимое
                DashboardContent(
                    state = state,
                    listState = listState,
                    onTaskClick = onTaskClick,
                    onHabitClick = onHabitClick,
                    onTaskCompleteChange = viewModel::onTaskCheckedChange,
                    onToggleTaskStatus = viewModel::toggleTaskStatus,
                    onDeleteTask = viewModel::onDeleteTask,
                    onHabitProgressIncrement = viewModel::onHabitProgressIncrement,
                    onViewAllTasks = onViewAllTasks,
                    onViewAllHabits = onViewAllHabits,
                    onStatisticsClick = onStatisticsClick
                )
            }

            // Индикация процесса загрузки данных при обновлении
            AnimatedVisibility(
                visible = state.isRefreshing,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                LinearProgressIndicator(
                    progress = {
                        0.75f // Можно использовать реальное значение прогресса, если оно доступно
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                )
            }
        }
    }
}