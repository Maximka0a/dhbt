package com.example.dhbt.presentation.habit.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.dhbt.presentation.habit.list.HabitWithProgress
import com.kizitonwose.calendar.compose.WeekCalendar
import com.kizitonwose.calendar.compose.weekcalendar.rememberWeekCalendarState
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

@Composable
fun HabitCalendarView(
    selectedDate: LocalDate,
    habits: List<HabitWithProgress>,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentMonth = remember(selectedDate) {
        YearMonth.from(selectedDate)
    }
    val startDate = remember { LocalDate.now().minusMonths(12) }
    val endDate = remember { LocalDate.now().plusMonths(12) }
    val firstDayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek

    // Создаем состояние календаря
    val calendarState = rememberWeekCalendarState(
        startDate = startDate,
        endDate = endDate,
        firstDayOfWeek = firstDayOfWeek,
        firstVisibleWeekDate = selectedDate
    )

    // Получаем заголовок с месяцем из текущей недели
    val currentWeek = remember(calendarState.firstVisibleWeek) {
        calendarState.firstVisibleWeek
    }

    // Форматирование заголовка месяца-года
    val monthYearFormatter = remember { DateTimeFormatter.ofPattern("LLLL yyyy") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(8.dp),
    ) {
        // Заголовок с месяцем и кнопками навигации
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    val previousWeek = selectedDate.minusWeeks(1)
                    onDateSelected(previousWeek)
                }
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Предыдущая неделя"
                )
            }

            // Получаем первую и последнюю дату недели для заголовка
            val firstDate = currentWeek.days.first().date
            val lastDate = currentWeek.days.last().date

            // Формируем заголовок в зависимости от того, охватывает ли неделя один или два месяца
            val monthYearText = if (firstDate.month == lastDate.month) {
                firstDate.format(monthYearFormatter)
            } else {
                "${firstDate.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} - " +
                        "${lastDate.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${lastDate.year}"
            }

            Text(
                text = monthYearText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            IconButton(
                onClick = {
                    val nextWeek = selectedDate.plusWeeks(1)
                    onDateSelected(nextWeek)
                }
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Следующая неделя"
                )
            }
        }

        // Календарь недели с индикаторами прогресса
        WeekCalendar(
            state = calendarState,
            dayContent = { day ->
                // Получаем дату из объекта CalendarDay
                val date = day.date
                val isSelected = date == selectedDate
                val isToday = date == LocalDate.now()

                // В WeekCalendar все дни имеют позицию WeekDayPosition.Day, поэтому не нужно особой проверки
                val isCurrentDay = true // В недельном календаре все дни считаем активными

                // Находим привычки для этой даты и подсчитываем прогресс
                val startOfDay = date.atStartOfDay()
                val dateMillis = startOfDay.toInstant(java.time.ZoneOffset.UTC).toEpochMilli()

                val habitsForDate = habits.filter { habit ->
                    val tracking = habit.todayTracking
                    tracking != null && tracking.date / (24 * 60 * 60 * 1000) == dateMillis / (24 * 60 * 60 * 1000)
                }

                val completedHabits = habitsForDate.count { it.isCompletedToday }
                val totalHabits = habits.size
                val hasProgress = completedHabits > 0 && totalHabits > 0
                val progressRatio = if (totalHabits > 0) completedHabits.toFloat() / totalHabits else 0f

                // Определяем цвет прогресса
                val progressColor = when {
                    progressRatio >= 1f -> MaterialTheme.colorScheme.primary
                    progressRatio > 0.5f -> MaterialTheme.colorScheme.secondary
                    progressRatio > 0f -> MaterialTheme.colorScheme.tertiary
                    else -> Color.Transparent
                }

                // Внешний контейнер для дня
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isSelected -> MaterialTheme.colorScheme.primaryContainer
                                isToday -> MaterialTheme.colorScheme.surfaceVariant
                                else -> Color.Transparent
                            }
                        )
                        .border(
                            width = if (isToday && !isSelected) 1.dp else 0.dp,
                            color = if (isToday && !isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = CircleShape
                        )
                        .clickable { onDateSelected(date) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // День недели
                        Text(
                            text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onSurface
                        )

                        // Число месяца
                        Text(
                            text = date.dayOfMonth.toString(),
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = if (isSelected)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onSurface
                        )

                        // Индикатор прогресса
                        if (hasProgress) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(progressColor)
                            )
                        }
                    }
                }
            },
            // Отключаем стандартный заголовок недели
            weekHeader = {},
            modifier = Modifier.fillMaxWidth()
        )

        // Легенда для индикаторов прогресса
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProgressLegendItem(
                color = MaterialTheme.colorScheme.primary,
                text = "Все выполнено"
            )

            Spacer(modifier = Modifier.width(16.dp))

            ProgressLegendItem(
                color = MaterialTheme.colorScheme.secondary,
                text = "Частично выполнено"
            )
        }
    }
}

@Composable
fun ProgressLegendItem(
    color: Color,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )

        Spacer(modifier = Modifier.width(4.dp))

        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}