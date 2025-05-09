@file:OptIn(ExperimentalMaterialApi::class)

package com.example.dhbt.presentation.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.dhbt.presentation.dashboard.components.*
import com.example.dhbt.presentation.util.ErrorManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Основной экран дашборда с оптимизированной структурой
 */
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
    // Состояния
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val listState = rememberLazyListState()

    // Обработчик событий UI
    LaunchedEffect(key1 = true) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is DashboardViewModel.UiEvent.ShowError -> {
                    val errorText = context.getString(event.error.type.messageResId)
                    snackbarHostState.showSnackbar(
                        message = errorText,
                        withDismissAction = true,
                        duration = SnackbarDuration.Short
                    )
                }
                is DashboardViewModel.UiEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        withDismissAction = true
                    )
                }
            }
        }
    }

    // Обновление при возвращении на экран
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Pull-to-refresh
    val refreshState = rememberPullRefreshState(
        refreshing = state.isRefreshing,
        onRefresh = viewModel::refresh
    )

    // Прокрутка тулбара
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

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
                .pullRefresh(refreshState)
        ) {
            // Используем разные композабл-функции в зависимости от состояния
            when {
                state.isLoading -> LoadingState()
                state.todayTasks.isEmpty() && state.todayHabits.isEmpty() -> {
                    EmptyDashboardState(
                        onAddTask = onAddTask,
                        onAddHabit = onAddHabit
                    )
                }
                else -> {
                    // Мемоизация обработчиков для предотвращения ненужных перерисовок
                    val onHabitProgressIncrement = remember(viewModel) {
                        { habitId: String -> viewModel.onHabitProgressIncrement(habitId) }
                    }

                    val onTaskStatusToggle = remember(viewModel) {
                        { taskId: String -> viewModel.toggleTaskStatus(taskId) }
                    }

                    val onTaskCheckedChange = remember(viewModel) {
                        { taskId: String, isCompleted: Boolean ->
                            viewModel.onTaskCheckedChange(taskId, isCompleted)
                        }
                    }

                    val onTaskDelete = remember(viewModel) {
                        { taskId: String -> viewModel.onDeleteTask(taskId) }
                    }

                    // Основное содержимое с оптимизацией перерисовок
                    DashboardContent(
                        state = state,
                        listState = listState,
                        onTaskClick = onTaskClick,
                        onHabitClick = onHabitClick,
                        onTaskCompleteChange = onTaskCheckedChange,
                        onToggleTaskStatus = onTaskStatusToggle,
                        onDeleteTask = onTaskDelete,
                        onHabitProgressIncrement = onHabitProgressIncrement,
                        onViewAllTasks = onViewAllTasks,
                        onViewAllHabits = onViewAllHabits,
                        onStatisticsClick = onStatisticsClick
                    )
                }
            }

            // Индикатор обновления
            PullRefreshIndicator(
                refreshing = state.isRefreshing,
                state = refreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                backgroundColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            )
        }
    }
}