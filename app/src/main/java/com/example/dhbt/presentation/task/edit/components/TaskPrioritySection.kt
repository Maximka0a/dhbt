package com.example.dhbt.presentation.task.edit.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.dhbt.R
import com.example.dhbt.domain.model.TaskPriority

@Composable
fun TaskPrioritySection(
    priority: TaskPriority,
    eisenhowerQuadrant: Int?,
    onPriorityChanged: (TaskPriority) -> Unit,
    onQuadrantChanged: (Int?) -> Unit
) {
    var showEisenhowerMatrix by remember { mutableStateOf(false) }
    val rotationState by animateFloatAsState(
        targetValue = if (showEisenhowerMatrix) 180f else 0f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        // Приоритет
        SectionHeader(title = stringResource(R.string.priority))

        Spacer(modifier = Modifier.height(8.dp))

        // Список приоритетов в виде LazyRow
        PrioritySelector(
            selectedPriority = priority,
            onPrioritySelected = onPriorityChanged
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Заголовок с возможностью разворачивания матрицы Эйзенхауэра
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showEisenhowerMatrix = !showEisenhowerMatrix }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.eisenhower_matrix),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.rotate(rotationState)
            )
        }

        // Анимированное появление/исчезновение матрицы Эйзенхауэра
        AnimatedVisibility(
            visible = showEisenhowerMatrix,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            EisenhowerMatrixSelector(
                selectedQuadrant = eisenhowerQuadrant,
                onQuadrantSelected = onQuadrantChanged
            )
        }
    }
}

@Composable
fun PrioritySelector(
    selectedPriority: TaskPriority,
    onPrioritySelected: (TaskPriority) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(TaskPriority.values()) { priority ->
            val (backgroundColor, contentColor, icon, text) = when (priority) {
                TaskPriority.HIGH -> {
                    val bgColor = Color(0xFFF44336)
                    Quadruple(
                        bgColor.copy(alpha = if (selectedPriority == priority) 1f else 0.2f),
                        if (selectedPriority == priority) Color.White else bgColor,
                        Icons.Rounded.PriorityHigh,
                        stringResource(R.string.priority_high)
                    )
                }
                TaskPriority.MEDIUM -> {
                    val bgColor = Color(0xFFFF9800)
                    Quadruple(
                        bgColor.copy(alpha = if (selectedPriority == priority) 1f else 0.2f),
                        if (selectedPriority == priority) Color.White else bgColor,
                        Icons.Rounded.DragHandle,
                        stringResource(R.string.priority_medium)
                    )
                }
                TaskPriority.LOW -> {
                    val bgColor = Color(0xFF4CAF50)
                    Quadruple(
                        bgColor.copy(alpha = if (selectedPriority == priority) 1f else 0.2f),
                        if (selectedPriority == priority) Color.White else bgColor,
                        Icons.Rounded.LowPriority,
                        stringResource(R.string.priority_low)
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .height(42.dp)
                    .clickable { onPrioritySelected(priority) },
                shape = RoundedCornerShape(50),
                color = backgroundColor
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(18.dp)
                    )

                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = contentColor
                    )
                }
            }
        }
    }
}

@Composable
fun EisenhowerMatrixSelector(
    selectedQuadrant: Int?,
    onQuadrantSelected: (Int?) -> Unit
) {
    Column(modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            QuadrantBox(
                title = stringResource(R.string.quadrant1_title),
                subtitle = stringResource(R.string.quadrant1_action),
                color = Color(0xFFF44336),
                isSelected = selectedQuadrant == 1,
                onClick = { onQuadrantSelected(if (selectedQuadrant == 1) null else 1) },
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .padding(end = 4.dp, bottom = 4.dp)
            )

            QuadrantBox(
                title = stringResource(R.string.quadrant2_title),
                subtitle = stringResource(R.string.quadrant2_action),
                color = Color(0xFF4CAF50),
                isSelected = selectedQuadrant == 2,
                onClick = { onQuadrantSelected(if (selectedQuadrant == 2) null else 2) },
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .padding(start = 4.dp, bottom = 4.dp)
            )
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            QuadrantBox(
                title = stringResource(R.string.quadrant3_title),
                subtitle = stringResource(R.string.quadrant3_action),
                color = Color(0xFFFF9800),
                isSelected = selectedQuadrant == 3,
                onClick = { onQuadrantSelected(if (selectedQuadrant == 3) null else 3) },
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .padding(end = 4.dp, top = 4.dp)
            )

            QuadrantBox(
                title = stringResource(R.string.quadrant4_title),
                subtitle = stringResource(R.string.quadrant4_action),
                color = Color(0xFF2196F3),
                isSelected = selectedQuadrant == 4,
                onClick = { onQuadrantSelected(if (selectedQuadrant == 4) null else 4) },
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .padding(start = 4.dp, top = 4.dp)
            )
        }
    }
}

@Composable
fun QuadrantBox(
    title: String,
    subtitle: String,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) color else color.copy(alpha = 0.2f)
    val contentColor = if (isSelected && color.luminance() < 0.5f) Color.White else Color.Black

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = backgroundColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }
    }
}

// Вспомогательный класс для хранения квадруплетов значений
data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)