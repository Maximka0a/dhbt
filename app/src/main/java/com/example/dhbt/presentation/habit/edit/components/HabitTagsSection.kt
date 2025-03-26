package com.example.dhbt.presentation.habit.edit.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.dhbt.domain.model.Tag
import kotlin.collections.forEach

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HabitTagsSection(
    tags: List<Tag>,
    selectedTagIds: Set<String>,
    onTagsChange: (Set<String>) -> Unit,
    onAddTagClick: () -> Unit,
    habitColor: Color
) {
    SectionTitle(title = "Теги", icon = Icons.Default.Tag)

    SectionCard {
        Column {
            Text(
                text = "Теги помогают группировать и находить похожие привычки",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (tags.isEmpty()) {
                EmptyListPlaceholder(
                    text = "У вас пока нет тегов",
                    buttonText = "Создать тег",
                    onClick = onAddTagClick
                )
            } else {
                // Горизонтальный скролл с тегами
                val scrollState = rememberScrollState()

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    maxItemsInEachRow = 4
                ) {
                    tags.forEach { tag ->
                        val isSelected = selectedTagIds.contains(tag.id)
                        TagChip(
                            tag = tag,
                            isSelected = isSelected,
                            onSelect = {
                                if (isSelected) {
                                    onTagsChange(selectedTagIds - tag.id)
                                } else {
                                    onTagsChange(selectedTagIds + tag.id)
                                }
                            },
                            highlightColor = habitColor
                        )
                    }

                    // Кнопка добавления нового тега
                    AddItemChip(
                        text = "Добавить",
                        onClick = onAddTagClick,
                        highlightColor = habitColor
                    )
                }
            }
        }
    }
}



