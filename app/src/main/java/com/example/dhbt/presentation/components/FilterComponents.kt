package com.example.dhbt.presentation.components

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.dhbt.R
import com.example.dhbt.domain.model.Category
import com.example.dhbt.domain.model.CategoryType
import com.example.dhbt.domain.model.TaskSort
import com.example.dhbt.presentation.theme.DHbtTheme
import com.example.dhbt.presentation.util.toColor

@Composable
fun FilterSortBar(
    onSortChanged: (TaskSort) -> Unit,
    currentSort: TaskSort,
    onFilterClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedSort by remember { mutableStateOf(false) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (expandedSort) 180f else 0f,
        label = "rotation"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = { expandedSort = !expandedSort }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Sort,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )

                Spacer(modifier = Modifier.width(4.dp))

                Text(stringResource(R.string.sort_tasks))

                Spacer(modifier = Modifier.width(4.dp))

                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier
                        .size(18.dp)
                        .rotate(rotationAngle)
                )
            }

            FilledTonalIconButton(
                onClick = onFilterClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FilterAlt,
                    contentDescription = stringResource(R.string.filter_by_category),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = expandedSort,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                SortRadioOption(
                    text = "По дате",
                    selected = currentSort == TaskSort.DATE,
                    onClick = { onSortChanged(TaskSort.DATE) }
                )

                SortRadioOption(
                    text = "По приоритету",
                    selected = currentSort == TaskSort.PRIORITY,
                    onClick = { onSortChanged(TaskSort.PRIORITY) }
                )

                SortRadioOption(
                    text = "По алфавиту",
                    selected = currentSort == TaskSort.ALPHABETICAL,
                    onClick = { onSortChanged(TaskSort.ALPHABETICAL) }
                )
            }
        }
    }
}

@Composable
private fun SortRadioOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.clickable(onClick = onClick)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryFilterChips(
    categories: List<Category>,
    selectedCategoryIds: Set<String>,
    onCategoryClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { category ->
            val selected = selectedCategoryIds.contains(category.id)
            val chipColor = category.color.toColor(MaterialTheme.colorScheme.primary)

            FilterChip(
                selected = selected,
                onClick = { onCategoryClick(category.id) },
                label = { Text(category.name) },
                leadingIcon = if (selected) {
                    {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                        )
                    }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = chipColor.copy(alpha = 0.12f),
                    selectedLabelColor = chipColor
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FilterSortBarPreview() {
    DHbtTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            FilterSortBar(
                onSortChanged = {},
                currentSort = TaskSort.DATE,
                onFilterClick = {},
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CategoryFilterChipsPreview() {
    DHbtTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            CategoryFilterChips(
                categories = listOf(
                    Category(
                        id = "1",
                        name = "Работа",
                        color = "#4CAF50",
                        iconEmoji = "💼",
                        type = CategoryType.TASK
                    ),
                    Category(
                        id = "2",
                        name = "Личное",
                        color = "#2196F3",
                        iconEmoji = "🏠",
                        type = CategoryType.TASK
                    ),
                    Category(
                        id = "3",
                        name = "Учеба",
                        color = "#FF9800",
                        iconEmoji = "📚",
                        type = CategoryType.TASK
                    )
                ),
                selectedCategoryIds = setOf("1"),
                onCategoryClick = {},
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}