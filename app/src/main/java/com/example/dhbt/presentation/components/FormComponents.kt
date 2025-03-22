package com.example.dhbt.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.dhbt.R
import com.example.dhbt.domain.model.Category
import com.example.dhbt.domain.model.CategoryType
import com.example.dhbt.domain.model.Subtask
import com.example.dhbt.domain.model.TaskPriority
import com.example.dhbt.presentation.theme.DHbtTheme
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DHbtTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    isError: Boolean = false,
    errorMessage: String? = null,
    maxLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default.copy(
        capitalization = KeyboardCapitalization.Sentences,
        imeAction = if (maxLines > 1) ImeAction.Done else ImeAction.Next
    ),
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    val focusManager = LocalFocusManager.current

    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            isError = isError,
            maxLines = maxLines,
            keyboardOptions = keyboardOptions,
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) },
                onDone = { focusManager.clearFocus() }
            ),
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            modifier = Modifier.fillMaxWidth()
        )

        if (isError && !errorMessage.isNullOrEmpty()) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    value: Long?,
    onValueChange: (Long) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }

    val formattedDate = remember(value) {
        value?.let { dateFormat.format(Date(it)) } ?: ""
    }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = formattedDate,
            onValueChange = { /* Readonly */ },
            label = { Text(label) },
            readOnly = true,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null
                )
            },
            trailingIcon = {
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.select_date)
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDatePicker = true }
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = value
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let(onValueChange)
                    showDatePicker = false
                }) {
                    Text(stringResource(id = R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerField(
    value: String?,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    var showTimePicker by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = value ?: "",
            onValueChange = { /* Readonly */ },
            label = { Text(label) },
            readOnly = true,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = null
                )
            },
            trailingIcon = {
                IconButton(onClick = { showTimePicker = true }) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.select_time)
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showTimePicker = true }
        )
    }

    // Реализация TimePickerDialog не включена, так как требует дополнительных компонентов
    // Это место для будущей реализации времени пикера
}

@Composable
fun PrioritySelector(
    selectedPriority: TaskPriority,
    onPriorityChange: (TaskPriority) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(vertical = 8.dp)) {
        Text(
            text = stringResource(R.string.priority),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PriorityChip(
                text = stringResource(R.string.low_priority),
                selected = selectedPriority == TaskPriority.LOW,
                color = Color(0xFF4CAF50), // Зеленый для низкого приоритета
                onClick = { onPriorityChange(TaskPriority.LOW) }
            )

            PriorityChip(
                text = stringResource(R.string.medium_priority),
                selected = selectedPriority == TaskPriority.MEDIUM,
                color = Color(0xFFFFC107), // Желтый для среднего приоритета
                onClick = { onPriorityChange(TaskPriority.MEDIUM) }
            )

            PriorityChip(
                text = stringResource(R.string.high_priority),
                selected = selectedPriority == TaskPriority.HIGH,
                color = Color(0xFFF44336), // Красный для высокого приоритета
                onClick = { onPriorityChange(TaskPriority.HIGH) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PriorityChip(
    text: String,
    selected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text) },
        leadingIcon = if (selected) {
            {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = color
                )
            }
        } else null,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = color.copy(alpha = 0.12f),
            selectedLabelColor = color
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySelector(
    categories: List<Category>,
    selectedCategoryId: String?,
    onCategorySelect: (String?) -> Unit,
    onAddCategoryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val selectedCategory = remember(selectedCategoryId, categories) {
        categories.find { it.id == selectedCategoryId }
    }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = selectedCategory?.name ?: "",
            onValueChange = { /* Readonly */ },
            label = { Text(stringResource(R.string.category)) },
            readOnly = true,
            leadingIcon = {
                selectedCategory?.let { category ->
                    Box(modifier = Modifier.padding(start = 8.dp)) {
                        ColorIndicator(
                            colorString = category.color,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                } ?: Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null
                )
            },
            trailingIcon = {
                IconButton(onClick = { expanded = true }) {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = stringResource(R.string.select_category)
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(IntrinsicSize.Max)
        ) {
            // Option to clear category selection
            DropdownMenuItem(
                text = { Text("Без категории") },
                onClick = {
                    onCategorySelect(null)
                    expanded = false
                }
            )

            HorizontalDivider()

            // List of categories
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.name) },
                    leadingIcon = {
                        ColorIndicator(
                            colorString = category.color,
                            modifier = Modifier.size(12.dp)
                        )
                    },
                    onClick = {
                        onCategorySelect(category.id)
                        expanded = false
                    }
                )
            }

            HorizontalDivider()

            // Option to add new category
            DropdownMenuItem(
                text = { Text("Добавить категорию...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null
                    )
                },
                onClick = {
                    onAddCategoryClick()
                    expanded = false
                }
            )
        }
    }
}

@Composable
fun SubtasksList(
    subtasks: List<Subtask>,
    onSubtaskCheckedChange: (String, Boolean) -> Unit,
    onSubtaskDelete: (String) -> Unit,
    onSubtaskEdit: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.subtasks),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        subtasks.forEach { subtask ->
            SubtaskItem(
                subtask = subtask,
                onCheckedChange = { isCompleted -> onSubtaskCheckedChange(subtask.id, isCompleted) },
                onDelete = { onSubtaskDelete(subtask.id) },
                onEdit = { newTitle -> onSubtaskEdit(subtask.id, newTitle) }
            )
        }
    }
}

@Composable
private fun SubtaskItem(
    subtask: Subtask,
    onCheckedChange: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onEdit: (String) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var editText by remember { mutableStateOf(subtask.title) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = subtask.isCompleted,
            onCheckedChange = { onCheckedChange(it) }
        )

        if (isEditing) {
            OutlinedTextField(
                value = editText,
                onValueChange = { editText = it },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                singleLine = true
            )

            IconButton(onClick = {
                onEdit(editText)
                isEditing = false
            }) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Сохранить"
                )
            }

        } else {
            Text(
                text = subtask.title,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            )

            IconButton(onClick = { isEditing = true }) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Редактировать"
                )
            }
        }

        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Удалить"
            )
        }
    }
}

@Composable
fun AddSubtaskField(
    onAddSubtask: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf("") }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text(stringResource(R.string.add_subtask)) },
            modifier = Modifier.weight(1f),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (text.isNotBlank()) {
                        onAddSubtask(text)
                        text = ""
                    }
                }
            )
        )

        IconButton(
            onClick = {
                if (text.isNotBlank()) {
                    onAddSubtask(text)
                    text = ""
                }
            }
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(R.string.add_subtask)
            )
        }
    }
}



@Preview(showBackground = true)
@Composable
fun DHbtTextFieldPreview() {
    DHbtTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            DHbtTextField(
                value = "Написать отчет о проделанной работе",
                onValueChange = {},
                label = "Название задачи",
                placeholder = "Введите название задачи...",
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DatePickerFieldPreview() {
    DHbtTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            DatePickerField(
                value = System.currentTimeMillis(),
                onValueChange = {},
                label = "Дата",
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TimePickerFieldPreview() {
    DHbtTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            TimePickerField(
                value = "14:30",
                onValueChange = {},
                label = "Время",
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PrioritySelectorPreview() {
    DHbtTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            PrioritySelector(
                selectedPriority = TaskPriority.MEDIUM,
                onPriorityChange = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CategorySelectorPreview() {
    DHbtTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            val categories = remember {
                listOf(
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
                )
            }

            CategorySelector(
                categories = categories,
                selectedCategoryId = "1",
                onCategorySelect = {},
                onAddCategoryClick = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SubtasksListPreview() {
    DHbtTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            val subtasks = remember {
                listOf(
                    Subtask(
                        id = "1",
                        taskId = "task1",
                        title = "Изучить документацию",
                        isCompleted = true
                    ),
                    Subtask(
                        id = "2",
                        taskId = "task1",
                        title = "Создать прототип",
                        isCompleted = false
                    ),
                    Subtask(
                        id = "3",
                        taskId = "task1",
                        title = "Написать тесты",
                        isCompleted = false
                    )
                )
            }

            Column(modifier = Modifier.padding(16.dp)) {
                SubtasksList(
                    subtasks = subtasks,
                    onSubtaskCheckedChange = { _, _ -> },
                    onSubtaskDelete = {},
                    onSubtaskEdit = { _, _ -> }
                )

                AddSubtaskField(
                    onAddSubtask = {}
                )
            }
        }
    }
}