package com.example.dhbt.presentation.habit.edit.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.dhbt.domain.model.Category


@Composable
fun CategoryChip(
    category: Category?,
    isSelected: Boolean,
    onSelect: () -> Unit,
    highlightColor: Color
) {
    val backgroundColor = if (isSelected) {
        highlightColor.copy(alpha = 0.1f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }

    val borderColor = if (isSelected) {
        highlightColor
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    }

    val categoryColor = category?.color?.let {
        try {
            Color(android.graphics.Color.parseColor(it))
        } catch (e: Exception) {
            highlightColor
        }
    } ?: highlightColor

    Surface(
        shape = RoundedCornerShape(50),
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .padding(end = 8.dp)
            .clickable(onClick = onSelect)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Цветной индикатор категории
            if (category != null) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(categoryColor)
                )

                Spacer(modifier = Modifier.width(8.dp))
            }

            Text(
                text = category?.name ?: "Без категории",
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSelected) highlightColor else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}