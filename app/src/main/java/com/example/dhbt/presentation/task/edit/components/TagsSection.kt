package com.example.dhbt.presentation.task.edit.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.dhbt.R
import com.example.dhbt.domain.model.Tag
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun TagsSection(
    tags: List<Tag>,
    selectedTagIds: Set<String>,
    onTagToggled: (String) -> Unit,
    onAddNewTag: (String, String) -> Unit
) {
    var showAddTagDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Эффект для проверки загрузки тегов
    LaunchedEffect(tags) {
        if (tags.isEmpty()) {
            delay(500) // Даем время для загрузки
            // Здесь можно добавить логику для повторной загрузки тегов, если нужно
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(
            title = stringResource(R.string.tags),
            actionText = stringResource(R.string.add_tag),
            onActionClick = { showAddTagDialog = true }
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (tags.isEmpty()) {
            NoItemsMessage(
                message = stringResource(R.string.no_tags),
                actionLabel = stringResource(R.string.create_tag),
                onAction = { showAddTagDialog = true }
            )
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(tags) { tag ->
                    val isSelected = selectedTagIds.contains(tag.id)

                    TagChip(
                        tag = tag,
                        isSelected = isSelected,
                        onClick = { onTagToggled(tag.id) }
                    )
                }

                // Кнопка добавления нового тега
                item {
                    Surface(
                        modifier = Modifier
                            .height(32.dp)
                            .clickable { showAddTagDialog = true },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )

                            Text(
                                text = stringResource(R.string.new_tag),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    // Диалог создания нового тега
    if (showAddTagDialog) {
        AddTagDialog(
            onAddTag = { name, color ->
                onAddNewTag(name, color)
                scope.launch {
                    delay(300) // Даём время для добавления
                }
            },
            onDismissRequest = { showAddTagDialog = false }
        )
    }
}

@Composable
fun TagChip(
    tag: Tag,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val tagColor = parseColor(tag.color, MaterialTheme.colorScheme.secondary)

    val backgroundColor = if (isSelected) {
        tagColor
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    }

    val contentColor = if (isSelected && tagColor.luminance() < 0.5f) {
        Color.White
    } else if (isSelected) {
        Color.Black
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = Modifier
            .height(32.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) contentColor else tagColor
                    )
            )

            Text(
                text = tag.name,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTagDialog(
    onAddTag: (name: String, color: String) -> Unit,
    onDismissRequest: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    var tagName by remember { mutableStateOf("") }
    var tagColor by remember { mutableStateOf(predefinedColors.first()) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(R.string.create_tag)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Поле ввода имени тега
                OutlinedTextField(
                    value = tagName,
                    onValueChange = { tagName = it },
                    label = { Text(stringResource(R.string.tag_name)) },
                    placeholder = { Text(stringResource(R.string.enter_tag_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                )

                // Выбор цвета
                Text(
                    text = stringResource(R.string.tag_color),
                    style = MaterialTheme.typography.bodyMedium
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(predefinedColors) { colorHex ->
                        val color = try {
                            Color(android.graphics.Color.parseColor(colorHex))
                        } catch (e: Exception) {
                            MaterialTheme.colorScheme.secondary
                        }

                        val isSelected = colorHex == tagColor

                        Box(
                            modifier = Modifier.size(42.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isSelected) 2.dp else 0.dp,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        shape = CircleShape
                                    )
                                    .clickable { tagColor = colorHex }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (tagName.isNotBlank()) {
                        onAddTag(tagName, tagColor)
                        onDismissRequest()
                    }
                },
                enabled = tagName.isNotBlank()
            ) {
                Text(stringResource(R.string.create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
val predefinedColors = listOf(
    "#F44336", // Красный
    "#E91E63", // Розовый
    "#9C27B0", // Пурпурный
    "#673AB7", // Темно-пурпурный
    "#3F51B5", // Индиго
    "#2196F3", // Синий
    "#03A9F4", // Светло-синий
    "#00BCD4", // Бирюзовый
    "#009688", // Зеленовато-синий
    "#4CAF50", // Зеленый
    "#8BC34A", // Светло-зеленый
    "#CDDC39", // Лаймовый
    "#FFEB3B", // Желтый
    "#FFC107", // Янтарный
    "#FF9800", // Оранжевый
    "#FF5722", // Темно-оранжевый
    "#795548", // Коричневый
    "#9E9E9E", // Серый
    "#607D8B"  // Сине-серый
)