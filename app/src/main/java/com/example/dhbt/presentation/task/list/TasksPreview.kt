package com.example.dhbt.presentation.task.list

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.dhbt.domain.model.Category
import com.example.dhbt.domain.model.CategoryType
import com.example.dhbt.domain.model.RecurrenceType
import com.example.dhbt.domain.model.Subtask
import com.example.dhbt.domain.model.Tag
import com.example.dhbt.domain.model.Task
import com.example.dhbt.domain.model.TaskPriority
import com.example.dhbt.domain.model.TaskRecurrence
import com.example.dhbt.domain.model.TaskStatus
import com.example.dhbt.presentation.shared.EmptyStateMessage
import com.example.dhbt.presentation.theme.DHbtTheme
import org.threeten.bp.LocalDate

// Примерные данные для предпросмотра
object PreviewData {
    val now = System.currentTimeMillis()
    val oneDayMillis = 24 * 60 * 60 * 1000L
    // Создаем несколько тегов для использования в задачах
    val tags = listOf(
        Tag(id = "tag1", name = "Работа", color = "#4285F4"),
        Tag(id = "tag2", name = "Личное", color = "#34A853"),
        Tag(id = "tag3", name = "Здоровье", color = "#FBBC05"),
        Tag(id = "tag4", name = "Срочно", color = "#EA4335"),
        Tag(id = "tag5", name = "Важно", color = "#9C27B0"),
        Tag(id = "tag6", name = "Дом", color = "#795548"),
        Tag(id = "tag7", name = "Учеба", color = "#2196F3")
    )

    val categories = listOf(
        Category(
            id = "cat1",
            name = "Работа",
            color = "#4CAF50",
            iconEmoji = "💼",
            type = CategoryType.TASK,
            order = 0
        ),
        Category(
            id = "cat2",
            name = "Личное",
            color = "#2196F3",
            iconEmoji = "🏠",
            type = CategoryType.TASK,
            order = 1
        ),
        Category(
            id = "cat3",
            name = "Учеба",
            color = "#FF9800",
            iconEmoji = "📚",
            type = CategoryType.TASK,
            order = 2
        )
    )
    // Создаем список задач
    val tasks = listOf(
        // Активные задачи
        Task(
            id = "task1",
            title = "Завершить отчет по проекту",
            description = "Подготовить финальную версию отчета и отправить руководству",
            categoryId = "cat_work",
            color = "#4285F4",
            creationDate = now - 2 * oneDayMillis,
            dueDate = now + oneDayMillis,
            dueTime = "15:00",
            duration = 120,
            priority = TaskPriority.HIGH,
            status = TaskStatus.ACTIVE,
            eisenhowerQuadrant = 1, // важно и срочно
            estimatedPomodoroSessions = 4,
            subtasks = listOf(
                Subtask(id = "sub1_1", taskId = "task1", title = "Собрать данные", isCompleted = true),
                Subtask(id = "sub1_2", taskId = "task1", title = "Подготовить графики", isCompleted = true),
                Subtask(id = "sub1_3", taskId = "task1", title = "Написать выводы", isCompleted = false),
                Subtask(id = "sub1_4", taskId = "task1", title = "Проверить орфографию", isCompleted = false)
            ),
            tags = listOf(tags[0], tags[4]) // работа, важно
        ),
        Task(
            id = "task2",
            title = "Купить продукты",
            description = "Молоко, хлеб, овощи, фрукты, мясо",
            categoryId = "cat_home",
            creationDate = now - oneDayMillis,
            dueDate = now,
            priority = TaskPriority.MEDIUM,
            status = TaskStatus.ACTIVE,
            eisenhowerQuadrant = 3, // не важно, но срочно
            subtasks = listOf(
                Subtask(id = "sub2_1", taskId = "task2", title = "Молочные продукты", isCompleted = false),
                Subtask(id = "sub2_2", taskId = "task2", title = "Хлебобулочные изделия", isCompleted = false),
                Subtask(id = "sub2_3", taskId = "task2", title = "Овощи и фрукты", isCompleted = false)
            ),
            tags = listOf(tags[5]) // дом
        ),
        Task(
            id = "task3",
            title = "Утренняя пробежка",
            description = "5 км по парку",
            categoryId = "cat_health",
            color = "#FBBC05",
            creationDate = now - 5 * oneDayMillis,
            dueDate = now, // сегодня
            dueTime = "07:00",
            duration = 30,
            priority = TaskPriority.MEDIUM,
            status = TaskStatus.ACTIVE,
            eisenhowerQuadrant = 2, // важно, но не срочно
            tags = listOf(tags[2]), // здоровье
            recurrence = TaskRecurrence(
                id = "rec1",
                taskId = "task3",
                type = RecurrenceType.DAILY, // ежедневно
                startDate = now - 10 * oneDayMillis
            )
        ),

        // Выполненные задачи
        Task(
            id = "task4",
            title = "Оплатить счета",
            description = "Электричество, вода, интернет",
            categoryId = "cat_home",
            creationDate = now - 3 * oneDayMillis,
            dueDate = now - oneDayMillis, // вчера
            priority = TaskPriority.HIGH,
            status = TaskStatus.COMPLETED,
            completionDate = now - oneDayMillis + 3600000, // выполнено вчера
            eisenhowerQuadrant = 1,
            tags = listOf(tags[1], tags[3]) // личное, срочно
        ),
        Task(
            id = "task5",
            title = "Подготовить презентацию",
            description = "Презентация для встречи с клиентами",
            categoryId = "cat_work",
            creationDate = now - 4 * oneDayMillis,
            dueDate = now - 2 * oneDayMillis,
            priority = TaskPriority.HIGH,
            status = TaskStatus.COMPLETED,
            completionDate = now - 2 * oneDayMillis,
            eisenhowerQuadrant = 1,
            subtasks = listOf(
                Subtask(id = "sub5_1", taskId = "task5", title = "Исследование аудитории", isCompleted = true),
                Subtask(id = "sub5_2", taskId = "task5", title = "Создать слайды", isCompleted = true),
                Subtask(id = "sub5_3", taskId = "task5", title = "Добавить анимации", isCompleted = true)
            ),
            tags = listOf(tags[0], tags[4]) // работа, важно
        ),

        // Архивированные задачи
        Task(
            id = "task6",
            title = "Старый проект: документация",
            description = "Завершить документацию по проекту X",
            categoryId = "cat_work",
            creationDate = now - 30 * oneDayMillis,
            dueDate = now - 20 * oneDayMillis,
            priority = TaskPriority.MEDIUM,
            status = TaskStatus.ARCHIVED,
            completionDate = now - 20 * oneDayMillis,
            tags = listOf(tags[0]) // работа
        ),

        // Будущие задачи
        Task(
            id = "task7",
            title = "Стоматолог",
            description = "Плановый осмотр",
            categoryId = "cat_health",
            creationDate = now - oneDayMillis,
            dueDate = now + 5 * oneDayMillis, // через 5 дней
            dueTime = "10:30",
            duration = 60,
            priority = TaskPriority.HIGH,
            status = TaskStatus.ACTIVE,
            eisenhowerQuadrant = 2, // важно, но не срочно
            tags = listOf(tags[2], tags[1]) // здоровье, личное
        ),
        Task(
            id = "task8",
            title = "Подготовиться к экзамену",
            description = "Изучить материалы по курсу Android-разработки",
            categoryId = "cat_education",
            color = "#2196F3",
            creationDate = now - 15 * oneDayMillis,
            dueDate = now + 10 * oneDayMillis,
            priority = TaskPriority.HIGH,
            status = TaskStatus.ACTIVE,
            eisenhowerQuadrant = 2,
            estimatedPomodoroSessions = 10,
            subtasks = listOf(
                Subtask(id = "sub8_1", taskId = "task8", title = "Kotlin основы", isCompleted = true),
                Subtask(id = "sub8_2", taskId = "task8", title = "Android компоненты", isCompleted = false),
                Subtask(id = "sub8_3", taskId = "task8", title = "Jetpack Compose", isCompleted = false),
                Subtask(id = "sub8_4", taskId = "task8", title = "Material Design", isCompleted = false),
                Subtask(id = "sub8_5", taskId = "task8", title = "Архитектурные паттерны", isCompleted = false)
            ),
            tags = listOf(tags[6]) // учеба
        ),

        // Задачи без даты выполнения
        Task(
            id = "task9",
            title = "Прочитать книгу по продуктивности",
            categoryId = "cat_personal",
            creationDate = now - 10 * oneDayMillis,
            priority = TaskPriority.LOW,
            status = TaskStatus.ACTIVE,
            eisenhowerQuadrant = 4, // не важно и не срочно
            tags = listOf(tags[1], tags[6]) // личное, учеба
        ),

        // Срочные задачи сегодня
        Task(
            id = "task10",
            title = "Срочный звонок клиенту",
            description = "Обсудить изменения в проекте",
            categoryId = "cat_work",
            creationDate = now - 2 * 3600000, // создано 2 часа назад
            dueDate = now,
            dueTime = "17:30",
            priority = TaskPriority.HIGH,
            status = TaskStatus.ACTIVE,
            eisenhowerQuadrant = 1, // важно и срочно
            tags = listOf(tags[0], tags[3], tags[4]) // работа, срочно, важно
        ),

        // Задачи с повторением
        Task(
            id = "task11",
            title = "Еженедельная встреча команды",
            categoryId = "cat_work",
            creationDate = now - 20 * oneDayMillis,
            dueDate = now + 2 * oneDayMillis,
            dueTime = "10:00",
            duration = 60,
            priority = TaskPriority.MEDIUM,
            status = TaskStatus.ACTIVE,
            eisenhowerQuadrant = 2,
            tags = listOf(tags[0]), // работа
            recurrence = TaskRecurrence(
                id = "rec2",
                taskId = "task11",
                RecurrenceType.WEEKLY, // еженедельно
                daysOfWeek = listOf(2), // вторник
                startDate = now - 20 * oneDayMillis
            )
        ),

        // Низкоприоритетные задачи
        Task(
            id = "task12",
            title = "Разобрать старые фотографии",
            categoryId = "cat_home",
            creationDate = now - 25 * oneDayMillis,
            priority = TaskPriority.LOW,
            status = TaskStatus.ACTIVE,
            eisenhowerQuadrant = 4, // не важно и не срочно
            tags = listOf(tags[1], tags[5]) // личное, дом
        )
    )

    val datesWithTasks = tasks
        .mapNotNull { task -> task.dueDate?.let { LocalDate.ofEpochDay(it / (24 * 60 * 60 * 1000)) } }
        .toSet()
}

@Preview(name = "Task Item", showBackground = true)
@Composable
fun PreviewTaskItem() {
    DHbtTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            TaskItem(
                task = PreviewData.tasks[0],
                onTaskClick = {},
                onTaskStatusChange = { _, _ -> },
                onTaskDelete = {},
                onTaskArchive = {}
            )
        }
    }
}

@Preview(name = "Task Item Compact", showBackground = true)
@Composable
fun PreviewTaskItemCompact() {
    DHbtTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            TaskItemCompact(
                task = PreviewData.tasks[0],
                onTaskClick = {},
                onTaskStatusChange = { _, _ -> }
            )
        }
    }
}

@Preview(name = "Calendar", showBackground = true)
@Composable
fun PreviewCalendar() {
    DHbtTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            TaskCalendarSimple(
                isExpanded = true,
                onExpandToggle = {},
                selectedDate = LocalDate.now(),
                onDateSelected = {},
                datesWithTasks = PreviewData.datesWithTasks
            )
        }
    }
}

@Preview(name = "Categories Filter", showBackground = true)
@Composable
fun PreviewCategoriesFilter() {
    DHbtTheme {
        CategoryFilterRow(
            categories = PreviewData.categories,
            selectedCategoryId = "cat1",
            onCategorySelected = {}
        )
    }
}

@Preview(name = "Sort Options Menu", showBackground = true)
@Composable
fun PreviewSortMenu() {
    DHbtTheme {
        SortOptionsMenu(
            currentSortOption = SortOption.DATE_ASC,
            onSortOptionSelected = {}
        )
    }
}

@Preview(name = "EisenhowerMatrix", showBackground = true)
@Composable
fun PreviewEisenhowerMatrix() {
    DHbtTheme {
        EisenhowerMatrix(
            tasks = PreviewData.tasks.filter { it.eisenhowerQuadrant != null },
            onTaskClick = {},
            onTaskStatusChange = { _, _ -> },
            onTaskDelete = {}
        )
    }
}

@Preview(name = "Task Tag", showBackground = true)
@Composable
fun PreviewTaskTag() {
    DHbtTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            TaskTag(tag = PreviewData.tags[0])
        }
    }
}

@Preview(name = "Filter Dialog", showBackground = true)
@Composable
fun PreviewFilterDialog() {
    DHbtTheme {
        FilterDialog(
            onDismiss = {},
            currentStatus = TaskStatus.ACTIVE,
            onStatusSelected = {},
            currentPriority = TaskPriority.HIGH,
            onPrioritySelected = {},
            tags = PreviewData.tags,
            selectedTagIds = listOf("tag1"),
            onTagSelected = {},
            searchQuery = "запрос",
            onSearchQueryChanged = {}
        )
    }
}

// Здесь функция предпросмотра для полного экрана с моковыми данными
@Preview(name = "Tasks Screen", showBackground = true)
@Composable
fun PreviewTasksScreen() {
    DHbtTheme {
        TasksScreenPreview()
    }
}

// Имитация экрана задач без использования ViewModel
@Composable
fun TasksScreenPreview() {
    val tasks = PreviewData.tasks
    val categories = PreviewData.categories
    val tags = PreviewData.tags
    val datesWithTasks = PreviewData.datesWithTasks

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Топ бар
            TasksTopAppBar(
                onSearchClicked = {},
                onSortClicked = {},
                isFiltersActive = true,
                onResetFilters = {},
                onToggleEisenhowerMatrix = {},
                showEisenhowerMatrix = false
            )

            // Календарь
            TaskCalendarSimple(
                isExpanded = false,
                onExpandToggle = {},
                selectedDate = LocalDate.now(),
                onDateSelected = {},
                datesWithTasks = datesWithTasks
            )

            // Фильтр категорий
            CategoryFilterRow(
                categories = categories,
                selectedCategoryId = "cat1",
                onCategorySelected = {}
            )

            // Список задач
            TasksList(
                tasks = tasks,
                onTaskClick = {},
                onTaskStatusChange = { _, _ -> },
                onTaskDelete = {},
                onTaskArchive = {}
            )
        }
    }
}

@Preview(name = "Tasks Screen - Empty", showBackground = true)
@Composable
fun PreviewTasksScreenEmpty() {
    DHbtTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TasksTopAppBar(
                    onSearchClicked = {},
                    onSortClicked = {},
                    isFiltersActive = false,
                    onResetFilters = {},
                    onToggleEisenhowerMatrix = {},
                    showEisenhowerMatrix = false
                )

                TaskCalendarSimple(
                    isExpanded = false,
                    onExpandToggle = {},
                    selectedDate = null,
                    onDateSelected = {},
                    datesWithTasks = emptySet()
                )


            }
        }
    }
}

@Preview(name = "Tasks Screen - Eisenhower Matrix", showBackground = true)
@Composable
fun PreviewTasksScreenEisenhower() {
    DHbtTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TasksTopAppBar(
                    onSearchClicked = {},
                    onSortClicked = {},
                    isFiltersActive = false,
                    onResetFilters = {},
                    onToggleEisenhowerMatrix = {},
                    showEisenhowerMatrix = true
                )

                EisenhowerMatrix(
                    tasks = PreviewData.tasks.filter { it.eisenhowerQuadrant != null },
                    onTaskClick = {},
                    onTaskStatusChange = { _, _ -> },
                    onTaskDelete = {}
                )
            }
        }
    }
}