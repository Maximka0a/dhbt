package com.example.dhbt.presentation.pomodoro

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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

    // Section expansion states
    var timerSectionExpanded by remember { mutableStateOf(true) }
    var autoStartSectionExpanded by remember { mutableStateOf(true) }
    var notificationSectionExpanded by remember { mutableStateOf(true) }

    // Dialog with custom shape and width (almost full width)
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth(0.98f) // Увеличиваем ширину диалога почти на всю ширину экрана
            .fillMaxHeight(0.9f)
            .clip(RoundedCornerShape(20.dp)),
        containerColor = MaterialTheme.colorScheme.surface,
        title = null,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                // Header with title and close button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.pomodoro_settings),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Закрыть",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Content area with scrolling
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp), // Увеличиваем горизонтальные отступы
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Timer settings section
                    SettingsSectionHeader(
                        title = "Длительность",
                        icon = Icons.Rounded.Timer,
                        expanded = timerSectionExpanded,
                        onExpandToggle = { timerSectionExpanded = !timerSectionExpanded }
                    )

                    AnimatedVisibility(
                        visible = timerSectionExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Work duration
                            DurationSliderItem(
                                icon = Icons.Rounded.Work,
                                title = stringResource(R.string.work_duration),
                                value = workDuration,
                                onValueChange = { workDuration = it },
                                valueRange = 1f..60f,
                                steps = 59,
                                valueText = "$workDuration мин",
                                color = MaterialTheme.colorScheme.primary
                            )

                            // Short break duration
                            DurationSliderItem(
                                icon = Icons.Rounded.Coffee,
                                title = stringResource(R.string.short_break_duration),
                                value = shortBreakDuration,
                                onValueChange = { shortBreakDuration = it },
                                valueRange = 1f..15f,
                                steps = 14,
                                valueText = "$shortBreakDuration мин",
                                color = MaterialTheme.colorScheme.secondary
                            )

                            // Long break duration
                            DurationSliderItem(
                                icon = Icons.Rounded.Weekend,
                                title = stringResource(R.string.long_break_duration),
                                value = longBreakDuration,
                                onValueChange = { longBreakDuration = it },
                                valueRange = 5f..30f,
                                steps = 25,
                                valueText = "$longBreakDuration мин",
                                color = MaterialTheme.colorScheme.tertiary
                            )

                            // Cycles before long break
                            DurationSliderItem(
                                icon = Icons.Rounded.Repeat,
                                title = stringResource(R.string.cycles_before_long_break),
                                value = cyclesBeforeLongBreak,
                                onValueChange = { cyclesBeforeLongBreak = it },
                                valueRange = 2f..6f,
                                steps = 4,
                                valueText = cyclesBeforeLongBreak.toString(),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 4.dp))

                    // Auto start section
                    SettingsSectionHeader(
                        title = "Автозапуск",
                        icon = Icons.Rounded.AutoMode,
                        expanded = autoStartSectionExpanded,
                        onExpandToggle = { autoStartSectionExpanded = !autoStartSectionExpanded }
                    )

                    AnimatedVisibility(
                        visible = autoStartSectionExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Auto start breaks
                            SwitchSettingItem(
                                icon = Icons.Rounded.Coffee,
                                title = stringResource(R.string.auto_start_breaks),
                                checked = autoStartBreaks,
                                onCheckedChange = { autoStartBreaks = it }
                            )

                            // Auto start pomodoros
                            SwitchSettingItem(
                                icon = Icons.Rounded.PlayCircleFilled,
                                title = stringResource(R.string.auto_start_pomodoros),
                                checked = autoStartPomodoros,
                                onCheckedChange = { autoStartPomodoros = it }
                            )
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 4.dp))

                    // Notifications section
                    SettingsSectionHeader(
                        title = "Уведомления",
                        icon = Icons.Rounded.Notifications,
                        expanded = notificationSectionExpanded,
                        onExpandToggle = { notificationSectionExpanded = !notificationSectionExpanded }
                    )

                    AnimatedVisibility(
                        visible = notificationSectionExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Sound
                            SwitchSettingItem(
                                icon = Icons.Rounded.VolumeUp,
                                title = stringResource(R.string.sound_enabled),
                                checked = soundEnabled,
                                onCheckedChange = { soundEnabled = it }
                            )

                            // Vibration
                            SwitchSettingItem(
                                icon = Icons.Rounded.Vibration,
                                title = stringResource(R.string.vibration_enabled),
                                checked = vibrationEnabled,
                                onCheckedChange = { vibrationEnabled = it }
                            )

                            // Keep screen on
                            SwitchSettingItem(
                                icon = Icons.Rounded.Visibility,
                                title = stringResource(R.string.keep_screen_on),
                                checked = keepScreenOn,
                                onCheckedChange = { keepScreenOn = it }
                            )
                        }
                    }
                }

                // Footer with buttons
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Cancel button
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.padding(end = 16.dp)
                        ) {
                            Text("Отмена")
                        }

                        // Save button with more horizontal padding
                        Button(
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
                            },
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
                        ) {
                            Text("Сохранить")
                        }
                    }
                }
            }
        },
        confirmButton = { },
        dismissButton = { }
    )
}

@Composable
fun SettingsSectionHeader(
    title: String,
    icon: ImageVector,
    expanded: Boolean,
    onExpandToggle: () -> Unit
) {
    val rotationState by animateFloatAsState(
        targetValue = if (expanded) 0f else -90f,
        animationSpec = tween(300),
        label = "rotation"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onExpandToggle)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Section icon
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(6.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Section title
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )

        // Expand/collapse arrow
        Icon(
            imageVector = Icons.Rounded.KeyboardArrowDown,
            contentDescription = if (expanded) "Свернуть" else "Развернуть",
            modifier = Modifier.rotate(rotationState)
        )
    }
}

@Composable
fun DurationSliderItem(
    icon: ImageVector,
    title: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueText: String,
    color: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = valueText,
                style = MaterialTheme.typography.bodyMedium,
                color = color,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(end = 8.dp) // Добавляем отступ справа
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Slider with custom colors
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color,
                inactiveTrackColor = color.copy(alpha = 0.2f)
            )
        )
    }
}

@Composable
fun SwitchSettingItem(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}