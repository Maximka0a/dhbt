package com.example.dhbt.presentation.task.detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.dhbt.R
import com.example.dhbt.domain.model.Category
import com.example.dhbt.domain.model.TaskPriority
import com.example.dhbt.presentation.task.edit.components.parseColor
import com.example.dhbt.presentation.util.toLocalDate
import com.example.dhbt.presentation.util.toLocalTime
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.FormatStyle
import java.util.*

@Composable
fun TaskInfoSection(
    dueDate: Long?,
    dueTime: String?,
    priority: TaskPriority?,
    category: Category?,
    eisenhowerQuadrant: Int?,
    isArchived: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = stringResource(R.string.task_details),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Индикация архивного статуса
        if (isArchived) {
            InfoItem(
                icon = Icons.Rounded.Archive,
                title = stringResource(R.string.status),
                content = stringResource(R.string.archived),
                contentColor = MaterialTheme.colorScheme.tertiary
            )
        }

        // Сроки выполнения
        if (dueDate != null) {
            val date = dueDate.toLocalDate()
            val formattedDate = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL))

            InfoItem(
                icon = Icons.Rounded.CalendarToday,
                title = stringResource(R.string.due_date),
                content = formattedDate
            )
        }

        // Время выполнения
        if (dueTime != null) {
            val time = dueTime.toLocalTime() ?: return@Column
            val formattedTime = time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))

            InfoItem(
                icon = Icons.Rounded.Schedule,
                title = stringResource(R.string.due_time),
                content = formattedTime
            )
        }

        // Приоритет
        if (priority != null) {
            val (color, label) = when (priority) {
                TaskPriority.HIGH -> Pair(Color(0xFFF44336), stringResource(R.string.priority_high))
                TaskPriority.MEDIUM -> Pair(Color(0xFFFF9800), stringResource(R.string.priority_medium))
                TaskPriority.LOW -> Pair(Color(0xFF4CAF50), stringResource(R.string.priority_low))
            }

            InfoItem(
                icon = Icons.Rounded.PriorityHigh,
                title = stringResource(R.string.priority),
                content = label,
                contentColor = color
            )
        }

        // Категория
        if (category != null) {
            val categoryColor = parseColor(category.color, MaterialTheme.colorScheme.primary)


            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Folder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.category),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(categoryColor)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Матрица Эйзенхауэра
        if (eisenhowerQuadrant != null && eisenhowerQuadrant in 1..4) {
            val (title, description, color) = when (eisenhowerQuadrant) {
                1 -> Triple(
                    stringResource(R.string.quadrant1),
                    stringResource(R.string.do_first),
                    Color(0xFFF44336) // Красный
                )
                2 -> Triple(
                    stringResource(R.string.quadrant2),
                    stringResource(R.string.schedule),
                    Color(0xFF4CAF50) // Зеленый
                )
                3 -> Triple(
                    stringResource(R.string.quadrant3),
                    stringResource(R.string.delegate),
                    Color(0xFFFF9800) // Оранжевый
                )
                4 -> Triple(
                    stringResource(R.string.quadrant4),
                    stringResource(R.string.eliminate),
                    Color(0xFF2196F3) // Синий
                )
                else -> Triple("", "", Color.Gray)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.GridView,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.eisenhower_matrix),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Surface(
                        color = color.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = color,
                                modifier = Modifier.weight(1f)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = "• $description",
                                style = MaterialTheme.typography.bodySmall,
                                color = color
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InfoItem(
    icon: ImageVector,
    title: String,
    content: String,
    contentColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}