package com.example.dhbt.presentation.dashboard.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.dhbt.domain.model.Habit
import com.example.dhbt.presentation.shared.EmojiIcon
import com.example.dhbt.presentation.util.toColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HabitsRow(
    habits: List<Habit>,
    onHabitClick: (String) -> Unit,
    onHabitProgressIncrement: (String) -> Unit
) {
    val hapticFeedback = LocalHapticFeedback.current
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Map для хранения состояния анимации для каждой привычки
    val animationStates = remember {
        mutableStateMapOf<String, Boolean>()
    }

    // Map для отслеживания последних обновлений привычек
    val lastUpdateTimestamp = remember {
        mutableStateMapOf<String, Long>()
    }

    LazyRow(
        state = lazyListState,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(habits, key = { it.id }) { habit ->
            // Анимация привычки
            val isAnimating = animationStates[habit.id] ?: false
            val scale by animateFloatAsState(
                targetValue = if (isAnimating) 1.1f else 1.0f,
                animationSpec = if (isAnimating) {
                    spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                } else {
                    tween(150)
                }
            )

            HabitCard(
                habit = habit,
                modifier = Modifier
                    .width(160.dp)
                    .height(130.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .combinedClickable(
                        onClick = { onHabitClick(habit.id) },
                        onLongClick = {
                            // Предотвращаем слишком частые обновления (дебаунс)
                            val currentTime = System.currentTimeMillis()
                            val lastUpdate = lastUpdateTimestamp[habit.id] ?: 0L
                            if (currentTime - lastUpdate > 1000) { // Минимум 1 секунда между обновлениями
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)

                                // Запуск анимации
                                coroutineScope.launch {
                                    animationStates[habit.id] = true
                                    delay(300) // Длительность анимации

                                    // Вызов функции инкремента прогресса
                                    onHabitProgressIncrement(habit.id)
                                    lastUpdateTimestamp[habit.id] = currentTime

                                    // Немного дольше задержка перед завершением анимации
                                    delay(300)
                                    animationStates[habit.id] = false
                                }
                            }
                        }
                    )
            )
        }
    }
}

@Composable
fun HabitCard(
    habit: Habit,
    modifier: Modifier = Modifier
) {
    val habitColor = habit.color.toColor(MaterialTheme.colorScheme.primary)

    // Запоминаем последнее значение прогресса для плавной анимации
    val previousProgress = remember { mutableFloatStateOf(0f) }

    // При изменении habit.currentStreak обновляем previousProgress
    LaunchedEffect(habit.currentStreak) {
        val newProgress = when (habit.type.value) {
            1, 2 -> { // QUANTITY или TIME
                val target = habit.targetValue ?: 1f
                if (target > 0f) (habit.currentStreak.toFloat() / target).coerceIn(0f, 1f)
                else 0f
            }
            else -> if (habit.currentStreak >= 1) 1f else 0f
        }
        previousProgress.floatValue = newProgress
    }

    ElevatedCard(
        modifier = modifier,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Иконка привычки
                EmojiIcon(
                    emoji = habit.iconEmoji,
                    backgroundColor = habitColor.copy(alpha = 0.2f),
                    emojiColor = habitColor,
                    modifier = Modifier.size(36.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Название привычки
                Text(
                    text = habit.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Прогресс-бар
            val progress = when (habit.type.value) {
                1, 2 -> { // QUANTITY или TIME
                    val target = habit.targetValue ?: 1f
                    if (target > 0f) (habit.currentStreak.toFloat() / target).coerceIn(0f, 1f)
                    else 0f
                }
                else -> if (habit.currentStreak >= 1) 1f else 0f // BINARY
            }

            // Анимировать прогресс с более заметной анимацией
            val animatedProgress by animateFloatAsState(
                targetValue = progress,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "Progress"
            )

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = habitColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Текст прогресса
            val progressText = when (habit.type.value) {
                1 -> "${habit.currentStreak}/${habit.targetValue?.toInt() ?: 0} ${habit.unitOfMeasurement ?: ""}"
                2 -> "${habit.currentStreak} мин"
                else -> if (habit.currentStreak >= 1) "Выполнено" else "Не выполнено"
            }

            Text(
                text = progressText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}