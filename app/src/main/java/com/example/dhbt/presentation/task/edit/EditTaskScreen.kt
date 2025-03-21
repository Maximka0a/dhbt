@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.dhbt.presentation.task.edit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.dhbt.domain.model.Category
import com.example.dhbt.domain.model.RecurrenceType
import com.example.dhbt.domain.model.Subtask
import com.example.dhbt.domain.model.Tag
import com.example.dhbt.domain.model.TaskPriority
import com.example.dhbt.presentation.components.TaskPrioritySelector
import com.example.dhbt.presentation.components.ColorSelector
import com.example.dhbt.presentation.components.DatePickerDialog
import com.example.dhbt.presentation.components.TimePickerDialog
import kotlinx.coroutines.flow.collectLatest
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalTime
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTaskScreen(
    navController: NavController,
    viewModel: EditTaskViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val categories by viewModel.categoryState.collectAsState()
    val tags by viewModel.tagsState.collectAsState()
    val subtasks by viewModel.subtasks.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Dialogs state
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showTagDialog by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }

    // Focus management
    val focusManager = LocalFocusManager.current
    val subtaskFocusRequester = remember { FocusRequester() }
    var subtaskInput by remember { mutableStateOf("") }

    // Listen for events
    LaunchedEffect(key1 = true) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is EditTaskEvent.NavigateBack -> {
                    navController.popBackStack()
                }
                is EditTaskEvent.ShowError -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is EditTaskEvent.ClearSubtaskInput -> {
                    subtaskInput = ""
                    subtaskFocusRequester.requestFocus()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (uiState.isEditing) "Edit Task" else "Create Task")
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.onCancel() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            BottomAppBar {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { viewModel.onCancel() }) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { viewModel.saveTask() }) {
                        Text("Save")
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title
            item {
                Text(
                    text = "Title",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
                OutlinedTextField(
                    value = uiState.title,
                    onValueChange = { viewModel.onTitleChanged(it) },
                    placeholder = { Text("Enter task title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next
                    ),
                    isError = uiState.title.isBlank()
                )
                if (uiState.title.isBlank()) {
                    Text(
                        text = "Title is required",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }
            }

            // Description
            item {
                Text(
                    text = "Description",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                OutlinedTextField(
                    value = uiState.description,
                    onValueChange = { viewModel.onDescriptionChanged(it) },
                    placeholder = { Text("Enter task description (optional)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences
                    )
                )
            }

            // Color selection
            item {
                Text(
                    text = "Color Label",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Display selected color
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(uiState.color?.let { Color(android.graphics.Color.parseColor(it)) }
                                ?: MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            .clickable { showColorPicker = true }
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Text(
                        text = uiState.color ?: "No color selected",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // Color picker dialog
                if (showColorPicker) {
                    AlertDialog(
                        onDismissRequest = { showColorPicker = false },
                        title = { Text("Select color") },
                        text = {
                            ColorSelector(
                                selectedColor = uiState.color,
                                onColorSelected = {
                                    viewModel.onColorSelected(it)
                                    showColorPicker = false
                                }
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = { showColorPicker = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }

            // Category selection
            item {
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val selectedCategory = categories.find { it.id == uiState.categoryId }

                    OutlinedButton(
                        onClick = { showCategoryDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(selectedCategory?.name ?: "Select Category")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(onClick = { showCategoryDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Category")
                    }
                }

                // Category selection dialog
                if (showCategoryDialog) {
                    CategorySelectionDialog(
                        categories = categories,
                        selectedCategoryId = uiState.categoryId,
                        onCategorySelected = { categoryId ->
                            viewModel.onCategorySelected(categoryId)
                            showCategoryDialog = false
                        },
                        onAddNewCategory = { name, color ->
                            viewModel.onAddNewCategory(name, color)
                            showCategoryDialog = false
                        },
                        onDismissRequest = { showCategoryDialog = false }
                    )
                }
            }

            // Due date and time
            item {
                Text(
                    text = "Due Date & Time",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Date selection
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            uiState.dueDate?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
                                ?: "Select Date"
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Time selection
                    OutlinedButton(
                        onClick = { showTimePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            uiState.dueTime?.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
                                ?: "Select Time"
                        )
                    }
                }

                // Date picker dialog
                if (showDatePicker) {
                    DatePickerDialog(
                        initialDate = uiState.dueDate ?: LocalDate.now(),
                        onDateSelected = {
                            viewModel.onDueDateChanged(it)
                            showDatePicker = false
                        },
                        onDismiss = { showDatePicker = false }
                    )
                }

                // Time picker dialog
                if (showTimePicker) {
                    TimePickerDialog(
                        initialTime = uiState.dueTime ?: LocalTime.now(),
                        onTimeSelected = {
                            viewModel.onDueTimeChanged(it)
                            showTimePicker = false
                        },
                        onDismiss = { showTimePicker = false }
                    )
                }
            }

            // Duration
            item {
                Text(
                    text = "Duration (minutes)",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                OutlinedTextField(
                    value = uiState.duration?.toString() ?: "",
                    onValueChange = {
                        viewModel.onDurationChanged(it.toIntOrNull())
                    },
                    placeholder = { Text("Enter duration in minutes") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    )
                )
            }

            // Priority
            item {
                Text(
                    text = "Priority",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                TaskPrioritySelector(
                    selectedPriority = uiState.priority,
                    onPrioritySelected = { viewModel.onPriorityChanged(it) }
                )
            }

            // Eisenhower Quadrant
            item {
                Text(
                    text = "Eisenhower Matrix",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                EisenhowerQuadrantSelector(
                    selectedQuadrant = uiState.eisenhowerQuadrant,
                    onQuadrantSelected = { viewModel.onEisenhowerQuadrantChanged(it) }
                )
            }

            // Tags
            item {
                Text(
                    text = "Tags",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LazyRow(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(tags) { tag ->
                            TagChip(
                                tag = tag,
                                isSelected = uiState.selectedTagIds.contains(tag.id),
                                onClick = { viewModel.onTagToggled(tag.id) }
                            )
                        }
                    }

                    IconButton(onClick = { showTagDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Tag")
                    }
                }

                // Tag creation dialog
                if (showTagDialog) {
                    TagCreationDialog(
                        onCreateTag = { name, color ->
                            viewModel.onAddNewTag(name, color)
                            showTagDialog = false
                        },
                        onDismissRequest = { showTagDialog = false }
                    )
                }
            }

            // Subtasks
            item {
                Text(
                    text = "Subtasks",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Existing subtasks
                    subtasks.forEachIndexed { index, subtask ->
                        SubtaskItem(
                            subtask = subtask,
                            onToggleCompletion = { viewModel.toggleSubtaskCompletion(subtask.id) },
                            onDelete = { viewModel.removeSubtask(subtask.id) }
                        )
                    }

                    // Add new subtask
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = subtaskInput,
                            onValueChange = { subtaskInput = it },
                            placeholder = { Text("New subtask") },
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(subtaskFocusRequester),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    viewModel.addSubtask(subtaskInput)
                                }
                            )
                        )

                        IconButton(
                            onClick = { viewModel.addSubtask(subtaskInput) }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add subtask")
                        }
                    }
                }
            }

            // Recurrence
            item {
                Text(
                    text = "Recurrence",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                RecurrenceSelector(
                    selectedType = uiState.recurrenceType,
                    selectedDays = uiState.daysOfWeek,
                    monthDay = uiState.monthDay,
                    customInterval = uiState.customInterval,
                    onTypeSelected = { viewModel.onRecurrenceTypeChanged(it) },
                    onDayToggled = { viewModel.onDayOfWeekToggled(it) },
                    onMonthDayChanged = { viewModel.onMonthDayChanged(it) },
                    onCustomIntervalChanged = { viewModel.onCustomIntervalChanged(it) }
                )
            }

            // Pomodoro sessions
            item {
                Text(
                    text = "Estimated Pomodoros",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                OutlinedTextField(
                    value = uiState.estimatedPomodoros?.toString() ?: "",
                    onValueChange = {
                        viewModel.onEstimatedPomodorosChanged(it.toIntOrNull())
                    },
                    placeholder = { Text("Number of pomodoro sessions") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() }
                    )
                )
            }

            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun SubtaskItem(
    subtask: Subtask,
    onToggleCompletion: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = subtask.isCompleted,
            onCheckedChange = { onToggleCompletion() }
        )

        Text(
            text = subtask.title,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            style = MaterialTheme.typography.bodyMedium
        )

        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete subtask")
        }
    }
}

@Composable
fun TagChip(
    tag: Tag,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        tag.color?.let { Color(android.graphics.Color.parseColor(it)) }
            ?: MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val textColor = if (isSelected) {
        Color.White
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = Modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor
    ) {
        Text(
            text = tag.name,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun CategorySelectionDialog(
    categories: List<Category>,
    selectedCategoryId: String?,
    onCategorySelected: (String?) -> Unit,
    onAddNewCategory: (String, String) -> Unit,
    onDismissRequest: () -> Unit
) {
    var showNewCategoryForm by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    var newCategoryColor by remember { mutableStateOf("#FF5733") }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Select Category") },
        text = {
            if (showNewCategoryForm) {
                Column {
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        label = { Text("Category Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    ColorSelector(
                        selectedColor = newCategoryColor,
                        onColorSelected = { newCategoryColor = it }
                    )
                }
            } else {
                LazyColumn {
                    item {
                        Text(
                            text = "None",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onCategorySelected(null) }
                                .padding(vertical = 12.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Divider()
                    }

                    items(categories) { category ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onCategorySelected(category.id) }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Category color indicator
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(category.color?.let { Color(android.graphics.Color.parseColor(it)) }
                                        ?: MaterialTheme.colorScheme.primary)
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            Text(
                                text = category.name,
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Spacer(modifier = Modifier.weight(1f))

                            // Selection indicator
                            if (category.id == selectedCategoryId) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Divider()
                    }

                    item {
                        // Add new category option
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showNewCategoryForm = true }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Category")

                            Spacer(modifier = Modifier.width(16.dp))

                            Text(
                                text = "Add New Category",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (showNewCategoryForm) {
                TextButton(
                    onClick = {
                        if (newCategoryName.isNotBlank()) {
                            onAddNewCategory(newCategoryName, newCategoryColor)
                        }
                    },
                    enabled = newCategoryName.isNotBlank()
                ) {
                    Text("Create")
                }
            } else {
                TextButton(onClick = onDismissRequest) {
                    Text("Cancel")
                }
            }
        },
        dismissButton = {
            if (showNewCategoryForm) {
                TextButton(onClick = { showNewCategoryForm = false }) {
                    Text("Back")
                }
            }
        }
    )
}

@Composable
fun TagCreationDialog(
    onCreateTag: (String, String) -> Unit,
    onDismissRequest: () -> Unit
) {
    var tagName by remember { mutableStateOf("") }
    var tagColor by remember { mutableStateOf("#5E97F6") }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Create New Tag") },
        text = {
            Column {
                OutlinedTextField(
                    value = tagName,
                    onValueChange = { tagName = it },
                    label = { Text("Tag Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                ColorSelector(
                    selectedColor = tagColor,
                    onColorSelected = { tagColor = it }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (tagName.isNotBlank()) {
                        onCreateTag(tagName, tagColor)
                    }
                },
                enabled = tagName.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EisenhowerQuadrantSelector(
    selectedQuadrant: Int?,
    onQuadrantSelected: (Int?) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Important & Urgent (Q1)
            QuadrantButton(
                title = "Important & Urgent",
                description = "Do First",
                isSelected = selectedQuadrant == 1,
                onClick = { onQuadrantSelected(if (selectedQuadrant == 1) null else 1) },
                color = Color(0xFFF44336), // Red
                modifier = Modifier
                    .weight(1f)
                    .height(100.dp)
                    .padding(end = 4.dp)
            )

            // Important & Not Urgent (Q2)
            QuadrantButton(
                title = "Important & Not Urgent",
                description = "Schedule",
                isSelected = selectedQuadrant == 2,
                onClick = { onQuadrantSelected(if (selectedQuadrant == 2) null else 2) },
                color = Color(0xFF4CAF50), // Green
                modifier = Modifier
                    .weight(1f)
                    .height(100.dp)
                    .padding(start = 4.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Not Important & Urgent (Q3)
            QuadrantButton(
                title = "Not Important & Urgent",
                description = "Delegate",
                isSelected = selectedQuadrant == 3,
                onClick = { onQuadrantSelected(if (selectedQuadrant == 3) null else 3) },
                color = Color(0xFFFF9800), // Orange
                modifier = Modifier
                    .weight(1f)
                    .height(100.dp)
                    .padding(end = 4.dp, top = 8.dp)
            )

            // Not Important & Not Urgent (Q4)
            QuadrantButton(
                title = "Not Important & Not Urgent",
                description = "Eliminate",
                isSelected = selectedQuadrant == 4,
                onClick = { onQuadrantSelected(if (selectedQuadrant == 4) null else 4) },
                color = Color(0xFF2196F3), // Blue
                modifier = Modifier
                    .weight(1f)
                    .height(100.dp)
                    .padding(start = 4.dp, top = 8.dp)
            )
        }
    }
}

@Composable
fun QuadrantButton(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    color: Color,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) color else color.copy(alpha = 0.2f)
    val contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface

    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun RecurrenceSelector(
    selectedType: RecurrenceType?,
    selectedDays: Set<Int>,
    monthDay: Int?,
    customInterval: Int?,
    onTypeSelected: (RecurrenceType?) -> Unit,
    onDayToggled: (Int) -> Unit,
    onMonthDayChanged: (Int?) -> Unit,
    onCustomIntervalChanged: (Int?) -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Recurrence type selection
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val recurrenceTypes = listOf(null) + RecurrenceType.values().toList()

            recurrenceTypes.forEach { type ->
                val isSelected = selectedType == type
                val backgroundColor = if (isSelected)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant

                val textColor = if (isSelected)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurfaceVariant

                Surface(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .clickable { onTypeSelected(type) },
                    shape = RoundedCornerShape(16.dp),
                    color = backgroundColor
                ) {
                    Text(
                        text = type?.name ?: "None",
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Additional options based on selected type
        AnimatedVisibility(
            visible = selectedType != null,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                when (selectedType) {
                    RecurrenceType.DAILY -> {
                        // No additional options for daily recurrence
                    }
                    RecurrenceType.WEEKLY -> {
                        Text(
                            text = "Repeat on days:",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // Day of week selection
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val daysOfWeek = listOf("M", "T", "W", "T", "F", "S", "S")

                            for (i in 1..7) {
                                val isSelected = selectedDays.contains(i)
                                val backgroundColor = if (isSelected)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.surfaceVariant

                                val textColor = if (isSelected)
                                    MaterialTheme.colorScheme.onPrimary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant

                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(backgroundColor)
                                        .clickable { onDayToggled(i) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = daysOfWeek[i-1],
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = textColor
                                    )
                                }
                            }
                        }
                    }
                    RecurrenceType.MONTHLY -> {
                        Text(
                            text = "Repeat on day of month:",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        OutlinedTextField(
                            value = monthDay?.toString() ?: "",
                            onValueChange = {
                                val day = it.toIntOrNull()
                                if (day == null || (day in 1..31)) {
                                    onMonthDayChanged(day)
                                }
                            },
                            placeholder = { Text("Day (1-31)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number
                            )
                        )
                    }
                    RecurrenceType.CUSTOM -> {
                        Text(
                            text = "Repeat every X days:",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        OutlinedTextField(
                            value = customInterval?.toString() ?: "",
                            onValueChange = {
                                val interval = it.toIntOrNull()
                                if (interval == null || interval > 0) {
                                    onCustomIntervalChanged(interval)
                                }
                            },
                            placeholder = { Text("Number of days") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number
                            )
                        )
                    }
                    null -> {
                        // No additional options
                    }

                    RecurrenceType.YEARLY ->    {
                            //todo доделать
                    }
                }
            }
        }
    }
}