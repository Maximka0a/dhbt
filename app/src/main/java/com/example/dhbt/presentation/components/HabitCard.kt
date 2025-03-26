package com.example.dhbt.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.dhbt.R
import com.example.dhbt.domain.model.FrequencyType
import com.example.dhbt.domain.model.Habit
import com.example.dhbt.domain.model.HabitFrequency
import com.example.dhbt.domain.model.HabitStatus
import com.example.dhbt.domain.model.HabitType
import com.example.dhbt.presentation.shared.EmojiIcon
import com.example.dhbt.presentation.theme.DHbtTheme
import com.example.dhbt.presentation.util.toColor

@Preview(showBackground = true)
@Composable
fun HabitCardBinaryPreview() {
    DHbtTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            val habit = remember {
                Habit(
                    id = "1",
                    title = "Ежедневная медитация",
                    description = "10 минут медитации каждый день",
                    iconEmoji = "🧘",
                    color = "#9C27B0",
                    creationDate = System.currentTimeMillis(),
                    type = HabitType.BINARY,
                    currentStreak = 3,
                    bestStreak = 7
                )
            }

            HabitCard(
                habit = habit,
                onHabitClick = {},
                onProgressIncrement = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HabitCardQuantityPreview() {
    DHbtTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            val habit = remember {
                Habit(
                    id = "2",
                    title = "Выпивать воду",
                    description = "8 стаканов воды в день",
                    iconEmoji = "💧",
                    color = "#2196F3",
                    creationDate = System.currentTimeMillis(),
                    type = HabitType.QUANTITY,
                    targetValue = 8f,
                    currentStreak = 5,
                    unitOfMeasurement = "стаканов",
                    bestStreak = 10,
                )
            }

            HabitCard(
                habit = habit,
                onHabitClick = {},
                onProgressIncrement = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HabitCardTimePreview() {
    DHbtTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            val habit = remember {
                Habit(
                    id = "3",
                    title = "Чтение книг",
                    description = "Ежедневное чтение 30 минут",
                    iconEmoji = "📚",
                    color = "#FF9800",
                    creationDate = System.currentTimeMillis(),
                    type = HabitType.TIME,
                    targetValue = 30f,
                    currentStreak = 15,
                    bestStreak = 20
                )
            }

            HabitCard(
                habit = habit,
                onHabitClick = {},
                onProgressIncrement = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitCard(
    habit: Habit,
    onHabitClick: (String) -> Unit,
    onProgressIncrement: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val habitColor = habit.color.toColor(MaterialTheme.colorScheme.primary)

    Card(
        onClick = { onHabitClick(habit.id) },
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (habit.status == HabitStatus.ARCHIVED)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Заголовок и иконка
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                EmojiIcon(
                    emoji = habit.iconEmoji,
                    backgroundColor = habitColor
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = habit.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (habit.currentStreak > 0) {
                        Text(
                            text = stringResource(id = R.string.streak, habit.currentStreak),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Кнопка быстрого увеличения прогресса для количественных привычек
                if (habit.type == HabitType.QUANTITY) {
                    IconButton(onClick = { onProgressIncrement(habit.id) }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Увеличить прогресс"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Прогресс-бар
            val progress = remember(habit) {
                when (habit.type) {
                    HabitType.BINARY ->
                        if (habit.status == HabitStatus.ACTIVE) 1f else 0f
                    HabitType.QUANTITY, HabitType.TIME ->
                        habit.targetValue?.let { target ->
                            if (target > 0f) {
                                (habit.currentStreak.toFloat() / target).coerceIn(0f, 1f)
                            } else 0f
                        } ?: 0f
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = habitColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Информация о прогрессе
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Отображение текущего прогресса в зависимости от типа привычки
                when (habit.type) {
                    HabitType.BINARY -> {
                        val statusText = if (habit.status == HabitStatus.ACTIVE) "Выполнено" else "Не выполнено"
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    HabitType.QUANTITY -> {
                        val current = habit.currentStreak
                        val target = habit.targetValue?.toInt() ?: 0
                        Text(
                            text = "$current/$target ${habit.unitOfMeasurement ?: ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    HabitType.TIME -> {
                        val duration = habit.currentStreak
                        Text(
                            text = "$duration мин",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Отображение лучшей серии (если доступно)
                if (habit.bestStreak > 0) {
                    Text(
                        text = stringResource(id = R.string.best_streak, habit.bestStreak),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

        }
    }
}

@Composable
private fun formatFrequency(frequency: HabitFrequency): String {
    return when (frequency.type) {
        FrequencyType.DAILY -> "Ежедневно"
        FrequencyType.SPECIFIC_DAYS -> {
            val days = frequency.daysOfWeek?.map { getDayOfWeekAbbr(it) }?.joinToString(", ") ?: ""
            "По дням: $days"
        }
        FrequencyType.TIMES_PER_WEEK ->
            "${frequency.timesPerPeriod ?: 0} раз в неделю"
        FrequencyType.TIMES_PER_MONTH ->
            "${frequency.timesPerPeriod ?: 0} раз в месяц"
    }
}

@Composable
private fun getDayOfWeekAbbr(dayIndex: Int): String {
    return when (dayIndex) {
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