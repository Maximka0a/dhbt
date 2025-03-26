package com.example.dhbt.presentation.dashboard.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.dhbt.R
import com.example.dhbt.domain.model.Task
import com.example.dhbt.domain.model.TaskStatus
import com.example.dhbt.presentation.shared.TaskItem
import kotlinx.coroutines.launch

@Suppress("DEPRECATION")
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SwipeableTaskItem(
    task: Task,
    onTaskClick: (String) -> Unit,
    onTaskCompleteChange: (Boolean) -> Unit, // Для передачи конкретного статуса
    onToggleTaskStatus: () -> Unit, // Новый параметр для простого переключения
    onDeleteTask: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val isCompleted = task.status == TaskStatus.COMPLETED

    // Реализуем свайп через SwipeToDismiss с улучшенной анимацией
    var isDismissed by remember { mutableStateOf(false) }
    val dismissState = rememberDismissState(
        confirmStateChange = { dismissValue ->
            when (dismissValue) {
                DismissValue.DismissedToStart -> {
                    // Удаление задачи - оставляем без изменений
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    isDismissed = true
                    scope.launch {
                        kotlinx.coroutines.delay(200)
                        onDeleteTask()
                        isDismissed = false
                    }
                    false
                }
                DismissValue.DismissedToEnd -> {
                    // Используем новый метод для переключения статуса
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    isDismissed = true
                    scope.launch {
                        kotlinx.coroutines.delay(200)
                        onToggleTaskStatus() // Просто переключаем статус
                        isDismissed = false
                    }
                    false
                }
                DismissValue.Default -> false
            }
        }
    )

    SwipeToDismiss(
        state = dismissState,
        directions = setOf(DismissDirection.StartToEnd, DismissDirection.EndToStart),
        dismissThresholds = { FractionalThreshold(0.3f) },
        background = {
            // Используем выделенную функцию для фона с текущим статусом задачи
            SwipeBackground(
                dismissDirection = dismissState.dismissDirection,
                progress = dismissState.progress.fraction,
                isTaskCompleted = isCompleted
            )
        },
        dismissContent = {
            TaskItem(
                task = task,
                onTaskClick = { onTaskClick(task.id) },
                // Оставляем только одну версию обработчика:
                onCompleteToggle = { onToggleTaskStatus() }, // Используем новый метод для переключения
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .fillMaxWidth()
            )
        },
        modifier = modifier.animateContentSize()
    )
}

// Выделенный компонент фона для свайпа
@Composable
private fun SwipeBackground(
    dismissDirection: DismissDirection?,
    progress: Float,
    isTaskCompleted: Boolean
) {
    // Анимированные цвета фона при свайпе
    val backgroundColor by animateColorAsState(
        targetValue = when (dismissDirection) {
            DismissDirection.StartToEnd -> if (isTaskCompleted)
                Color(0xFFFF9800).copy(alpha = 0.2f * progress) // Оранжевый для отмены выполнения
            else
                Color(0xFF4CAF50).copy(alpha = 0.2f * progress) // Зеленый для выполнения
            DismissDirection.EndToStart -> Color(0xFFF44336).copy(alpha = 0.2f * progress) // Красный для удаления
            null -> Color.Transparent
        },
        animationSpec = tween(300)
    )

    // Анимация иконки при свайпе
    val iconScale by animateFloatAsState(
        targetValue = progress.coerceIn(0.5f, 1.2f),
        animationSpec = tween(300)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(horizontal = 20.dp),
        contentAlignment = when (dismissDirection) {
            DismissDirection.StartToEnd -> Alignment.CenterStart
            DismissDirection.EndToStart -> Alignment.CenterEnd
            null -> Alignment.Center
        }
    ) {
        when (dismissDirection) {
            DismissDirection.StartToEnd -> {
                if (progress > 0) {
                    if (isTaskCompleted) {
                        // Для выполненных задач - иконка отмены выполнения
                        Icon(
                            imageVector = Icons.Filled.RemoveCircle,
                            contentDescription = stringResource(R.string.mark_as_not_completed),
                            tint = Color(0xFFFF9800), // Оранжевый
                            modifier = Modifier
                                .scale(iconScale)
                                .size(28.dp)
                        )
                    } else {
                        // Для невыполненных задач - иконка выполнения
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = stringResource(R.string.mark_as_completed),
                            tint = Color(0xFF4CAF50), // Зеленый
                            modifier = Modifier
                                .scale(iconScale)
                                .size(28.dp)
                        )
                    }
                }
            }
            DismissDirection.EndToStart -> {
                if (progress > 0) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.delete),
                        tint = Color(0xFFF44336), // Красный
                        modifier = Modifier
                            .scale(iconScale)
                            .size(28.dp)
                    )
                }
            }
            null -> {}
        }
    }
}