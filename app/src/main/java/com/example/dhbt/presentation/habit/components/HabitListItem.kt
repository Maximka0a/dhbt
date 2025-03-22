package com.example.dhbt.presentation.habit.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dhbt.R
import com.example.dhbt.domain.model.HabitType
import com.example.dhbt.presentation.habit.list.HabitWithProgress

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
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

    // Анимация прогресса с пружинным эффектом
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "progress"
    )


    // Получаем цвет привычки
    val habitColor = getHabitColor(habit.color, MaterialTheme.colorScheme.primary)

    // Определяем цвет фона для карточки
    val cardColor = if (isCompleted) {
        MaterialTheme.colorScheme.surfaceContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    // Определяем цвет для полосы прогресса
    val progressColor = if (isCompleted) {
        habitColor
    } else {
        MaterialTheme.colorScheme.secondary
    }

    // Определяем цвет для серии
    val streakColor = when {
        habit.currentStreak >= 30 -> MaterialTheme.colorScheme.tertiary
        habit.currentStreak >= 14 -> MaterialTheme.colorScheme.secondary
        habit.currentStreak >= 7 -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline
    }

    Card(
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isCompleted) 4.dp else 1.dp,
            pressedElevation = 8.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(16.dp)
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
                        .size(48.dp)
                        .shadow(elevation = 4.dp, shape = CircleShape)
                        .clip(CircleShape)
                        .background(habitColor)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = habit.iconEmoji ?: "📌",
                        fontSize = 22.sp,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Название привычки с индикатором типа
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = habit.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Тип привычки
                        val typeIcon = when (habit.type) {
                            HabitType.BINARY -> Icons.Default.Check
                            HabitType.QUANTITY -> Icons.Default.Numbers
                            HabitType.TIME -> Icons.Default.Timer
                        }

                        val typeColor = when (habit.type) {
                            HabitType.BINARY -> MaterialTheme.colorScheme.primary
                            HabitType.QUANTITY -> MaterialTheme.colorScheme.secondary
                            HabitType.TIME -> MaterialTheme.colorScheme.tertiary
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Icon(
                            imageVector = typeIcon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = typeColor
                        )
                    }

                    // Описание привычки (если есть)
                    if (!habit.description.isNullOrBlank()) {
                        Text(
                            text = habit.description,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

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
                        Icon(
                            imageVector = Icons.Default.Whatshot,
                            contentDescription = null,
                            tint = streakColor,
                            modifier = Modifier.size(18.dp)
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        Text(
                            text = habit.currentStreak.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = streakColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Индикатор прогресса с пульсирующим эффектом
            Box(modifier = Modifier.fillMaxWidth()) {
                // Фоновая полоса
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )

                // Полоса прогресса
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(progressColor)
                )

                // Пульсирующая точка на конце прогрессбара для незавершенных задач
                if (!isCompleted && animatedProgress > 0.05f) {
                    Box(
                        modifier = Modifier
                            .offset(x = (animatedProgress * 100).coerceIn(0f, 100f).dp - 6.dp)
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(progressColor)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Нижняя часть: Прогресс и кнопки управления
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Отображение прогресса в зависимости от типа привычки
                val progressText = when (habit.type) {
                    HabitType.BINARY -> if (isCompleted)
                        stringResource(R.string.completed)
                    else
                        stringResource(R.string.not_completed)
                    HabitType.QUANTITY -> {
                        val currentValue = habitWithProgress.todayTracking?.value ?: 0f
                        val targetValue = habit.targetValue ?: 1f
                        "$currentValue/${targetValue.toInt()} ${habit.unitOfMeasurement ?: ""}"
                    }
                    HabitType.TIME -> {
                        val currentDuration = habitWithProgress.todayTracking?.duration ?: 0
                        val targetDuration = habit.targetValue?.toInt() ?: 0
                        "$currentDuration/${targetDuration} ${stringResource(R.string.minutes)}"
                    }
                }

                AnimatedContent(
                    targetState = progressText,
                    transitionSpec = {
                        slideInVertically { height -> height } + fadeIn() with
                                slideOutVertically { height -> -height } + fadeOut()
                    }
                ) { text ->
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isCompleted)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isCompleted) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Кнопки управления прогрессом
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Для количественных и временных привычек показываем кнопки + и -
                    if (habit.type != HabitType.BINARY) {
                        // Кнопка уменьшения прогресса
                        FilledTonalIconButton(
                            onClick = onDecrement,
                            modifier = Modifier.size(36.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Remove,
                                contentDescription = stringResource(R.string.decrease),
                                modifier = Modifier.size(18.dp)
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
                                checkedColor = habitColor,
                                checkmarkColor = Color.White
                            )
                        )
                    } else {
                        // Кнопка увеличения прогресса
                        FilledIconButton(
                            onClick = onIncrement,
                            modifier = Modifier.size(36.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = habitColor,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = stringResource(R.string.increase),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Меню действий
                    var expanded by remember { mutableStateOf(false) }

                    Box {
                        IconButton(
                            onClick = { expanded = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.more_actions)
                            )
                        }

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier
                                .width(220.dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceContainerHigh,
                                    RoundedCornerShape(16.dp)
                                )
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.details)) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    expanded = false
                                    onClick()
                                }
                            )

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

                            Divider(
                                modifier = Modifier.padding(vertical = 4.dp)
                            )

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        stringResource(R.string.archive),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.Archive,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
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
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "progress"
    )

    // Получаем цвет привычки
    val backgroundColor = getHabitColor(habit.color, MaterialTheme.colorScheme.primary)
    val containerColor = if (isCompleted) {
        MaterialTheme.colorScheme.surfaceContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = modifier
            .height(160.dp)
            .aspectRatio(0.85f),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isCompleted) 4.dp else 1.dp,
            pressedElevation = 8.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onClick)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Эмодзи/иконка привычки с индикатором серии
            Box(
                contentAlignment = Alignment.Center
            ) {
                // Основной значок привычки
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .shadow(elevation = 4.dp, shape = CircleShape)
                        .clip(CircleShape)
                        .background(backgroundColor)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = habit.iconEmoji ?: "📌",
                        fontSize = 24.sp,
                        textAlign = TextAlign.Center
                    )
                }

                // Бейдж серии, если > 0
                if (habit.currentStreak > 0) {
                    val streakColor = when {
                        habit.currentStreak >= 30 -> MaterialTheme.colorScheme.tertiary
                        habit.currentStreak >= 14 -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.primary
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 8.dp, y = (-4).dp)
                            .size(24.dp)
                            .shadow(elevation = 2.dp, shape = CircleShape)
                            .clip(CircleShape)
                            .background(streakColor)
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = habit.currentStreak.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // Название привычки
            Text(
                text = habit.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            // Прогресс
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                when (habit.type) {
                    HabitType.BINARY -> {
                        // Чекбокс для бинарных привычек
                        Checkbox(
                            checked = isCompleted,
                            onCheckedChange = { onToggleCompletion() },
                            colors = CheckboxDefaults.colors(
                                checkedColor = backgroundColor,
                                checkmarkColor = Color.White
                            ),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    else -> {
                        // Круговой прогресс для количественных и временных привычек
                        Box(
                            modifier = Modifier.size(44.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier.fillMaxSize(),
                                color = if (isCompleted) backgroundColor else MaterialTheme.colorScheme.secondary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                strokeWidth = 4.dp,
                                strokeCap = StrokeCap.Round
                            )

                            IconButton(
                                onClick = onIncrement,
                                modifier = Modifier.matchParentSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = stringResource(R.string.increase),
                                    tint = MaterialTheme.colorScheme.onSurface
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