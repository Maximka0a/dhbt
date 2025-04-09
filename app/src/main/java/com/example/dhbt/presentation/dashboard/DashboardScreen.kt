@file:OptIn(ExperimentalMaterialApi::class)

package com.example.dhbt.presentation.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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

    // Обновление при возвращении на экран - с помощью LifecycleEffect
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Обновляем данные при возвращении на экран
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

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

    // Pull-to-refresh состояние
    val refreshState = rememberPullRefreshState(
        refreshing = state.isRefreshing,
        onRefresh = { viewModel.refresh() }
    )

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
                .pullRefresh(refreshState) // Применяем Pull-to-Refresh к корневому контейнеру
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

            // Индикатор Pull-to-Refresh
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