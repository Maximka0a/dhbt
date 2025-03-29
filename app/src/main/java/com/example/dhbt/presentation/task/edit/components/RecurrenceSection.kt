package com.example.dhbt.presentation.task.edit.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.dhbt.R
import com.example.dhbt.domain.model.RecurrenceType

@Composable
fun RecurrenceSection(
    selectedType: RecurrenceType?,
    selectedDays: Set<Int>,
    monthDay: Int?,
    customInterval: Int?,
    onTypeSelected: (RecurrenceType?) -> Unit,
    onDayToggled: (Int) -> Unit,
    onMonthDayChanged: (Int?) -> Unit,
    onCustomIntervalChanged: (Int?) -> Unit
) {
    var expanded by remember { mutableStateOf(selectedType != null) }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Заголовок с возможностью раскрытия/скрытия
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.recurrence),
                style = MaterialTheme.typography.titleMedium
            )

            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null
            )
        }

        // Содержимое раздела
        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                // Выбор типа повторения
                Text(
                    text = stringResource(R.string.repeat), // Изменено на доступный ресурс
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                RecurrenceTypeSelector(
                    selectedType = selectedType,
                    onTypeSelected = onTypeSelected
                )

                // Дополнительные настройки в зависимости от типа
                AnimatedVisibility(visible = selectedType != null) {
                    Column(modifier = Modifier.padding(top = 16.dp)) {
                        when (selectedType) {
                            RecurrenceType.WEEKLY -> {
                                DaysOfWeekSelector( // Исправлено на правильное имя
                                    selectedDays = selectedDays,
                                    onDayToggled = onDayToggled
                                )
                            }
                            RecurrenceType.MONTHLY -> {
                                MonthDaySelector(
                                    selectedDay = monthDay,
                                    onDayChanged = onMonthDayChanged
                                )
                            }
                            RecurrenceType.CUSTOM -> {
                                CustomIntervalInput( // Исправлено на правильное имя
                                    interval = customInterval,
                                    onIntervalChanged = onCustomIntervalChanged
                                )
                            }
                            else -> { /* Для других типов доп. настройки не требуются */ }
                        }
                    }
                }

                // Предупреждение о потенциальных проблемах
                if (selectedType != null) {
                    Text(
                        text = stringResource(R.string.warning), // Изменено на доступный ресурс
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}
@Composable
fun RecurrenceTypeSelector(
    selectedType: RecurrenceType?,
    onTypeSelected: (RecurrenceType?) -> Unit
) {
    val recurrenceTypes = listOf(
        RecurrenceInfo(null, Icons.Rounded.DoNotDisturbAlt, stringResource(R.string.none)),
        RecurrenceInfo(
            RecurrenceType.DAILY,
            Icons.Rounded.Today,
            stringResource(R.string.daily)
        ),
        RecurrenceInfo(
            RecurrenceType.WEEKLY,
            Icons.Rounded.ViewWeek,
            stringResource(R.string.weekly)
        ),
        RecurrenceInfo(
            RecurrenceType.MONTHLY,
            Icons.Rounded.CalendarMonth,
            stringResource(R.string.monthly)
        ),
        RecurrenceInfo(
            RecurrenceType.YEARLY,
            Icons.Rounded.CalendarToday,
            stringResource(R.string.yearly)
        ),
        RecurrenceInfo(
            RecurrenceType.CUSTOM,
            Icons.Rounded.MoreTime,
            stringResource(R.string.custom)
        )
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(recurrenceTypes) { recurrenceInfo ->
            val isSelected = selectedType == recurrenceInfo.type

            Surface(
                modifier = Modifier
                    .height(38.dp)
                    .clickable { onTypeSelected(recurrenceInfo.type) },
                shape = RoundedCornerShape(50),
                color = if (isSelected)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = recurrenceInfo.icon,
                        contentDescription = null,
                        tint = if (isSelected)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )

                    Text(
                        text = recurrenceInfo.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isSelected)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun DaysOfWeekSelector(
    selectedDays: Set<Int>,
    onDayToggled: (Int) -> Unit
) {
    // Вызываем stringResource напрямую в Composable функции
    val mondayLabel = stringResource(R.string.monday_short)
    val tuesdayLabel = stringResource(R.string.tuesday_short)
    val wednesdayLabel = stringResource(R.string.wednesday_short)
    val thursdayLabel = stringResource(R.string.thursday_short)
    val fridayLabel = stringResource(R.string.friday_short)
    val saturdayLabel = stringResource(R.string.saturday_short)
    val sundayLabel = stringResource(R.string.sunday_short)

    // Используем remember с уже полученными строками
    val daysOfWeek = remember {
        listOf(
            Pair(1, mondayLabel),
            Pair(2, tuesdayLabel),
            Pair(3, wednesdayLabel),
            Pair(4, thursdayLabel),
            Pair(5, fridayLabel),
            Pair(6, saturdayLabel),
            Pair(7, sundayLabel)
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        daysOfWeek.forEach { (dayValue, dayLabel) ->
            val isSelected = selectedDays.contains(dayValue)

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                    .border(
                        width = 1.dp,
                        color = if (isSelected)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        shape = CircleShape
                    )
                    .clickable { onDayToggled(dayValue) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = dayLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun MonthDaySelector(
    selectedDay: Int?,
    onDayChanged: (Int?) -> Unit
) {
    OutlinedTextField(
        value = selectedDay?.toString() ?: "",
        onValueChange = {
            val day = it.toIntOrNull()
            if (it.isEmpty()) {
                onDayChanged(null)
            } else if (day != null && day in 1..31) {
                onDayChanged(day)
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        label = { Text(stringResource(R.string.day_of_month)) },
        placeholder = { Text(stringResource(R.string.enter_day_1_31)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        supportingText = { Text(stringResource(R.string.valid_values_1_31)) }
    )
}

@Composable
fun CustomIntervalInput(
    interval: Int?,
    onIntervalChanged: (Int?) -> Unit
) {
    OutlinedTextField(
        value = interval?.toString() ?: "",
        onValueChange = {
            val customInterval = it.toIntOrNull()
            if (it.isEmpty()) {
                onIntervalChanged(null)
            } else if (customInterval != null && customInterval > 0) {
                onIntervalChanged(customInterval)
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        label = { Text(stringResource(R.string.days_interval)) },
        placeholder = { Text(stringResource(R.string.enter_days_count)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        supportingText = { Text(stringResource(R.string.every_x_days_explanation)) }
    )
}

// Вспомогательные классы и компоненты
data class RecurrenceInfo(
    val type: RecurrenceType?,
    val icon: ImageVector,
    val label: String
)