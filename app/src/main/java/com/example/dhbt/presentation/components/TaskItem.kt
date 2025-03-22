package com.example.dhbt.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.dhbt.R
import com.example.dhbt.domain.model.Task
import com.example.dhbt.domain.model.TaskPriority
import com.example.dhbt.domain.model.TaskStatus
import org.threeten.bp.Instant
import org.threeten.bp.LocalDate
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.FormatStyle

@Composable
fun TaskItem(
    task: Task,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showCheckbox: Boolean = false,
    onCheckChanged: ((Boolean) -> Unit)? = null
) {
    val isCompleted = task.status == TaskStatus.COMPLETED

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Цветовой индикатор категории или приоритета
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(getTaskColor(task))
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Информация о задаче
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Заголовок задачи
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Дополнительная информация (только если есть)
                if (task.dueDate != null || task.description != null) {
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Если есть срок выполнения
                        task.dueDate?.let { dueDate ->
                            val formattedDate = formatDueDate(dueDate)

                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.width(4.dp))

                            Text(
                                text = formattedDate,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        // Индикатор приоритета
                        if (task.priority == TaskPriority.HIGH) {
                            Icon(
                                imageVector = Icons.Default.Flag,
                                contentDescription = stringResource(R.string.high_priority),
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFFE57373) // Красный цвет для высокого приоритета
                            )
                        }

                        // Отображение описания, если оно есть и срок не указан
                        if (task.description != null && task.dueDate == null) {
                            Text(
                                text = task.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Чекбокс для изменения статуса задачи (опционально)
            if (showCheckbox && onCheckChanged != null) {
                Checkbox(
                    checked = isCompleted,
                    onCheckedChange = onCheckChanged
                )
            }
        }
    }
}

@Composable
private fun getTaskColor(task: Task): Color {
    return task.color?.let { colorString ->
        try {
            Color(android.graphics.Color.parseColor(colorString))
        } catch (e: Exception) {
            // Если не удалось распарсить цвет или он не указан, используем цвет по умолчанию
            when (task.priority) {
                TaskPriority.HIGH -> Color(0xFFE57373) // Красный для высокого приоритета
                TaskPriority.MEDIUM -> Color(0xFFFFB74D) // Оранжевый для среднего приоритета
                TaskPriority.LOW -> Color(0xFF81C784) // Зеленый для низкого приоритета
            }
        }
    } ?: when (task.priority) {
        TaskPriority.HIGH -> Color(0xFFE57373) // Красный для высокого приоритета
        TaskPriority.MEDIUM -> Color(0xFFFFB74D) // Оранжевый для среднего приоритета
        TaskPriority.LOW -> Color(0xFF81C784) // Зеленый для низкого приоритета
    }
}

private fun formatDueDate(dueDateMillis: Long): String {
    val dueDate = Instant.ofEpochMilli(dueDateMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()

    val today = LocalDate.now()
    val tomorrow = today.plusDays(1)
    val yesterday = today.minusDays(1)

    return when {
        dueDate.isEqual(today) -> "Сегодня"
        dueDate.isEqual(tomorrow) -> "Завтра"
        dueDate.isEqual(yesterday) -> "Вчера"
        else -> dueDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT))
    }
}