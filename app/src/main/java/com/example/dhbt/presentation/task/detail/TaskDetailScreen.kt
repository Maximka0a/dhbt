package com.example.dhbt.presentation.task.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.dhbt.R
import com.example.dhbt.domain.model.TaskStatus
import com.example.dhbt.presentation.task.detail.components.TaskDetailContent
import com.example.dhbt.presentation.task.detail.components.TaskDetailTopAppBar
import com.example.dhbt.presentation.task.detail.components.TaskErrorState
import com.example.dhbt.presentation.task.detail.components.TaskLoadingState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.dhbt.presentation.task.detail.components.TaskStatusOptionsMenu

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    onNavigateToPomodoro: (String) -> Unit,
    viewModel: TaskDetailViewModel = hiltViewModel()
) {
    val state = viewModel.state.value
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }

    var showStatusOptions by remember { mutableStateOf(false) }


    // Добавьте этот блок - он будет вызывать обновление данных при навигации обратно на экран
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.reloadTaskDetails()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }


    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    // Обработка навигации после удаления задачи
    LaunchedEffect(state.task) {
        if (state.task == null && !state.isLoading) {
            // Задача была удалена, возвращаемся обратно
            onNavigateBack()
        }
    }

    // Обработка флагов в состоянии ViewModel для навигации
    LaunchedEffect(state.showEditTask) {
        if (state.showEditTask) {
            state.task?.id?.let { taskId ->
                onNavigateToEdit(taskId)
                viewModel.toggleEditTask() // Сбрасываем флаг после обработки
            }
        }
    }

    // Основной контейнер
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            // Верхняя панель с дополнительным флагом архива
            TaskDetailTopAppBar(
                title = state.task?.title ?: "",
                isComplete = state.task?.status == TaskStatus.COMPLETED,
                isArchived = state.task?.status == TaskStatus.ARCHIVED,
                onNavigateBack = onNavigateBack,
                onEdit = { viewModel.toggleEditTask() },
                onDelete = { viewModel.toggleDeleteDialog()},
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            AnimatedVisibility(
                visible = state.task != null && !state.isLoading,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    // FAB для запуска Pomodoro
                    SmallFloatingActionButton(
                        onClick = {
                            state.task?.id?.let { taskId ->
                                onNavigateToPomodoro(taskId)
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_pomodoro),
                            contentDescription = stringResource(R.string.start_pomodoro_session)
                        )
                    }

                    // Основная FAB для изменения статуса
                    ExtendedFloatingActionButton(
                        onClick = { showStatusOptions = true },
                        icon = {
                            Icon(
                                if (state.task?.status == TaskStatus.COMPLETED)
                                    Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                contentDescription = null
                            )
                        },
                        text = {
                            Text(
                                text = if (state.task?.status == TaskStatus.COMPLETED)
                                    stringResource(R.string.completed)
                                else
                                    stringResource(R.string.mark_as_complete)
                            )
                        },
                        containerColor = if (state.task?.status == TaskStatus.COMPLETED)
                            MaterialTheme.colorScheme.secondaryContainer
                        else
                            MaterialTheme.colorScheme.primaryContainer
                    )
                }
            }
        },
        bottomBar = {

        }
    ) { paddingValues ->
        when {
            // Состояние загрузки
            state.isLoading -> {
                TaskLoadingState(modifier = Modifier.padding(paddingValues))
            }

            // Состояние ошибки (если нет данных задачи)
            state.task == null -> {
                TaskErrorState(
                    message = state.error ?: stringResource(R.string.task_not_found),
                    onRetry = { /* Добавьте логику повторной загрузки */ },
                    onNavigateBack = onNavigateBack,
                    modifier = Modifier.padding(paddingValues)
                )
            }

            // Основной контент
            else -> {
                TaskDetailContent(
                    state = state,
                    onSubtaskToggle = viewModel::toggleSubtaskCompletion,
                    modifier = Modifier.padding(paddingValues),
                    onDeleteTask = {
                        viewModel.toggleDeleteDialog()
                    },
                    onNavigateToPomodoro = onNavigateToPomodoro
                )
            }
        }
    }

    // Диалог подтверждения удаления
    if (state.showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteDialog() },
            title = { Text(stringResource(R.string.confirm_delete_task)) },
            text = { Text(stringResource(R.string.delete_task_confirmation)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTask() // deleteTask должен сам сбрасывать флаг
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDeleteDialog() }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Меню выбора статуса
    if (showStatusOptions) {
        TaskStatusOptionsMenu(
            currentStatus = state.task?.status ?: TaskStatus.ACTIVE,
            onStatusSelected = { status ->
                viewModel.updateTaskStatus(status)
                showStatusOptions = false
            },
            onDismiss = { showStatusOptions = false }
        )
    }
}