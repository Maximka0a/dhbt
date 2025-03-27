package com.example.dhbt.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.*

@Composable
fun DatePickerDialog(
    initialDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    var currentMonth by remember { mutableStateOf(YearMonth.from(initialDate)) }
    var selectedDate by remember { mutableStateOf(initialDate) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .wrapContentSize()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .widthIn(max = 320.dp)
            ) {
                // Header with month navigation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        currentMonth = currentMonth.minusMonths(1)
                    }) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowLeft,
                            contentDescription = "Previous Month"
                        )
                    }

                    Text(
                        text = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${currentMonth.year}",
                        style = MaterialTheme.typography.titleMedium
                    )

                    IconButton(onClick = {
                        currentMonth = currentMonth.plusMonths(1)
                    }) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = "Next Month"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Days of week header
                Row(modifier = Modifier.fillMaxWidth()) {
                    val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                    daysOfWeek.forEach { day ->
                        Text(
                            text = day,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Calendar grid
                val firstDayOfMonth = currentMonth.atDay(1).dayOfWeek.value % 7 // 1-7 (Monday-Sunday)
                val daysInMonth = currentMonth.lengthOfMonth()
                val calendarItems = buildList {
                    // Add empty spaces for days before the first day of month
                    repeat(firstDayOfMonth - 1) {
                        add(CalendarDay(null, false))
                    }
                    // Add days of the month
                    for (day in 1..daysInMonth) {
                        val date = currentMonth.atDay(day)
                        add(CalendarDay(date, date.equals(selectedDate)))
                    }
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    state = rememberLazyGridState(),
                    modifier = Modifier.height(240.dp)
                ) {
                    items(calendarItems) { item ->
                        CalendarDayItem(
                            day = item,
                            onDaySelected = { date ->
                                selectedDate = date
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(onClick = {
                        onDateSelected(selectedDate)
                    }) {
                        Text("OK")
                    }
                }
            }
        }
    }
}

data class CalendarDay(
    val date: LocalDate?,
    val isSelected: Boolean
)

@Composable
fun CalendarDayItem(
    day: CalendarDay,
    onDaySelected: (LocalDate) -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(4.dp)
            .clip(CircleShape)
            .background(
                if (day.isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surface
            )
            .then(
                if (day.date != null) {
                    Modifier.clickable { onDaySelected(day.date) }
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        day.date?.let { date ->
            Text(
                text = date.dayOfMonth.toString(),
                color = if (day.isSelected) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}