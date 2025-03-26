package com.example.dhbt.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.dhbt.domain.model.Subtask
import com.example.dhbt.domain.model.Tag
import com.example.dhbt.domain.model.Task
import com.example.dhbt.domain.model.TaskPriority
import com.example.dhbt.domain.model.TaskStatus
import com.example.dhbt.presentation.shared.ColorIndicator
import com.example.dhbt.presentation.theme.DHbtTheme
import com.example.dhbt.presentation.util.toColor
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@Preview(showBackground = true)
@Composable
fun TaskCardBasicPreview() {
    DHbtTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            val task = remember {
                Task(
                    id = "1",
                    title = "Сделать отчет по проекту",
                    description = "Подготовить ежемесячный отчет для руководства",
                    creationDate = System.currentTimeMillis(),
                    dueDate = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(2),
                    dueTime = "15:00",
                    color = "#4CAF50",
                    priority = TaskPriority.MEDIUM
                )
            }

            TaskCard(
                task = task,
                onTaskClick = {},
                onCompleteToggle = { _, _ -> },
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TaskCardCompletedPreview() {
    DHbtTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            val task = remember {
                Task(
                    id = "2",
                    title = "Купить продукты",
                    description = "Молоко, хлеб, овощи",
                    creationDate = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1),
                    dueDate = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(2),
                    status = TaskStatus.COMPLETED,
                    completionDate = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(3),
                    priority = TaskPriority.LOW
                )
            }

            TaskCard(
                task = task,
                onTaskClick = {},
                onCompleteToggle = { _, _ -> },
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TaskCardWithSubtasksAndTagsPreview() {
    DHbtTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            val task = remember {
                Task(
                    id = "3",
                    title = "Подготовка к презентации",
                    description = "Подготовить слайды и текст выступления",
                    creationDate = System.currentTimeMillis(),
                    dueDate = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(3),
                    dueTime = "10:30",
                    priority = TaskPriority.HIGH,
                    subtasks = listOf(
                        Subtask(
                            id = "sub1",
                            taskId = "3",
                            title = "Подготовить слайды",
                            isCompleted = true
                        ),
                        Subtask(
                            id = "sub2",
                            taskId = "3",
                            title = "Написать текст",
                            isCompleted = false
                        ),
                        Subtask(
                            id = "sub3",
                            taskId = "3",
                            title = "Отрепетировать",
                            isCompleted = false
                        )
                    ),
                    tags = listOf(
                        Tag(
                            id = "tag1",
                            name = "Работа",
                            color = "#2196F3"
                        ),
                        Tag(
                            id = "tag2",
                            name = "Важное",
                            color = "#F44336"
                        ),
                        Tag(
                            id = "tag3",
                            name = "Квартальный отчет",
                            color = "#9C27B0"
                        )
                    )
                )
            }

            TaskCard(
                task = task,
                onTaskClick = {},
                onCompleteToggle = { _, _ -> },
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TaskCardOverduePreview() {
    DHbtTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            val task = remember {
                Task(
                    id = "4",
                    title = "Оплатить счета",
                    description = "Электричество, интернет, телефон",
                    creationDate = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(5),
                    dueDate = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1),
                    priority = TaskPriority.HIGH
                )
            }

            TaskCard(
                task = task,
                onTaskClick = {},
                onCompleteToggle = { _, _ -> },
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskCard(
    task: Task,
    onTaskClick: (String) -> Unit,
    onCompleteToggle: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val isCompleted = remember(task.status) { task.status == TaskStatus.COMPLETED }
    val dateFormat = remember { SimpleDateFormat("dd MMM", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Card(
        onClick = { onTaskClick(task.id) },
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Приоритетный индикатор или цветовая метка
            task.color?.let { colorString ->
                ColorIndicator(
                    colorString = colorString,
                    modifier = Modifier.padding(end = 12.dp)
                )
            } ?: run {
                // Отображение индикатора приоритета, если цвет не указан
                val priorityColor = when(task.priority) {
                    TaskPriority.LOW -> Color(0xFF4CAF50) // Зеленый
                    TaskPriority.MEDIUM -> Color(0xFFFFC107) // Желтый
                    TaskPriority.HIGH -> Color(0xFFF44336) // Красный
                }

                ColorIndicator(
                    colorString = null,
                    defaultColor = priorityColor,
                    modifier = Modifier.padding(end = 12.dp)
                )
            }

            // Основное содержимое
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Заголовок задачи
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else null,
                    color = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface
                )

                // Описание (если есть)
                task.description?.takeIf { it.isNotBlank() }?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Дата выполнения
                task.dueDate?.let { dueDate ->
                    Spacer(modifier = Modifier.height(4.dp))
                    val date = Date(dueDate)
                    val formattedDate = dateFormat.format(date)
                    val timeText = task.dueTime?.let { " · $it" } ?: ""

                    val isOverdue = dueDate < System.currentTimeMillis() && !isCompleted
                    val textColor = if (isOverdue) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant

                    Text(
                        text = "$formattedDate$timeText",
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor
                    )
                }

                // Подзадачи (если есть)
                val subtasksCount = task.subtasks.size
                if (subtasksCount > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    val completedSubtasks = task.subtasks.count { it.isCompleted }
                    Text(
                        text = "$completedSubtasks/$subtasksCount подзадач",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Теги (если есть)
                if (task.tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        task.tags.take(2).forEach { tag ->
                            SuggestionChip(
                                onClick = { },
                                label = { Text(tag.name) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = tag.color.toColor(MaterialTheme.colorScheme.secondaryContainer).copy(alpha = 0.5f)
                                )
                            )
                        }

                        if (task.tags.size > 2) {
                            SuggestionChip(
                                onClick = { },
                                label = { Text("+${task.tags.size - 2}") }
                            )
                        }
                    }
                }
            }

            // Чекбокс для отметки выполнения
            IconButton(
                onClick = { onCompleteToggle(task.id, !isCompleted) }
            ) {
                Icon(
                    imageVector = if (isCompleted) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                    contentDescription = if (isCompleted) "Отметить как невыполненную" else "Отметить как выполненную",
                    tint = if (isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}