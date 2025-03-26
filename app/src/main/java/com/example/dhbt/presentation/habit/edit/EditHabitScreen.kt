@file:OptIn(ExperimentalLayoutApi::class)

package com.example.dhbt.presentation.habit.edit

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.dhbt.presentation.components.ConfirmDeleteDialog
import com.example.dhbt.presentation.components.EmojiPickerDialog
import com.example.dhbt.presentation.habit.edit.components.HabitBasicInfoSection
import com.example.dhbt.presentation.habit.edit.components.HabitCategorySection
import com.example.dhbt.presentation.habit.edit.components.HabitFrequencySection
import com.example.dhbt.presentation.habit.edit.components.HabitProgressSection
import com.example.dhbt.presentation.habit.edit.components.HabitReminderSection
import com.example.dhbt.presentation.habit.edit.components.HabitTagsSection
import com.example.dhbt.presentation.habit.edit.components.HabitTypeSection
import com.example.dhbt.presentation.habit.edit.components.dialogs.AddCategoryDialog
import com.example.dhbt.presentation.habit.edit.components.dialogs.AddTagDialog
import com.example.dhbt.presentation.habit.edit.components.dialogs.ColorPickerDialog
import com.example.dhbt.presentation.habit.edit.components.dialogs.CustomTimePickerDialog

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