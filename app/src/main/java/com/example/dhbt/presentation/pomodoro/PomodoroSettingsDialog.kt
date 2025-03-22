package com.example.dhbt.presentation.pomodoro

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.dhbt.R
import com.example.dhbt.domain.model.PomodoroPreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PomodoroSettingsDialog(
    pomodoroPreferences: PomodoroPreferences,
    onDismiss: () -> Unit,
    onSaveSettings: (PomodoroPreferences) -> Unit
) {
    var workDuration by remember { mutableStateOf(pomodoroPreferences.workDuration) }
    var shortBreakDuration by remember { mutableStateOf(pomodoroPreferences.shortBreakDuration) }
    var longBreakDuration by remember { mutableStateOf(pomodoroPreferences.longBreakDuration) }
    var cyclesBeforeLongBreak by remember { mutableStateOf(pomodoroPreferences.pomodorosUntilLongBreak) }
    var autoStartBreaks by remember { mutableStateOf(pomodoroPreferences.autoStartBreaks) }
    var autoStartPomodoros by remember { mutableStateOf(pomodoroPreferences.autoStartPomodoros) }
    var soundEnabled by remember { mutableStateOf(pomodoroPreferences.soundEnabled) }
    var vibrationEnabled by remember { mutableStateOf(pomodoroPreferences.vibrationEnabled) }
    var keepScreenOn by remember { mutableStateOf(pomodoroPreferences.keepScreenOn) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.pomodoro_settings),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Длительность работы
                Text(
                    text = stringResource(R.string.work_duration),
                    style = MaterialTheme.typography.bodyLarge
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$workDuration ${stringResource(R.string.minutes_short)}",
                        modifier = Modifier.width(60.dp)
                    )

                    Slider(
                        value = workDuration.toFloat(),
                        onValueChange = { workDuration = it.toInt() },
                        valueRange = 1f..60f,
                        steps = 59,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Длительность короткого перерыва
                Text(
                    text = stringResource(R.string.short_break_duration),
                    style = MaterialTheme.typography.bodyLarge
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$shortBreakDuration ${stringResource(R.string.minutes_short)}",
                        modifier = Modifier.width(60.dp)
                    )

                    Slider(
                        value = shortBreakDuration.toFloat(),
                        onValueChange = { shortBreakDuration = it.toInt() },
                        valueRange = 1f..15f,
                        steps = 14,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Длительность длинного перерыва
                Text(
                    text = stringResource(R.string.long_break_duration),
                    style = MaterialTheme.typography.bodyLarge
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$longBreakDuration ${stringResource(R.string.minutes_short)}",
                        modifier = Modifier.width(60.dp)
                    )

                    Slider(
                        value = longBreakDuration.toFloat(),
                        onValueChange = { longBreakDuration = it.toInt() },
                        valueRange = 5f..30f,
                        steps = 25,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Циклов до длинного перерыва
                Text(
                    text = stringResource(R.string.cycles_before_long_break),
                    style = MaterialTheme.typography.bodyLarge
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = cyclesBeforeLongBreak.toString(),
                        modifier = Modifier.width(60.dp)
                    )

                    Slider(
                        value = cyclesBeforeLongBreak.toFloat(),
                        onValueChange = { cyclesBeforeLongBreak = it.toInt() },
                        valueRange = 2f..6f,
                        steps = 4,
                        modifier = Modifier.weight(1f)
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Настройки автоматического запуска
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.auto_start_breaks),
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Switch(
                        checked = autoStartBreaks,
                        onCheckedChange = { autoStartBreaks = it }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.auto_start_pomodoros),
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Switch(
                        checked = autoStartPomodoros,
                        onCheckedChange = { autoStartPomodoros = it }
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Настройки уведомлений
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.sound_enabled),
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Switch(
                        checked = soundEnabled,
                        onCheckedChange = { soundEnabled = it }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.vibration_enabled),
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Switch(
                        checked = vibrationEnabled,
                        onCheckedChange = { vibrationEnabled = it }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.keep_screen_on),
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Switch(
                        checked = keepScreenOn,
                        onCheckedChange = { keepScreenOn = it }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSaveSettings(
                        PomodoroPreferences(
                            workDuration = workDuration,
                            shortBreakDuration = shortBreakDuration,
                            longBreakDuration = longBreakDuration,
                            pomodorosUntilLongBreak = cyclesBeforeLongBreak,
                            autoStartBreaks = autoStartBreaks,
                            autoStartPomodoros = autoStartPomodoros,
                            soundEnabled = soundEnabled,
                            vibrationEnabled = vibrationEnabled,
                            keepScreenOn = keepScreenOn
                        )
                    )
                    onDismiss()
                }
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}