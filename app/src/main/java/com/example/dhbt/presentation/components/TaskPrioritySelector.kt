package com.example.dhbt.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.dhbt.domain.model.TaskPriority

@Composable
fun TaskPrioritySelector(
    selectedPriority: TaskPriority,
    onPrioritySelected: (TaskPriority) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PriorityOption(
            priority = TaskPriority.LOW,
            isSelected = selectedPriority == TaskPriority.LOW,
            onSelect = { onPrioritySelected(TaskPriority.LOW) },
            color = Color(0xFF8BC34A), // Light Green
            modifier = Modifier.weight(1f)
        )

        PriorityOption(
            priority = TaskPriority.MEDIUM,
            isSelected = selectedPriority == TaskPriority.MEDIUM,
            onSelect = { onPrioritySelected(TaskPriority.MEDIUM) },
            color = Color(0xFFFFC107), // Amber
            modifier = Modifier.weight(1f)
        )

        PriorityOption(
            priority = TaskPriority.HIGH,
            isSelected = selectedPriority == TaskPriority.HIGH,
            onSelect = { onPrioritySelected(TaskPriority.HIGH) },
            color = Color(0xFFF44336), // Red
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun PriorityOption(
    priority: TaskPriority,
    isSelected: Boolean,
    onSelect: () -> Unit,
    color: Color,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) color else color.copy(alpha = 0.2f)
    val contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface

    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable { onSelect() }
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Flag,
                contentDescription = "Priority Flag",
                tint = contentColor
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = when(priority) {
                    TaskPriority.LOW -> "Low"
                    TaskPriority.MEDIUM -> "Medium"
                    TaskPriority.HIGH -> "High"
                },
                color = contentColor
            )
        }
    }
}