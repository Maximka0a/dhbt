package com.example.dhbt.presentation.task.edit.components

import android.graphics.Color.parseColor
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
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.dhbt.R
import com.example.dhbt.domain.model.Category

@Composable
fun CategorySection(
    categories: List<Category>,
    selectedCategoryId: String?,
    onCategorySelected: (String?) -> Unit,
    onAddNewCategory: (String, String) -> Unit
) {
    var showAddCategorySheet by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(
            title = stringResource(R.string.category),
            actionText = stringResource(R.string.add_new),
            onActionClick = { showAddCategorySheet = true }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Показываем список категорий или сообщение, если их нет
        if (categories.isEmpty()) {
            NoItemsMessage(
                message = stringResource(R.string.no_categories),
                actionLabel = stringResource(R.string.create_category),
                onAction = { showAddCategorySheet = true }
            )
        } else {
            // Отображаем категорию "Нет" и список реальных категорий
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                // Опция "Нет категории"
                item {
                    CategoryChip(
                        name = stringResource(R.string.none),
                        icon = Icons.Rounded.DoNotDisturbAlt,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        isSelected = selectedCategoryId == null,
                        onClick = { onCategorySelected(null) }
                    )
                }

                // Существующие категории
                // Внутри LazyRow или LazyColumn
                items(categories) { category ->
                    val iconMapping = remember { categoryIconMap }

                    // Используем ту же вспомогательную функцию
                    val categoryColor = parseColor(category.color, MaterialTheme.colorScheme.primary)

                    // Получаем иконку из маппинга или используем дефолтную
                    val icon = category.iconEmoji?.let { iconName ->
                        iconMapping[iconName] ?: Icons.Rounded.Category
                    } ?: Icons.Rounded.Category

                    CategoryChip(
                        name = category.name,
                        icon = icon,
                        color = categoryColor,
                        isSelected = category.id == selectedCategoryId,
                        onClick = { onCategorySelected(category.id) }
                    )
                }
            }
        }
    }

    // Нижний лист для создания новой категории
    if (showAddCategorySheet) {
        AddCategoryBottomSheet(
            onDismiss = { showAddCategorySheet = false },
            onAddCategory = { name, color, iconName ->
                onAddNewCategory(name, color)
                showAddCategorySheet = false
            }
        )
    }
}

// Вспомогательная функция для парсинга цвета
fun parseColor(colorString: String?, defaultColor: Color): Color {
    return try {
        colorString?.let { Color(android.graphics.Color.parseColor(it)) } ?: defaultColor
    } catch (e: Exception) {
        defaultColor
    }
}

@Composable
fun CategoryChip(
    name: String,
    icon: ImageVector,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        color
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    }

    val contentColor = if (isSelected) {
        // Проверяем, достаточно ли темный цвет фона для белого текста
        val luminance = color.luminance()
        if (luminance > 0.5f) Color.Black else Color.White
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = Modifier
            .height(38.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(50),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )

            Text(
                text = name,
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
fun AddCategoryBottomSheet(
    onDismiss: () -> Unit,
    onAddCategory: (name: String, color: String, iconName: String?) -> Unit
) {
    val focusManager = LocalFocusManager.current
    var categoryName by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(predefinedColors.first()) }
    var selectedIconName by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = stringResource(R.string.create_category),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Поле ввода имени категории
            OutlinedTextField(
                value = categoryName,
                onValueChange = { categoryName = it },
                label = { Text(stringResource(R.string.category_name)) },
                placeholder = { Text(stringResource(R.string.enter_category_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
            )

            // Выбор цвета
            Text(
                text = stringResource(R.string.select_color),
                style = MaterialTheme.typography.titleMedium
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

                    val isSelected = colorHex == selectedColor

                    Box(
                        modifier = Modifier.size(46.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 2.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = colorHex }
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .align(Alignment.Center)
                                )
                            }
                        }
                    }
                }
            }

            // Выбор иконки
            Text(
                text = stringResource(R.string.select_icon),
                style = MaterialTheme.typography.titleMedium
            )

            val iconMap = remember { categoryIconMap }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(iconMap.entries.toList()) { entry ->
                    val isSelected = entry.key == selectedIconName

                    Box(
                        modifier = Modifier.size(46.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) {
                                        try {
                                            Color(android.graphics.Color.parseColor(selectedColor))
                                        } catch (e: Exception) {
                                            MaterialTheme.colorScheme.primary
                                        }
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    }
                                )
                                .clickable { selectedIconName = entry.key },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = entry.value,
                                contentDescription = entry.key,
                                tint = if (isSelected) {
                                    val color = try {
                                        Color(android.graphics.Color.parseColor(selectedColor))
                                    } catch (e: Exception) {
                                        MaterialTheme.colorScheme.primary
                                    }

                                    if (color.luminance() > 0.5f) Color.Black else Color.White
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            // Кнопка добавления
            Button(
                onClick = { onAddCategory(categoryName, selectedColor, selectedIconName) },
                enabled = categoryName.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.create_category))
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// Маппинг названий иконок к самим иконкам
val categoryIconMap = mapOf(
    "calendar" to Icons.Rounded.CalendarMonth,
    "work" to Icons.Rounded.Work,
    "home" to Icons.Rounded.Home,
    "study" to Icons.Rounded.School,
    "health" to Icons.Rounded.Favorite,
    "fitness" to Icons.Rounded.FitnessCenter,
    "shopping" to Icons.Rounded.ShoppingCart,
    "travel" to Icons.Rounded.Flight,
    "finance" to Icons.Rounded.AccountBalance,
    "personal" to Icons.Rounded.Person,
    "family" to Icons.Rounded.People,
    "food" to Icons.Rounded.Restaurant,
    "other" to Icons.Rounded.MoreHoriz
)