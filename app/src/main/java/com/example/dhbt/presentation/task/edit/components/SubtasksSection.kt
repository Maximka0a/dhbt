package com.example.dhbt.presentation.task.edit.components

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.dhbt.R
import com.example.dhbt.domain.model.Subtask
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SubtasksSection(
    subtasks: List<Subtask>,
    onAddSubtask: (String) -> Unit,
    onDeleteSubtask: (String) -> Unit,
    onToggleSubtaskCompletion: (String) -> Unit
) {
    var subtaskInput by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(title = stringResource(R.string.subtasks))

        Spacer(modifier = Modifier.height(8.dp))

        // Существующие подзадачи
        if (subtasks.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                subtasks.forEach { subtask ->
                    SubtaskItem(
                        subtask = subtask,
                        onToggleCompletion = { onToggleSubtaskCompletion(subtask.id) },
                        onDelete = { onDeleteSubtask(subtask.id) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Поле для добавления новой подзадачи
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = subtaskInput,
                onValueChange = { subtaskInput = it },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                placeholder = { Text(stringResource(R.string.add_subtask)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (subtaskInput.isNotBlank()) {
                            onAddSubtask(subtaskInput)
                            subtaskInput = ""
                            scope.launch {
                                delay(100) // Небольшая задержка для обновления UI
                                focusRequester.requestFocus()
                            }
                        } else {
                            focusManager.clearFocus()
                        }
                    }
                ),
                trailingIcon = {
                    if (subtaskInput.isNotBlank()) {
                        IconButton(
                            onClick = {
                                onAddSubtask(subtaskInput)
                                subtaskInput = ""
                                scope.launch {
                                    delay(100)
                                    focusRequester.requestFocus()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(R.string.add_subtask)
                            )
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun SubtaskItem(
    subtask: Subtask,
    onToggleCompletion: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = subtask.isCompleted,
                onCheckedChange = { onToggleCompletion() },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            Text(
                text = subtask.title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (subtask.isCompleted)
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = stringResource(R.string.delete_subtask),
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}