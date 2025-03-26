package com.example.dhbt.presentation.habit.edit.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RepeatOn
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dhbt.domain.model.Category
import com.example.dhbt.domain.model.FrequencyType
import com.example.dhbt.domain.model.HabitType
import com.example.dhbt.domain.model.PeriodType
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale
import kotlin.collections.forEach

@Composable
fun TargetStreakSelector(
    targetStreak: Int,
    onTargetStreakChange: (Int) -> Unit,
    habitColor: Color
) {
    Column {
        Text(
            text = "Целевая серия (необязательно)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = if (targetStreak > 0) targetStreak.toString() else "",
            onValueChange = {
                val newValue = it.toIntOrNull() ?: 0
                onTargetStreakChange(newValue)
            },
            label = { Text("Дней подряд") },
            placeholder = { Text("Например: 30 дней") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = habitColor,
                focusedLabelColor = habitColor
            ),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Whatshot, // замена для LocalFire
                    contentDescription = null,
                    tint = habitColor
                )
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Установите цель по количеству дней подряд, которые вы хотите выполнять эту привычку",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


@OptIn(ExperimentalAnimationApi::class)
@Composable
fun HabitFrequencySection(
    frequencyType: FrequencyType,
    onFrequencyTypeChange: (FrequencyType) -> Unit,
    selectedDays: Set<DayOfWeek>,
    onDaysOfWeekChange: (Set<DayOfWeek>) -> Unit,
    timesPerPeriod: Int,
    onTimesPerPeriodChange: (Int) -> Unit,
    periodType: PeriodType,
    onPeriodTypeChange: (PeriodType) -> Unit,
    habitColor: Color
) {
    SectionTitle(title = "Частота", icon = Icons.Default.RepeatOn)

    SectionCard {
        Column {
            // Выбор типа частоты
            FrequencyTypeSelector(
                selectedType = frequencyType,
                onTypeSelected = onFrequencyTypeChange,
                habitColor = habitColor
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Содержимое зависит от выбранного типа частоты
            AnimatedContent(
                targetState = frequencyType,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) with
                            fadeOut(animationSpec = tween(300))
                }
            ) { frequencyType ->
                when (frequencyType) {
                    FrequencyType.DAILY -> {
                        Text(
                            text = "Эта привычка будет отслеживаться каждый день",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FrequencyType.SPECIFIC_DAYS -> {
                        DaysOfWeekSelector(
                            selectedDays = selectedDays,
                            onDaysChanged = onDaysOfWeekChange,
                            habitColor = habitColor
                        )
                    }
                    FrequencyType.TIMES_PER_WEEK -> {
                        TimesPerPeriodSelector(
                            times = timesPerPeriod,
                            onTimesChange = onTimesPerPeriodChange,
                            periodType = PeriodType.WEEK,
                            habitColor = habitColor
                        )
                    }
                    FrequencyType.TIMES_PER_MONTH -> {
                        TimesPerPeriodSelector(
                            times = timesPerPeriod,
                            onTimesChange = onTimesPerPeriodChange,
                            periodType = PeriodType.MONTH,
                            habitColor = habitColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FrequencyTypeSelector(
    selectedType: FrequencyType,
    onTypeSelected: (FrequencyType) -> Unit,
    habitColor: Color
) {
    val options = listOf(
        FrequencyType.DAILY to "Ежедневно",
        FrequencyType.SPECIFIC_DAYS to "По дням недели",
        FrequencyType.TIMES_PER_WEEK to "X раз в неделю",
        FrequencyType.TIMES_PER_MONTH to "X раз в месяц"
    )

    Column {
        Text(
            text = "Как часто вы хотите отслеживать эту привычку?",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        options.forEach { (type, label) ->
            FrequencyOptionItem(
                label = label,
                isSelected = selectedType == type,
                onClick = { onTypeSelected(type) },
                highlightColor = habitColor
            )
        }
    }
}

@Composable
fun FrequencyOptionItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    highlightColor: Color
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) highlightColor.copy(alpha = 0.1f) else Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(8.dp)
        ) {
            RadioButton(
                selected = isSelected,
                onClick = null,
                colors = RadioButtonDefaults.colors(
                    selectedColor = highlightColor
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSelected) highlightColor else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun DaysOfWeekSelector(
    selectedDays: Set<DayOfWeek>,
    onDaysChanged: (Set<DayOfWeek>) -> Unit,
    habitColor: Color
) {
    Column {
        Text(
            text = "Выберите дни недели:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            DayOfWeek.values().forEach { day ->
                val isSelected = selectedDays.contains(day)

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) habitColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                        .clickable {
                            if (isSelected) {
                                onDaysChanged(selectedDays - day)
                            } else {
                                onDaysChanged(selectedDays + day)
                            }
                        }
                ) {
                    Text(
                        text = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()).first().toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun TimesPerPeriodSelector(
    times: Int,
    onTimesChange: (Int) -> Unit,
    periodType: PeriodType,
    habitColor: Color
) {
    val periodText = if (periodType == PeriodType.WEEK) "неделю" else "месяц"

    Column {
        Text(
            text = "Сколько раз в $periodText вы хотите выполнять эту привычку?",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = { if (times > 1) onTimesChange(times - 1) },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Уменьшить",
                    tint = if (times > 1) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = times.toString(),
                style = MaterialTheme.typography.headlineMedium,
                color = habitColor,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(48.dp),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.width(16.dp))

            IconButton(
                onClick = { onTimesChange(times + 1) },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Увеличить",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "раз в $periodText",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun HabitProgressSection(
    habitType: HabitType,
    targetValue: Float,
    onTargetValueChange: (Float) -> Unit,
    unitOfMeasurement: String,
    onUnitOfMeasurementChange: (String) -> Unit,
    targetStreak: Int,
    onTargetStreakChange: (Int) -> Unit,
    habitColor: Color
) {
    SectionTitle(title = "Прогресс", icon = Icons.Default.TrendingUp)

    SectionCard {
        Column {
            // Различное содержимое в зависимости от типа привычки
            when (habitType) {
                HabitType.BINARY -> {
                    Text(
                        text = "Для этой привычки достаточно просто отметить её выполнение.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                HabitType.QUANTITY -> {
                    QuantityTargetSelector(
                        targetValue = targetValue,
                        onTargetValueChange = onTargetValueChange,
                        unit = unitOfMeasurement,
                        onUnitChange = onUnitOfMeasurementChange,
                        habitColor = habitColor
                    )
                }
                HabitType.TIME -> {
                    TimeTargetSelector(
                        targetMinutes = targetValue.toInt(),
                        onTargetMinutesChange = { onTargetValueChange(it.toFloat()) },
                        habitColor = habitColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Целевая серия (для всех типов привычек)
            TargetStreakSelector(
                targetStreak = targetStreak,
                onTargetStreakChange = onTargetStreakChange,
                habitColor = habitColor
            )
        }
    }
}

@Composable
fun QuantityTargetSelector(
    targetValue: Float,
    onTargetValueChange: (Float) -> Unit,
    unit: String,
    onUnitChange: (String) -> Unit,
    habitColor: Color
) {
    Column {
        Text(
            text = "Сколько раз/единиц нужно для выполнения цели?",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Поле для ввода целевого значения
            OutlinedTextField(
                value = targetValue.toString(),
                onValueChange = {
                    val newValue = it.toFloatOrNull() ?: return@OutlinedTextField
                    if (newValue > 0) {
                        onTargetValueChange(newValue)
                    }
                },
                label = { Text("Целевое значение") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = habitColor,
                    focusedLabelColor = habitColor
                ),
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Поле для ввода единицы измерения
            OutlinedTextField(
                value = unit,
                onValueChange = onUnitChange,
                label = { Text("Ед. изм.") },
                placeholder = { Text("стаканов") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = habitColor,
                    focusedLabelColor = habitColor
                ),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun TimeTargetSelector(
    targetMinutes: Int,
    onTargetMinutesChange: (Int) -> Unit,
    habitColor: Color
) {
    Column {
        Text(
            text = "Сколько минут нужно для выполнения цели?",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = { if (targetMinutes > 1) onTargetMinutesChange(targetMinutes - 1) },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Уменьшить",
                    tint = if (targetMinutes > 1) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = targetMinutes.toString(),
                style = MaterialTheme.typography.headlineMedium,
                color = habitColor,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(60.dp),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.width(16.dp))

            IconButton(
                onClick = { onTargetMinutesChange(targetMinutes + 1) },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Увеличить",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "минут",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun HabitCategorySection(
    categories: List<Category>,
    selectedCategoryId: String?,
    onCategorySelected: (String?) -> Unit,
    onAddCategoryClick: () -> Unit,
    habitColor: Color
) {
    SectionTitle(title = "Категория", icon = Icons.Default.Folder)

    SectionCard {
        Column {
            if (categories.isEmpty()) {
                EmptyListPlaceholder(
                    text = "У вас пока нет категорий",
                    buttonText = "Создать категорию",
                    onClick = onAddCategoryClick
                )
            } else {
                // Горизонтальный скролл с категориями
                val scrollState = rememberScrollState()

                Row(
                    modifier = Modifier
                        .horizontalScroll(scrollState)
                        .padding(vertical = 8.dp)
                ) {
                    // Опция "Без категории"
                    CategoryChip(
                        category = null,
                        isSelected = selectedCategoryId == null,
                        onSelect = { onCategorySelected(null) },
                        highlightColor = habitColor
                    )

                    categories.forEach { category ->
                        CategoryChip(
                            category = category,
                            isSelected = selectedCategoryId == category.id,
                            onSelect = { onCategorySelected(category.id) },
                            highlightColor = habitColor
                        )
                    }

                    // Кнопка добавления новой категории
                    AddItemChip(
                        text = "Создать новую",
                        onClick = onAddCategoryClick,
                        highlightColor = habitColor
                    )
                }
            }
        }
    }
}

@Composable
fun HabitTypeSection(
    selectedType: HabitType,
    onTypeSelected: (HabitType) -> Unit
) {
    SectionTitle(title = "Тип привычки", icon = Icons.Default.Category)

    SectionCard {
        Column {
            Text(
                text = "Как вы хотите измерять прогресс?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HabitTypeOption(
                    type = HabitType.BINARY,
                    icon = Icons.Default.CheckBox,
                    title = "Да/Нет",
                    description = "Просто отмечать выполнение",
                    isSelected = selectedType == HabitType.BINARY,
                    onSelect = { onTypeSelected(HabitType.BINARY) },
                    modifier = Modifier.weight(1f)
                )

                HabitTypeOption(
                    type = HabitType.QUANTITY,
                    icon = Icons.Default.Numbers,
                    title = "Количество",
                    description = "Отслеживать числовые значения",
                    isSelected = selectedType == HabitType.QUANTITY,
                    onSelect = { onTypeSelected(HabitType.QUANTITY) },
                    modifier = Modifier.weight(1f)
                )

                HabitTypeOption(
                    type = HabitType.TIME,
                    icon = Icons.Default.Timer,
                    title = "Время",
                    description = "Отслеживать затраченное время",
                    isSelected = selectedType == HabitType.TIME,
                    onSelect = { onTypeSelected(HabitType.TIME) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}


@Composable
fun HabitBasicInfoSection(
    title: String,
    onTitleChange: (String) -> Unit,
    titleError: String?,
    description: String,
    onDescriptionChange: (String) -> Unit,
    emoji: String,
    onEmojiClick: () -> Unit,
    selectedColor: Color,
    onColorClick: () -> Unit
) {
    SectionCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Эмодзи и цвет
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(end = 16.dp)
            ) {
                // Кнопка выбора эмодзи
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(selectedColor)
                        .clickable(onClick = onEmojiClick)
                ) {
                    Text(
                        text = emoji,
                        fontSize = 28.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Кнопка выбора цвета
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(onClick = onColorClick)
                        .border(2.dp, selectedColor, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.ColorLens,
                        contentDescription = "Выбрать цвет",
                        tint = selectedColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Заголовок и описание
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Поле названия привычки
                OutlinedTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    label = { Text("Название") },
                    placeholder = { Text("Например: Медитация") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next
                    ),
                    isError = titleError != null,
                    supportingText = titleError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Поле описания
                OutlinedTextField(
                    value = description,
                    onValueChange = onDescriptionChange,
                    label = { Text("Описание (необязательно)") },
                    placeholder = { Text("Для чего эта привычка") },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done
                    ),
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
