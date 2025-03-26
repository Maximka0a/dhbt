package com.example.dhbt.presentation.habit.edit.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.LocalTime

@Composable
fun HabitReminderSection(
    reminderEnabled: Boolean,
    onReminderEnabledChange: (Boolean) -> Unit,
    reminderTime: LocalTime,
    onReminderTimeClick: () -> Unit,
    reminderDays: Set<DayOfWeek>,
    onReminderDaysChange: (Set<DayOfWeek>) -> Unit,
    habitColor: Color
) {
    SectionTitle(title = "Напоминания", icon = Icons.Default.Notifications)

    SectionCard {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Включить напоминания",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )

                Switch(
                    checked = reminderEnabled,
                    onCheckedChange = onReminderEnabledChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = habitColor,
                        checkedTrackColor = habitColor.copy(alpha = 0.5f),
                        checkedIconColor = Color.White
                    )
                )
            }

            AnimatedVisibility(visible = reminderEnabled) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    // Выбор времени для напоминания
                    ReminderTimeSelector(
                        time = reminderTime,
                        onClick = onReminderTimeClick,
                        habitColor = habitColor
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Выбор дней для напоминаний
                    Text(
                        text = "В какие дни напоминать:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    DaysOfWeekSelector(
                        selectedDays = reminderDays,
                        onDaysChanged = onReminderDaysChange,
                        habitColor = habitColor
                    )
                }
            }
        }
    }
}
