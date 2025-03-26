package com.example.dhbt.presentation.task.detail.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.dhbt.R
import com.example.dhbt.domain.model.RecurrenceType
import com.example.dhbt.domain.model.TaskRecurrence
import java.util.*

@Composable
fun TaskAdditionalInfoSection(
    duration: Int?,
    estimatedPomodoros: Int?,
    totalFocusTime: Int?,
    recurrence: TaskRecurrence?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = stringResource(R.string.additional_info),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Длительность
        if (duration != null && duration > 0) {
            InfoRow(
                label = stringResource(R.string.duration),
                value = formatDuration(duration)
            )
        }

        // Оценка в помидорах - текстовое представление вместо иконок
        if (estimatedPomodoros != null && estimatedPomodoros > 0) {
            InfoRow(
                label = stringResource(R.string.estimated_pomodoros),
                value = stringResource(R.string.pomodoro_count, estimatedPomodoros)
            )
        }

        // Фокус-время
        if (totalFocusTime != null && totalFocusTime > 0) {
            InfoRow(
                label = stringResource(R.string.total_focus_time),
                value = formatDuration(totalFocusTime)
            )
        }

        // Повторение
        if (recurrence != null) {
            RecurrenceRow(recurrence = recurrence)
        }

        // Кнопка для запуска Pomodoro-таймера
        if (estimatedPomodoros != null && estimatedPomodoros > 0) {
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { /* Действие происходит через FAB */ },
                modifier = Modifier.align(Alignment.CenterHorizontally),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_pomodoro),
                    contentDescription = null,
                    modifier = Modifier
                        .size(18.dp)
                        .padding(end = 8.dp)
                )
                Text(stringResource(R.string.start_pomodoro_session))
            }
        }
    }
}

@Composable
fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f)
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.6f)
        )
    }
}

@Composable
fun EstimatedPomodorosRow(
    count: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "${stringResource(R.string.estimated_pomodoros)}:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(0.6f)
        ) {
            repeat(count) {
                Icon(
                    painter = painterResource(R.drawable.ic_pomodoro),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }

            Text(
                text = " ($count)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun RecurrenceRow(
    recurrence: TaskRecurrence,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = "${stringResource(R.string.recurrence)}:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f)
        )

        Text(
            text = formatRecurrence(recurrence),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.6f)
        )
    }
}

// Вспомогательные функции
fun formatDuration(minutes: Int): String {
    val hours = minutes / 60
    val mins = minutes % 60

    return when {
        hours > 0 && mins > 0 -> "$hours ч $mins мин"
        hours > 0 -> "$hours ч"
        else -> "$mins мин"
    }
}

fun formatRecurrence(recurrence: TaskRecurrence): String {
    return when (recurrence.type) {
        RecurrenceType.DAILY -> "Ежедневно"
        RecurrenceType.WEEKLY -> {
            val days = recurrence.daysOfWeek
            if (days.isNullOrEmpty()) "Еженедельно" else {
                val dayNames = days.map { getDayName(it) }
                "Еженедельно (${dayNames.joinToString(", ")})"
            }
        }
        RecurrenceType.MONTHLY -> {
            recurrence.monthDay?.let { "Ежемесячно (День $it)" } ?: "Ежемесячно"
        }
        RecurrenceType.YEARLY -> "Ежегодно"
        RecurrenceType.CUSTOM -> {
            recurrence.customInterval?.let { "Каждые $it дней" } ?: "Пользовательское"
        }
    }
}

fun getDayName(dayOfWeek: Int): String {
    return when (dayOfWeek) {
        1 -> "Пн"
        2 -> "Вт"
        3 -> "Ср"
        4 -> "Чт"
        5 -> "Пт"
        6 -> "Сб"
        7 -> "Вс"
        else -> ""
    }
}