package com.example.dhbt.presentation.dashboard.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.dhbt.R
import com.example.dhbt.presentation.theme.ProgressColors

@Composable
fun StatisticsCard(
    completedTasks: Int,
    totalTasks: Int,
    completedHabits: Int,
    totalHabits: Int,
    onCardClick: () -> Unit = {}
) {
    val isFirstRender = remember { mutableStateOf(true) }

    // Анимация появления
    LaunchedEffect(Unit) {
        isFirstRender.value = false
    }

    Card(
        onClick = onCardClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp // Добавляем тень как в карточке привычки
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Статистика по задачам
                ProgressMetric(
                    icon = Icons.Rounded.Task,
                    title = stringResource(R.string.tasks),
                    completed = completedTasks,
                    total = totalTasks,
                    modifier = Modifier.weight(1f)
                )

                // Статистика по привычкам
                ProgressMetric(
                    icon = Icons.Rounded.Loop,
                    title = stringResource(R.string.habits),
                    completed = completedHabits,
                    total = totalHabits,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun ProgressMetric(
    icon: ImageVector,
    title: String,
    completed: Int,
    total: Int,
    modifier: Modifier = Modifier
) {
    val animatedProgress = animateFloatAsState(
        targetValue = if (total > 0) completed.toFloat() / total else 0f,
        animationSpec = tween(1000, easing = LinearOutSlowInEasing)
    )

    val progressColor = animateColorAsState(
        targetValue = when {
            total == 0 -> ProgressColors.neutral
            completed >= total -> ProgressColors.completed
            completed >= total * 0.75 -> ProgressColors.excellent
            completed >= total * 0.5 -> ProgressColors.good
            completed >= total * 0.25 -> ProgressColors.moderate
            else -> ProgressColors.poor
        },
        animationSpec = tween(500)
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(horizontal = 8.dp)
    ) {
        Box(
            modifier = Modifier.size(72.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                progress = { animatedProgress.value },
                modifier = Modifier.fillMaxSize(),
                color = progressColor.value,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeWidth = 8.dp,
                strokeCap = StrokeCap.Round
            )

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Исправленный текст прогресса
        Text(
            text = "$completed/$total",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}