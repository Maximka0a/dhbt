@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.dhbt.presentation.eisenhower

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dhbt.domain.model.Category
import com.example.dhbt.domain.model.TaskPriority

@Composable
fun CreateTaskDialog(
    quadrant: Int,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onCreateTask: (title: String, quadrant: Int, categoryId: String?, dueDate: Long?) -> Unit
) {
    var taskTitle by remember { mutableStateOf("") }
    var selectedQuadrant by remember { mutableStateOf(quadrant) }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Создать задачу в квадранте") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = taskTitle,
                    onValueChange = { taskTitle = it },
                    label = { Text("Название задачи") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Квадрант:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Выбор квадранта (1-4)
                    QuadrantChip(
                        selected = selectedQuadrant == 1,
                        onClick = { selectedQuadrant = 1 },
                        label = "I"
                    )
                    QuadrantChip(
                        selected = selectedQuadrant == 2,
                        onClick = { selectedQuadrant = 2 },
                        label = "II"
                    )
                    QuadrantChip(
                        selected = selectedQuadrant == 3,
                        onClick = { selectedQuadrant = 3 },
                        label = "III"
                    )
                    QuadrantChip(
                        selected = selectedQuadrant == 4,
                        onClick = { selectedQuadrant = 4 },
                        label = "IV"
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (categories.isNotEmpty()) {
                    Text(
                        "Категория:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    var expandedCategory by remember { mutableStateOf(false) }

                    ExposedDropdownMenuBox(
                        expanded = expandedCategory,
                        onExpandedChange = { expandedCategory = !expandedCategory }
                    ) {
                        OutlinedTextField(
                            value = categories.find { it.id == selectedCategoryId }?.name ?: "Выберите категорию",
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategory)
                            }
                        )

                        ExposedDropdownMenu(
                            expanded = expandedCategory,
                            onDismissRequest = { expandedCategory = false }
                        ) {
                            categories.forEach { category ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (category.iconEmoji != null) {
                                                Text(category.iconEmoji, modifier = Modifier.padding(end = 8.dp))
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .size(16.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            Color(
                                                                android.graphics.Color.parseColor(
                                                                    category.color ?: "#6200EE"
                                                                )
                                                            )
                                                        )
                                                        .padding(end = 8.dp)
                                                )
                                            }
                                            Text(category.name)
                                        }
                                    },
                                    onClick = {
                                        selectedCategoryId = category.id
                                        expandedCategory = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (taskTitle.isNotBlank()) {
                        onCreateTask(taskTitle, selectedQuadrant, selectedCategoryId, null)
                        onDismiss()
                    }
                },
                enabled = taskTitle.isNotBlank()
            ) {
                Text("Создать")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Composable
fun QuadrantChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        border = if (!selected) ButtonDefaults.outlinedButtonBorder else null,
        modifier = modifier
            .size(40.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = label,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}