package com.example.dhbt.presentation.task.detail.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.dhbt.R
import com.example.dhbt.domain.model.Subtask
import com.example.dhbt.domain.model.TaskStatus
import com.example.dhbt.presentation.task.detail.TaskDetailState

@Composable
fun TaskDetailContent(
    state: TaskDetailState,
    onSubtaskToggle: (Subtask) -> Unit,
    onNavigateToPomodoro: (String) -> Unit,
    onDeleteTask: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCompleted = state.task?.status == TaskStatus.COMPLETED
    val contentAlpha = if (isCompleted) 0.7f else 1f

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .alpha(contentAlpha)
    ) {
        // Основное описание
        if (!state.task?.description.isNullOrBlank()) {
            TaskDescriptionSection(description = state.task?.description!!)
        }

        // Информационный блок с новыми параметрами
        TaskInfoSection(
            dueDate = state.task?.dueDate,
            dueTime = state.task?.dueTime,
            priority = state.task?.priority,
            category = state.category,
            eisenhowerQuadrant = state.task?.eisenhowerQuadrant,
            isArchived = state.task?.status == TaskStatus.ARCHIVED,
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 16.dp)
        )

        // Подзадачи
        if (state.subtasks.isNotEmpty()) {
            TaskSubtasksSection(
                subtasks = state.subtasks,
                onToggle = onSubtaskToggle
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }

        // Теги
        if (state.tags.isNotEmpty()) {
            TaskTagsSection(tags = state.tags)

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }

        // Дополнительная информация
        TaskAdditionalInfoSection(
            duration = state.task?.duration,
            estimatedPomodoros = state.task?.estimatedPomodoroSessions,
            totalFocusTime = state.totalFocusTime,
            recurrence = state.recurrence,
            onStartPomodoro = {
                state.task?.id?.let { taskId ->
                    onNavigateToPomodoro(taskId)
                }
            }
        )
        if (state.task != null && !state.isLoading) {
            Spacer(modifier = Modifier.height(24.dp))

// Кнопка удаления задачи с улучшенным дизайном
            OutlinedButton(
                onClick = onDeleteTask, // Использовать метод ViewModel
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = stringResource(R.string.delete_task),
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
        // Увеличенный отступ для кнопки удаления внизу
        Spacer(modifier = Modifier.height(100.dp))
    }
}