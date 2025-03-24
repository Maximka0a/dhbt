package com.example.dhbt.presentation.eisenhower

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.dhbt.domain.model.Task
import com.example.dhbt.domain.model.TaskPriority
import kotlinx.coroutines.launch
import org.threeten.bp.Instant
import org.threeten.bp.LocalDate
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EisenhowerScreen(
    navController: NavController,
    viewModel: EisenhowerViewModel = hiltViewModel()
) {
    val quadrantTasks by viewModel.quadrantTasks.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val draggingItem = remember { mutableStateOf<Task?>(null) }
    val dragPosition = remember { mutableStateOf<Offset?>(null) }
    val currentQuadrant = remember { mutableStateOf<Int?>(null) }
    var showAddTaskDialog by remember { mutableStateOf(false) }

    // Когда появляется ошибка, показываем Snackbar
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Матрица Эйзенхауэра") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                ),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshData() }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Обновить данные"
                        )
                    }
                    // Фильтр по категориям
                    Box {
                        var expanded by remember { mutableStateOf(false) }

                        IconButton(onClick = { expanded = true }) {
                            Icon(
                                Icons.Default.FilterList,
                                contentDescription = "Фильтр по категориям"
                            )
                        }

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Все категории") },
                                onClick = {
                                    viewModel.selectCategory(null)
                                    expanded = false
                                }
                            )

                            categories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category.name) },
                                    onClick = {
                                        viewModel.selectCategory(category.id)
                                        expanded = false
                                    },
                                    leadingIcon = {
                                        if (category.iconEmoji != null) {
                                            Text(text = category.iconEmoji)
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        Color(android.graphics.Color.parseColor(category.color ?: "#6200EE"))
                                                    )
                                            )
                                        }
                                    },
                                    trailingIcon = {
                                        if (category.id == selectedCategoryId) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = "Выбрано"
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // Кнопка добавления новой задачи
                    IconButton(onClick = { showAddTaskDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Добавить задачу")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Основная матрица
                EisenhowerMatrix(
                    quadrantTasks = quadrantTasks,
                    onTaskClick = { taskId ->
                        // Навигация к деталям задачи
                        navController.navigate("taskDetail/$taskId")
                    },
                    onDragTask = { task, offset ->
                        draggingItem.value = task
                        dragPosition.value = offset
                    },
                    onDropTask = { task, quadrant ->
                        if (task.eisenhowerQuadrant != quadrant) {
                            viewModel.moveTaskToQuadrant(task.id, quadrant)
                        }
                        draggingItem.value = null
                        dragPosition.value = null
                        currentQuadrant.value = null
                    },
                    onQuadrantHover = { quadrant ->
                        currentQuadrant.value = quadrant
                    },
                    onAddInQuadrant = { quadrant ->
                        showAddTaskDialog = true
                        currentQuadrant.value = quadrant
                    },
                    currentDragQuadrant = currentQuadrant.value
                )

                // Перетаскиваемый элемент, который следует за пальцем
                dragPosition.value?.let { position ->
                    draggingItem.value?.let { task ->
                        Box(
                            modifier = Modifier
                                .offset(position.x.dp - 100.dp, position.y.dp - 30.dp)
                                .width(200.dp)
                                .zIndex(10f)
                        ) {
                            TaskItemFloat(task = task)
                        }
                    }
                }
            }
        }
    }

    // Диалог добавления новой задачи
    if (showAddTaskDialog) {
        var taskTitle by remember { mutableStateOf("") }
        var taskQuadrant by remember { mutableStateOf(currentQuadrant.value ?: 1) }
        var taskCategory by remember { mutableStateOf<String?>(selectedCategoryId) }

        AlertDialog(
            onDismissRequest = { showAddTaskDialog = false },
            title = { Text("Новая задача") },
            text = {
                Column(modifier = Modifier.padding(8.dp)) {
                    OutlinedTextField(
                        value = taskTitle,
                        onValueChange = { taskTitle = it },
                        label = { Text("Название задачи") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Квадрант", fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth()) {
                        QuadrantSelectionChip(
                            selected = taskQuadrant == 1,
                            onSelected = { taskQuadrant = 1 },
                            label = "Срочно и важно",
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        QuadrantSelectionChip(
                            selected = taskQuadrant == 2,
                            onSelected = { taskQuadrant = 2 },
                            label = "Важно, не срочно",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        QuadrantSelectionChip(
                            selected = taskQuadrant == 3,
                            onSelected = { taskQuadrant = 3 },
                            label = "Срочно, не важно",
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        QuadrantSelectionChip(
                            selected = taskQuadrant == 4,
                            onSelected = { taskQuadrant = 4 },
                            label = "Не срочно, не важно",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Категория", fontWeight = FontWeight.Bold)
                    if (categories.isEmpty()) {
                        Text(
                            "Нет доступных категорий",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    } else {
                        DropdownMenu(
                            expanded = false,  // Placeholder, реальное меню управляется внешним состоянием
                            onDismissRequest = { }
                        ) {
                            categories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category.name) },
                                    onClick = { taskCategory = category.id }
                                )
                            }
                        }

                        // Отображаем выбранную категорию или подсказку
                        val selectedCategory = categories.find { it.id == taskCategory }
                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    // Здесь должно открываться настоящее выпадающее меню
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (selectedCategory != null) {
                                    if (selectedCategory.iconEmoji != null) {
                                        Text(
                                            text = selectedCategory.iconEmoji,
                                            modifier = Modifier.padding(end = 8.dp)
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    Color(
                                                        android.graphics.Color.parseColor(
                                                            selectedCategory.color ?: "#6200EE"
                                                        )
                                                    )
                                                )
                                                .padding(end = 8.dp)
                                        )
                                    }
                                    Text(selectedCategory.name)
                                } else {
                                    Text(
                                        "Выберите категорию",
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }

                                Spacer(modifier = Modifier.weight(1f))

                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = "Выбрать категорию"
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (taskTitle.isNotBlank()) {
                            viewModel.createTaskInQuadrant(
                                title = taskTitle,
                                quadrant = taskQuadrant,
                                categoryId = taskCategory
                            )
                            showAddTaskDialog = false
                        }
                    },
                    enabled = taskTitle.isNotBlank()
                ) {
                    Text("Добавить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTaskDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
fun QuadrantSelectionChip(
    selected: Boolean,
    onSelected: () -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
        modifier = modifier.clickable { onSelected() }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
fun EisenhowerMatrix(
    quadrantTasks: Map<Int, List<Task>>,
    onTaskClick: (String) -> Unit,
    onDragTask: (Task, Offset) -> Unit,
    onDropTask: (Task, Int) -> Unit,
    onQuadrantHover: (Int) -> Unit,
    onAddInQuadrant: (Int) -> Unit,
    currentDragQuadrant: Int?
) {
    // Используем ширину экрана для определения размера квадрантов
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val quadrantSize = (screenWidth / 2) - 8.dp

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.weight(1f)) {
            // Квадрант 1 (Важно и Срочно)
            QuadrantBox(
                title = "Срочно и важно",
                subtitle = "Сделать немедленно",
                tasks = quadrantTasks[1] ?: emptyList(),
                color = Color(0xFFF44336).copy(alpha = 0.2f), // Красный с прозрачностью
                onTaskClick = onTaskClick,
                onDragTask = onDragTask,
                onDrop = { task -> onDropTask(task, 1) },
                onHover = { onQuadrantHover(1) },
                onAddClick = { onAddInQuadrant(1) },
                isHighlighted = currentDragQuadrant == 1,
                modifier = Modifier
                    .weight(1f)
                    .height(quadrantSize)
            )

            // Квадрант 2 (Важно, но не срочно)
            QuadrantBox(
                title = "Важно, не срочно",
                subtitle = "Запланировать",
                tasks = quadrantTasks[2] ?: emptyList(),
                color = Color(0xFF4CAF50).copy(alpha = 0.2f), // Зеленый с прозрачностью
                onTaskClick = onTaskClick,
                onDragTask = onDragTask,
                onDrop = { task -> onDropTask(task, 2) },
                onHover = { onQuadrantHover(2) },
                onAddClick = { onAddInQuadrant(2) },
                isHighlighted = currentDragQuadrant == 2,
                modifier = Modifier
                    .weight(1f)
                    .height(quadrantSize)
            )
        }

        Row(modifier = Modifier.weight(1f)) {
            // Квадрант 3 (Срочно, но не важно)
            QuadrantBox(
                title = "Срочно, не важно",
                subtitle = "Делегировать",
                tasks = quadrantTasks[3] ?: emptyList(),
                color = Color(0xFFFF9800).copy(alpha = 0.2f), // Оранжевый с прозрачностью
                onTaskClick = onTaskClick,
                onDragTask = onDragTask,
                onDrop = { task -> onDropTask(task, 3) },
                onHover = { onQuadrantHover(3) },
                onAddClick = { onAddInQuadrant(3) },
                isHighlighted = currentDragQuadrant == 3,
                modifier = Modifier
                    .weight(1f)
                    .height(quadrantSize)
            )

            // Квадрант 4 (Не срочно и не важно)
            QuadrantBox(
                title = "Не срочно, не важно",
                subtitle = "Исключить",
                tasks = quadrantTasks[4] ?: emptyList(),
                color = Color(0xFF2196F3).copy(alpha = 0.2f), // Синий с прозрачностью
                onTaskClick = onTaskClick,
                onDragTask = onDragTask,
                onDrop = { task -> onDropTask(task, 4) },
                onHover = { onQuadrantHover(4) },
                onAddClick = { onAddInQuadrant(4) },
                isHighlighted = currentDragQuadrant == 4,
                modifier = Modifier
                    .weight(1f)
                    .height(quadrantSize)
            )
        }
    }
}

@Composable
fun QuadrantBox(
    title: String,
    subtitle: String,
    tasks: List<Task>,
    color: Color,
    onTaskClick: (String) -> Unit,
    onDragTask: (Task, Offset) -> Unit,
    onDrop: (Task) -> Unit,
    onHover: () -> Unit,
    onAddClick: () -> Unit,
    isHighlighted: Boolean,
    modifier: Modifier = Modifier
) {
    val borderWidth = if (isHighlighted) 2.dp else 0.5.dp
    val borderColor = if (isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)

    Card(
        modifier = modifier
            .padding(4.dp)
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { /* ничего не делаем, так как перетаскивание начинается с элемента */ },
                    onDrag = { change, _ -> change.consume(); onHover() },
                    onDragEnd = { /* ничего не делаем */ },
                    onDragCancel = { /* ничего не делаем */ }
                )
            },
        colors = CardDefaults.cardColors(containerColor = color),
        border = BorderStroke(borderWidth, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = subtitle,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }

                IconButton(
                    onClick = onAddClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Добавить задачу",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Список задач
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(tasks) { task ->
                    TaskItem(
                        task = task,
                        onClick = { onTaskClick(task.id) },
                        onDrag = { offset -> onDragTask(task, offset) },
                        onDrop = { onDrop(task) }
                    )
                }
            }

            if (tasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Нет задач",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskItem(
    task: Task,
    onClick: () -> Unit,
    onDrag: (Offset) -> Unit,
    onDrop: () -> Unit
) {
    var isDragging by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        isDragging = true
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        onDrag(change.position)
                    },
                    onDragEnd = {
                        isDragging = false
                        onDrop()
                    },
                    onDragCancel = {
                        isDragging = false
                        onDrop()
                    }
                )
            },
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val priorityColor = when (task.priority) {
                TaskPriority.HIGH -> Color(0xFFF44336) // Красный
                TaskPriority.MEDIUM -> Color(0xFFFF9800) // Оранжевый
                TaskPriority.LOW -> Color(0xFF4CAF50) // Зеленый
            }

            // Индикатор приоритета
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(priorityColor)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Название задачи
            Text(
                text = task.title,
                maxLines = 2,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )

            // Дата выполнения, если есть
            task.dueDate?.let {
                Spacer(modifier = Modifier.width(8.dp))

                val dateFormatter = DateTimeFormatter.ofPattern("dd.MM")
                val date = Instant
                    .ofEpochMilli(it)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()

                Text(
                    text = date.format(dateFormatter),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }

    // Анимация исчезновения при перетаскивании
    AnimatedVisibility(
        visible = isDragging,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent
            )
        ) {
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth()
                    .height(38.dp),
                contentAlignment = Alignment.Center
            ) {
                // Пустой Box для сохранения места при перетаскивании
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskItemFloat(task: Task) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp
        )
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val priorityColor = when (task.priority) {
                TaskPriority.HIGH -> Color(0xFFF44336) // Красный
                TaskPriority.MEDIUM -> Color(0xFFFF9800) // Оранжевый
                TaskPriority.LOW -> Color(0xFF4CAF50) // Зеленый
            }

            // Индикатор приоритета
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(priorityColor)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Название задачи
            Text(
                text = task.title,
                maxLines = 1,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}