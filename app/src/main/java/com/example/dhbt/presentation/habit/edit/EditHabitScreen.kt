@file:OptIn(ExperimentalAnimationApi::class, ExperimentalLayoutApi::class)

package com.example.dhbt.presentation.habit.edit

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.dhbt.domain.model.*
import com.example.dhbt.presentation.components.ConfirmDeleteDialog
import com.example.dhbt.presentation.components.EmojiPickerDialog
import com.example.dhbt.presentation.theme.habitTypeColors
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditHabitScreen(
    navController: NavController,
    viewModel: EditHabitViewModel = hiltViewModel()
) {

    val isCreationMode = navController.previousBackStackEntry?.destination?.route?.contains("habitId=null") == true
            || navController.previousBackStackEntry?.destination?.route?.contains("habitId=") != true

    val uiState by viewModel.uiState.collectAsState()
    val validationState by viewModel.validationState.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val tags by viewModel.tags.collectAsState()
    val saveResult by viewModel.saveResult.collectAsState()

    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    // Диалоговые состояния
    var showColorPicker by remember { mutableStateOf(false) }
    var showEmojiPicker by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showTagDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showTimePickerDialog by remember { mutableStateOf(false) }
    var tempSelectedTime by remember { mutableStateOf(uiState.reminderTime) }

    // Цвет фона привычки (как элемент пользовательского интерфейса)
    val habitColor = try {
        Color(android.graphics.Color.parseColor(uiState.selectedColor))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    // Обработка результата сохранения
    LaunchedEffect(saveResult) {
        when (saveResult) {
            is SaveResult.Success -> {
                Toast.makeText(
                    context,
                    "Привычка успешно сохранена!",
                    Toast.LENGTH_SHORT
                ).show()
                navController.navigateUp()
            }
            is SaveResult.Deleted -> {
                Toast.makeText(
                    context,
                    "Привычка удалена",
                    Toast.LENGTH_SHORT
                ).show()
                navController.navigateUp()
            }
            is SaveResult.Error -> {
                Toast.makeText(
                    context,
                    "Ошибка: ${(saveResult as SaveResult.Error).message}",
                    Toast.LENGTH_LONG
                ).show()
            }
            null -> {} // Ничего не делаем
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isCreationMode)
                            "Создание новой привычки"
                        else
                            "Редактирование привычки"
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showDeleteConfirmation = true },
                        enabled = navController.previousBackStackEntry?.destination?.route?.contains("habitId=null") != true
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Удалить",
                            tint = if (navController.previousBackStackEntry?.destination?.route?.contains("habitId=null") != true)
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }
                    IconButton(onClick = { viewModel.onEvent(EditHabitEvent.SaveHabit) }) {
                        Icon(Icons.Default.Save, contentDescription = "Сохранить")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    focusManager.clearFocus()
                    viewModel.onEvent(EditHabitEvent.SaveHabit)
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(Icons.Default.Check, contentDescription = "Сохранить")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            // Блок основной информации
            HabitBasicInfoSection(
                title = uiState.title,
                onTitleChange = { viewModel.onEvent(EditHabitEvent.TitleChanged(it)) },
                titleError = validationState.titleError,
                description = uiState.description,
                onDescriptionChange = { viewModel.onEvent(EditHabitEvent.DescriptionChanged(it)) },
                emoji = uiState.iconEmoji,
                onEmojiClick = { showEmojiPicker = true },
                selectedColor = habitColor,
                onColorClick = { showColorPicker = true }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Блок типа привычки
            HabitTypeSection(
                selectedType = uiState.habitType,
                onTypeSelected = { viewModel.onEvent(EditHabitEvent.HabitTypeChanged(it)) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Блок категории
            HabitCategorySection(
                categories = categories,
                selectedCategoryId = uiState.categoryId,
                onCategorySelected = { viewModel.onEvent(EditHabitEvent.CategoryChanged(it)) },
                onAddCategoryClick = { showCategoryDialog = true },
                habitColor = habitColor
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Блок частоты
            HabitFrequencySection(
                frequencyType = uiState.frequencyType,
                onFrequencyTypeChange = { viewModel.onEvent(EditHabitEvent.FrequencyTypeChanged(it)) },
                selectedDays = uiState.selectedDaysOfWeek,
                onDaysOfWeekChange = { viewModel.onEvent(EditHabitEvent.DaysOfWeekChanged(it)) },
                timesPerPeriod = uiState.timesPerPeriod,
                onTimesPerPeriodChange = { viewModel.onEvent(EditHabitEvent.TimesPerPeriodChanged(it)) },
                periodType = uiState.periodType,
                onPeriodTypeChange = { viewModel.onEvent(EditHabitEvent.PeriodTypeChanged(it)) },
                habitColor = habitColor
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Блок прогресса (зависит от типа привычки)
            HabitProgressSection(
                habitType = uiState.habitType,
                targetValue = uiState.targetValue,
                onTargetValueChange = { viewModel.onEvent(EditHabitEvent.TargetValueChanged(it)) },
                unitOfMeasurement = uiState.unitOfMeasurement,
                onUnitOfMeasurementChange = { viewModel.onEvent(EditHabitEvent.UnitOfMeasurementChanged(it)) },
                targetStreak = uiState.targetStreak,
                onTargetStreakChange = { viewModel.onEvent(EditHabitEvent.TargetStreakChanged(it)) },
                habitColor = habitColor
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Блок напоминаний
            HabitReminderSection(
                reminderEnabled = uiState.reminderEnabled,
                onReminderEnabledChange = { viewModel.onEvent(EditHabitEvent.ReminderEnabledChanged(it)) },
                reminderTime = uiState.reminderTime,
                onReminderTimeClick = {
                    tempSelectedTime = uiState.reminderTime
                    showTimePickerDialog = true
                },
                reminderDays = uiState.reminderDays,
                onReminderDaysChange = { viewModel.onEvent(EditHabitEvent.ReminderDaysChanged(it)) },
                habitColor = habitColor
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Блок тегов
            HabitTagsSection(
                tags = tags,
                selectedTagIds = uiState.selectedTagIds,
                onTagsChange = { viewModel.onEvent(EditHabitEvent.TagsChanged(it)) },
                onAddTagClick = { showTagDialog = true },
                habitColor = habitColor
            )

            // Дополнительное пространство внизу для FAB
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    // Диалоги
    if (showEmojiPicker) {
        EmojiPickerDialog(
            onEmojiSelected = { emoji ->
                viewModel.onEvent(EditHabitEvent.IconEmojiChanged(emoji))
                showEmojiPicker = false
            },
            onDismiss = { showEmojiPicker = false }
        )
    }

    if (showColorPicker) {
        ColorPickerDialog(
            initialColor = habitColor,
            onColorSelected = { color ->
                val hexColor = "#${Integer.toHexString(color.toArgb()).substring(2)}"
                viewModel.onEvent(EditHabitEvent.ColorChanged(hexColor))
                showColorPicker = false
            },
            onDismiss = { showColorPicker = false }
        )
    }

    if (showCategoryDialog) {
        AddCategoryDialog(
            onCategoryAdded = { name, color ->
                viewModel.onEvent(EditHabitEvent.CreateNewCategory(name, color))
                showCategoryDialog = false
            },
            onDismiss = { showCategoryDialog = false }
        )
    }

    if (showTagDialog) {
        AddTagDialog(
            onTagAdded = { name, color ->
                viewModel.onEvent(EditHabitEvent.CreateNewTag(name, color))
                showTagDialog = false
            },
            onDismiss = { showTagDialog = false }
        )
    }

    if (showTimePickerDialog) {
        CustomTimePickerDialog(
            initialTime = tempSelectedTime,
            onTimeSelected = { time ->
                viewModel.onEvent(EditHabitEvent.ReminderTimeChanged(time))
                showTimePickerDialog = false
            },
            onDismiss = { showTimePickerDialog = false }
        )
    }

    if (showDeleteConfirmation) {
        ConfirmDeleteDialog(
            title = "Удалить привычку?",
            text = "Вы действительно хотите удалить эту привычку? Это действие невозможно отменить.",
            onConfirm = {
                viewModel.onEvent(EditHabitEvent.DeleteHabit)
                showDeleteConfirmation = false
            },
            onDismiss = { showDeleteConfirmation = false }
        )
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
fun HabitTypeOption(
    type: HabitType,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = habitTypeColors[type] ?: MaterialTheme.colorScheme.primary
    val backgroundColor = if (isSelected) {
        color.copy(alpha = 0.1f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    }

    val borderColor = if (isSelected) {
        color
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier.clickable(onClick = onSelect)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) color else MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
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

@Composable
fun AddItemChip(
    text: String,
    onClick: () -> Unit,
    highlightColor: Color
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = Modifier
            .padding(end = 8.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = highlightColor,
                modifier = Modifier.size(16.dp)
            )

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

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

@Composable
fun HabitReminderSection(
    reminderEnabled: Boolean,
    onReminderEnabledChange: (Boolean) -> Unit,
    reminderTime: LocalTime,
    onReminderTimeClick: () -> Unit,
    reminderDays: Set<DayOfWeek>,
    onReminderDaysChange: (Set<DayOfWeek>) -> Unit,
    habitColor: Color
) {
    SectionTitle(title = "Напоминания", icon = Icons.Default.Notifications)

    SectionCard {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Включить напоминания",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )

                Switch(
                    checked = reminderEnabled,
                    onCheckedChange = onReminderEnabledChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = habitColor,
                        checkedTrackColor = habitColor.copy(alpha = 0.5f),
                        checkedIconColor = Color.White
                    )
                )
            }

            AnimatedVisibility(visible = reminderEnabled) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    // Выбор времени для напоминания
                    ReminderTimeSelector(
                        time = reminderTime,
                        onClick = onReminderTimeClick,
                        habitColor = habitColor
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Выбор дней для напоминаний
                    Text(
                        text = "В какие дни напоминать:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    DaysOfWeekSelector(
                        selectedDays = reminderDays,
                        onDaysChanged = onReminderDaysChange,
                        habitColor = habitColor
                    )
                }
            }
        }
    }
}

@Composable
fun ReminderTimeSelector(
    time: LocalTime,
    onClick: () -> Unit,
    habitColor: Color
) {
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = habitColor
                )

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = "Время напоминания",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Text(
                text = time.format(timeFormatter),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = habitColor
            )
        }
    }
}

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

@Composable
fun TagChip(
    tag: Tag,
    isSelected: Boolean,
    onSelect: () -> Unit,
    highlightColor: Color
) {
    val tagColor = tag.color?.let {
        try {
            Color(android.graphics.Color.parseColor(it))
        } catch (e: Exception) {
            highlightColor
        }
    } ?: highlightColor

    val backgroundColor = if (isSelected) {
        tagColor.copy(alpha = 0.1f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }

    val borderColor = if (isSelected) {
        tagColor
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier.clickable(onClick = onSelect)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(tagColor)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = tag.name,
                style = MaterialTheme.typography.bodySmall,
                color = if (isSelected) tagColor else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun SectionTitle(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 1.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier.padding(16.dp)
        ) {
            content()
        }
    }
}

@Composable
fun EmptyListPlaceholder(
    text: String,
    buttonText: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(text = buttonText)
        }
    }
}

@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalSpacing: Int = 0, // Используем простое целое число для вертикального отступа
    maxItemsInEachRow: Int = Int.MAX_VALUE,
    content: @Composable () -> Unit
) {
    Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val itemConstraints = constraints.copy(minWidth = 0)
        val placeables = measurables.map { it.measure(itemConstraints) }

        val rows = mutableListOf<List<androidx.compose.ui.layout.Placeable>>()
        var currentRow = mutableListOf<androidx.compose.ui.layout.Placeable>()
        var currentRowWidth = 0
        var currentRowCount = 0

        placeables.forEach { placeable ->
            if (currentRowWidth + placeable.width > constraints.maxWidth || currentRowCount >= maxItemsInEachRow) {
                rows.add(currentRow)
                currentRow = mutableListOf()
                currentRowWidth = 0
                currentRowCount = 0
            }

            currentRow.add(placeable)
            currentRowWidth += placeable.width
            currentRowCount++
        }

        if (currentRow.isNotEmpty()) {
            rows.add(currentRow)
        }

        // Вычисляем полную высоту с фиксированным вертикальным отступом
        val height = rows.sumOf { row ->
            row.maxOfOrNull { it.height } ?: 0
        } + (rows.size - 1) * verticalSpacing

        layout(constraints.maxWidth, height) {
            var y = 0

            rows.forEach { row ->
                val rowHeight = row.maxOfOrNull { it.height } ?: 0
                val padding = when (horizontalArrangement) {
                    Arrangement.Start -> 0
                    Arrangement.End -> constraints.maxWidth - row.sumOf { it.width }
                    Arrangement.Center -> (constraints.maxWidth - row.sumOf { it.width }) / 2
                    else -> 0
                }

                var x = padding

                row.forEach { placeable ->
                    placeable.place(x, y)
                    x += placeable.width
                }

                y += rowHeight + verticalSpacing
            }
        }
    }
}

// Исправленные диалоговые окна
@Composable
fun ColorPickerDialog(
    initialColor: Color,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    val materialColors = listOf(
        Color(0xFFE57373), // Red
        Color(0xFFF06292), // Pink
        Color(0xFFBA68C8), // Purple
        Color(0xFF9575CD), // Deep Purple
        Color(0xFF7986CB), // Indigo
        Color(0xFF64B5F6), // Blue
        Color(0xFF4FC3F7), // Light Blue
        Color(0xFF4DD0E1), // Cyan
        Color(0xFF4DB6AC), // Teal
        Color(0xFF81C784), // Green
        Color(0xFFAED581), // Light Green
        Color(0xFFFFD54F), // Amber
        Color(0xFFFFB74D), // Orange
        Color(0xFFFF8A65), // Deep Orange
        Color(0xFFA1887F), // Brown
        Color(0xFF90A4AE)  // Blue Grey
    )

    var selectedColor by remember { mutableStateOf(initialColor) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Выберите цвет") },
        text = {
            Column {
                // Сетка с цветами
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(materialColors) { color ->
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = 2.dp,
                                    color = if (selectedColor == color) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = color }
                        ) {
                            if (selectedColor == color) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Выбранный цвет
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = selectedColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {}
            }
        },
        confirmButton = {
            Button(
                onClick = { onColorSelected(selectedColor) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = selectedColor
                )
            ) {
                Text("Выбрать")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Отменить")
            }
        }
    )
}

@Composable
fun AddCategoryDialog(
    onCategoryAdded: (name: String, color: String) -> Unit,
    onDismiss: () -> Unit
) {
    var categoryName by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(Color(0xFF6200EE)) } // Default purple
    var showColorPicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новая категория") },
        text = {
            Column {
                OutlinedTextField(
                    value = categoryName,
                    onValueChange = { categoryName = it },
                    label = { Text("Название категории") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = selectedColor,
                        focusedLabelColor = selectedColor
                    ),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Цвет категории:",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showColorPicker = true }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(selectedColor)
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Text(
                            text = "Изменить цвет",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (categoryName.isNotBlank()) {
                        // Convert color to hex string
                        val hexColor = "#${Integer.toHexString(selectedColor.toArgb()).substring(2)}"
                        onCategoryAdded(categoryName, hexColor)
                    }
                },
                enabled = categoryName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = selectedColor
                )
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

    if (showColorPicker) {
        ColorPickerDialog(
            initialColor = selectedColor,
            onColorSelected = {
                selectedColor = it
                showColorPicker = false
            },
            onDismiss = { showColorPicker = false }
        )
    }
}

@Composable
fun AddTagDialog(
    onTagAdded: (name: String, color: String) -> Unit,
    onDismiss: () -> Unit
) {
    var tagName by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(Color(0xFF6200EE)) } // Default purple
    var showColorPicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новый тег") },
        text = {
            Column {
                OutlinedTextField(
                    value = tagName,
                    onValueChange = { tagName = it },
                    label = { Text("Название тега") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = selectedColor,
                        focusedLabelColor = selectedColor
                    ),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Цвет тега:",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showColorPicker = true }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(selectedColor)
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Text(
                            text = "Изменить цвет",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (tagName.isNotBlank()) {
                        // Convert color to hex string
                        val hexColor = "#${Integer.toHexString(selectedColor.toArgb()).substring(2)}"
                        onTagAdded(tagName, hexColor)
                    }
                },
                enabled = tagName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = selectedColor
                )
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

    if (showColorPicker) {
        ColorPickerDialog(
            initialColor = selectedColor,
            onColorSelected = {
                selectedColor = it
                showColorPicker = false
            },
            onDismiss = { showColorPicker = false }
        )
    }
}

@Composable
fun CustomTimePickerDialog(
    initialTime: LocalTime,
    onTimeSelected: (LocalTime) -> Unit,
    onDismiss: () -> Unit
) {
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    var selectedHour by remember { mutableStateOf(initialTime.hour) }
    var selectedMinute by remember { mutableStateOf(initialTime.minute) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Выберите время напоминания") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Часы
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Час",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(80.dp)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    selectedHour = if (selectedHour == 0) 23 else selectedHour - 1
                                }
                            ) {
                                Icon(Icons.Default.ArrowDropUp, contentDescription = "Вверх")
                            }

                            Text(
                                text = String.format("%02d", selectedHour),
                                style = MaterialTheme.typography.headlineMedium,
                                modifier = Modifier.width(48.dp),
                                textAlign = TextAlign.Center
                            )

                            IconButton(
                                onClick = {
                                    selectedHour = if (selectedHour == 23) 0 else selectedHour + 1
                                }
                            ) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Вниз")
                            }
                        }
                    }

                    Text(
                        text = ":",
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center
                    )

                    // Минуты
                    Column {
                        Text(
                            text = "Минуты",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(80.dp)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    selectedMinute = if (selectedMinute == 0) 59 else selectedMinute - 1
                                }
                            ) {
                                Icon(Icons.Default.ArrowDropUp, contentDescription = "Вверх")
                            }

                            Text(
                                text = String.format("%02d", selectedMinute),
                                style = MaterialTheme.typography.headlineMedium,
                                modifier = Modifier.width(48.dp),
                                textAlign = TextAlign.Center
                            )

                            IconButton(
                                onClick = {
                                    selectedMinute = if (selectedMinute == 59) 0 else selectedMinute + 1
                                }
                            ) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Вниз")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = "Выбранное время: ${String.format("%02d:%02d", selectedHour, selectedMinute)}",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onTimeSelected(LocalTime.of(selectedHour, selectedMinute))
                }
            ) {
                Text("Выбрать")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Отмена")
            }
        }
    )
}