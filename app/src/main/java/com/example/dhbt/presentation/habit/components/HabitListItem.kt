@file:OptIn(ExperimentalMaterialApi::class)

package com.example.dhbt.presentation.habit.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.rememberSwipeableState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dhbt.R
import com.example.dhbt.domain.model.HabitType
import com.example.dhbt.presentation.habit.list.HabitWithProgress

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun HabitListItem(
    habitWithProgress: HabitWithProgress,
    onToggleCompletion: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
    modifier: Modifier = Modifier
) {
    val habit = habitWithProgress.habit
    val progress = habitWithProgress.currentProgress
    val isCompleted = habitWithProgress.isCompletedToday

    // Анимация прогресса
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 500),
        label = "progress"
    )

    var swipeableState = rememberSwipeableState(initialValue = 0)

    // Получаем цвет привычки вне блоков try-catch
    val backgroundColor = getHabitColor(habit.color, MaterialTheme.colorScheme.surfaceVariant)

    val streakColor = when {
        habit.currentStreak >= 30 -> MaterialTheme.colorScheme.primary
        habit.currentStreak >= 14 -> MaterialTheme.colorScheme.secondary
        habit.currentStreak >= 7 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.outline
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(12.dp)
        ) {
            // Верхняя часть: Эмодзи, название и серия
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Эмодзи/иконка привычки в кружке с цветом привычки
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(backgroundColor)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = habit.iconEmoji ?: "📌",
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Название привычки
                Text(
                    text = habit.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Индикатор серии (streak)
                if (habit.currentStreak > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        // Заменим LocalFire на другую подходящую иконку
                        Icon(
                            imageVector = Icons.Default.Whatshot,
                            contentDescription = null,
                            tint = streakColor,
                            modifier = Modifier.size(16.dp)
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        Text(
                            text = habit.currentStreak.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Индикатор прогресса
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Нижняя часть: Прогресс и кнопки управления
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Отображение прогресса в зависимости от типа привычки
                val progressText = when (habit.type) {
                    HabitType.BINARY -> if (isCompleted) "Выполнено" else "Не выполнено"
                    HabitType.QUANTITY -> {
                        val currentValue = habitWithProgress.todayTracking?.value ?: 0f
                        val targetValue = habit.targetValue ?: 1f
                        "$currentValue/${targetValue.toInt()} ${habit.unitOfMeasurement ?: ""}"
                    }
                    HabitType.TIME -> {
                        val currentDuration = habitWithProgress.todayTracking?.duration ?: 0
                        val targetDuration = habit.targetValue?.toInt() ?: 0
                        "$currentDuration/${targetDuration} мин"
                    }
                }

                Text(
                    text = progressText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )

                // Кнопки управления прогрессом
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Для количественных и временных привычек показываем кнопки + и -
                    if (habit.type != HabitType.BINARY) {
                        IconButton(
                            onClick = onDecrement,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Remove,
                                contentDescription = "Уменьшить",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    // Для бинарных привычек - чекбокс, для остальных - кнопка +
                    if (habit.type == HabitType.BINARY) {
                        Checkbox(
                            checked = isCompleted,
                            onCheckedChange = { onToggleCompletion() },
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    } else {
                        IconButton(
                            onClick = onIncrement,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "Увеличить",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Меню действий
                    var expanded by remember { mutableStateOf(false) }

                    Box {
                        IconButton(onClick = { expanded = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Ещё"
                            )
                        }

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.edit)) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.Edit,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    expanded = false
                                    onEdit()
                                }
                            )

                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.archive)) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.Archive,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    expanded = false
                                    onArchive()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HabitGridItem(
    habitWithProgress: HabitWithProgress,
    onToggleCompletion: () -> Unit,
    onIncrement: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val habit = habitWithProgress.habit
    val progress = habitWithProgress.currentProgress
    val isCompleted = habitWithProgress.isCompletedToday

    // Анимация прогресса
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 500),
        label = "progress"
    )

    // Получаем цвет привычки вне блоков try-catch
    val backgroundColor = getHabitColor(habit.color, MaterialTheme.colorScheme.surfaceVariant)

    Card(
        modifier = modifier
            .height(120.dp)
            .aspectRatio(1f)
            .padding(4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onClick)
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Эмодзи/иконка привычки
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(backgroundColor)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = habit.iconEmoji ?: "📌",
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center
                )
            }

            // Название привычки
            Text(
                text = habit.title,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            // Прогресс
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                when (habit.type) {
                    HabitType.BINARY -> {
                        // Чекбокс для бинарных привычек
                        Checkbox(
                            checked = isCompleted,
                            onCheckedChange = { onToggleCompletion() },
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    else -> {
                        // Круговой прогресс для количественных и временных привычек
                        Box(
                            modifier = Modifier.size(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier.fillMaxSize(),
                                color = if (isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                strokeWidth = 3.dp,
                                strokeCap = StrokeCap.Round
                            )

                            IconButton(
                                onClick = onIncrement,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = "Увеличить",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Безопасно извлекает цвет из строки цветового кода
 */
@Composable
fun getHabitColor(colorString: String?, defaultColor: Color): Color {
    return if (colorString != null) {
        try {
            Color(android.graphics.Color.parseColor(colorString))
        } catch (e: Exception) {
            defaultColor
        }
    } else {
        defaultColor
    }
}