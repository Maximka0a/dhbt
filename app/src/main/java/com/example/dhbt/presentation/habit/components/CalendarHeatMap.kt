package com.example.dhbt.presentation.habit.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.*

@Composable
fun CalendarHeatMap(
    data: Map<LocalDate, Float>,
    startDate: LocalDate,
    endDate: LocalDate,
    colorEmpty: Color,
    colorFilled: Color,
    modifier: Modifier = Modifier
) {
    val weeks = generateWeeksList(startDate, endDate)

    Column(modifier = modifier) {
        // Дни недели (заголовок)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.End
        ) {
            // Пустая ячейка для отступа
            Spacer(modifier = Modifier.width(20.dp))

            // Дни недели
            for (day in DayOfWeek.values()) {
                if (day == DayOfWeek.SUNDAY || day == DayOfWeek.WEDNESDAY) {
                    Box(
                        modifier = Modifier.width(16.dp)
                    ) {
                        Text(
                            text = day.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(16.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(16.dp))
                }
            }
        }

        // Сетка календаря
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            weeks.forEach { week ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Отображаем месяц, если первый день недели - первый день месяца
                    val firstDayOfWeek = week.first()
                    if (firstDayOfWeek.dayOfMonth == 1) {
                        Text(
                            text = firstDayOfWeek.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp,
                            modifier = Modifier.width(20.dp)
                        )
                    } else {
                        Spacer(modifier = Modifier.width(20.dp))
                    }

                    // Дни недели
                    for (date in week) {
                        val value = data[date] ?: 0f
                        val color = if (value > 0f) {
                            colorFilled.copy(alpha = 0.3f + value * 0.7f)
                        } else {
                            colorEmpty
                        }

                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(color)
                        )
                    }
                }
            }
        }
    }
}

// Функция для формирования списка недель для календаря
private fun generateWeeksList(startDate: LocalDate, endDate: LocalDate): List<List<LocalDate>> {
    val weeks = mutableListOf<List<LocalDate>>()
    var currentDate = startDate

    while (!currentDate.isAfter(endDate)) {
        val week = mutableListOf<LocalDate>()

        // Заполняем неделю начиная с воскресенья
        val firstDayOfWeek = currentDate.with(DayOfWeek.SUNDAY)
        for (i in 0 until 7) {
            val date = firstDayOfWeek.plusDays(i.toLong())
            if (!date.isBefore(startDate) && !date.isAfter(endDate)) {
                week.add(date)
            }
        }

        if (week.isNotEmpty()) {
            weeks.add(week)
        }

        currentDate = currentDate.plusWeeks(1).with(DayOfWeek.SUNDAY)
    }

    return weeks
}