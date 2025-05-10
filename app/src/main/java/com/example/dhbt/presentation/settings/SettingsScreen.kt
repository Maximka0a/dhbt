package com.example.dhbt.presentation.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.dhbt.domain.model.AppTheme
import com.example.dhbt.domain.model.Category
import com.example.dhbt.domain.model.CategoryType
import com.example.dhbt.domain.model.StartScreen
import com.example.dhbt.domain.model.UserData
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val userData by viewModel.userData.collectAsState()
    val userPreferences by viewModel.userPreferences.collectAsState()
    val pomodoroPreferences by viewModel.pomodoroPreferences.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val showDeleteDialog by viewModel.showDeleteDialog.collectAsState()
    val showLanguageDialog by viewModel.showLanguageDialog.collectAsState()
    val showCategoryDialog by viewModel.showCategoryDialog.collectAsState()
    val showThemeDialog by viewModel.showThemeDialog.collectAsState()
    val showQuietHoursDialog by viewModel.showQuietHoursDialog.collectAsState()
    val editCategory by viewModel.editCategory.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Обработка сообщений об успехе и ошибках
    LaunchedEffect(successMessage, errorMessage) {
        successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onAction(SettingsAction.DismissMessage)
        }
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onAction(SettingsAction.DismissError)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                ),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    actionColor = MaterialTheme.colorScheme.primary
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Основное содержимое
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp)
            ) {
                // Профиль пользователя
                ProfileSection(
                    userData = userData,
                    onUpdateName = { viewModel.onAction(SettingsAction.UpdateUserName(it)) },
                    onUpdateEmail = { viewModel.onAction(SettingsAction.UpdateUserEmail(it)) }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Секция интерфейса
                SettingsSection(title = "Внешний вид и интерфейс") {
                    // Тема
                    SettingsItem(
                        icon = Icons.Default.DarkMode,
                        title = "Тема оформления",
                        subtitle = when(userPreferences.theme) {
                            AppTheme.LIGHT -> "Светлая"
                            AppTheme.DARK -> "Темная"
                            AppTheme.SYSTEM -> "Системная"
                        },
                        onClick = { viewModel.onAction(SettingsAction.ToggleThemeDialog(true)) }
                    )

                    // Язык
                    SettingsItem(
                        icon = Icons.Default.Language,
                        title = "Язык приложения",
                        subtitle = viewModel.availableLanguages.find { it.code == userPreferences.language }?.displayName ?: "Русский",
                        onClick = { viewModel.onAction(SettingsAction.ToggleLanguageDialog(true)) }
                    )

                    // Стартовый экран
                    val startScreenOptions = listOf(
                        "Дашборд" to StartScreen.DASHBOARD.value,
                        "Задачи" to StartScreen.TASKS.value,
                        "Привычки" to StartScreen.HABITS.value
                    )
                    DropdownSettingItem(
                        icon = Icons.Default.Home,
                        title = "Стартовый экран",
                        options = startScreenOptions,
                        selectedValue = userPreferences.startScreenType.value,
                        onSelectionChanged = { viewModel.onAction(SettingsAction.UpdateStartScreen(it)) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Секция уведомлений
                SettingsSection(title = "Уведомления и напоминания") {
                    // Звук
                    SwitchSettingItem(
                        icon = Icons.Default.VolumeUp,
                        title = "Звук уведомлений",
                        subtitle = if (userPreferences.defaultSoundEnabled) "Включен" else "Отключен",
                        isChecked = userPreferences.defaultSoundEnabled,
                        onCheckedChange = {
                            viewModel.onAction(SettingsAction.UpdateSoundEnabled(it))
                        }
                    )

                    // Вибрация
                    SwitchSettingItem(
                        icon = Icons.Default.Vibration,
                        title = "Вибрация",
                        subtitle = if (userPreferences.defaultVibrationEnabled) "Включена" else "Отключена",
                        isChecked = userPreferences.defaultVibrationEnabled,
                        onCheckedChange = {
                            viewModel.onAction(SettingsAction.UpdateVibrationEnabled(it))
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Секция задач и привычек
                SettingsSection(title = "Задачи и привычки") {
                    // Представление задач по умолчанию
                    val taskViewOptions = listOf(
                        "Список" to 0,
                        "Канбан" to 1,
                        "Эйзенхауэр" to 2
                    )
                    DropdownSettingItem(
                        icon = Icons.Default.FormatListBulleted,
                        title = "Вид задач по умолчанию",
                        options = taskViewOptions,
                        selectedValue = userPreferences.defaultTaskView.value,
                        onSelectionChanged = {
                            viewModel.onAction(SettingsAction.UpdateDefaultTaskView(it))
                        }
                    )

                    // Сортировка задач
                    val taskSortOptions = listOf(
                        "По дате" to 0,
                        "По приоритету" to 1,
                        "По алфавиту" to 2
                    )
                    DropdownSettingItem(
                        icon = Icons.Default.Sort,
                        title = "Сортировка задач",
                        options = taskSortOptions,
                        selectedValue = userPreferences.defaultTaskSort.value,
                        onSelectionChanged = {
                            viewModel.onAction(SettingsAction.UpdateDefaultTaskSort(it))
                        }
                    )

                    // Сортировка привычек
                    val habitSortOptions = listOf(
                        "По алфавиту" to 0,
                        "По серии" to 1
                    )
                    DropdownSettingItem(
                        icon = Icons.Default.SwapVert,
                        title = "Сортировка привычек",
                        options = habitSortOptions,
                        selectedValue = userPreferences.defaultHabitSort.value,
                        onSelectionChanged = {
                            viewModel.onAction(SettingsAction.UpdateDefaultHabitSort(it))
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Настройки Pomodoro
                SettingsSection(title = "Настройки Pomodoro") {
                    // Длительность рабочей сессии
                    SliderSettingItem(
                        icon = Icons.Default.Timer,
                        title = "Длительность рабочей сессии",
                        subtitle = "${pomodoroPreferences.workDuration} минут",
                        value = pomodoroPreferences.workDuration.toFloat(),
                        valueRange = 10f..60f,
                        steps = 10,
                        onValueChange = {
                            val updatedSettings = pomodoroPreferences.copy(workDuration = it.toInt())
                            viewModel.onAction(SettingsAction.UpdatePomodoroSettings(updatedSettings))
                        }
                    )

                    // Короткий перерыв
                    SliderSettingItem(
                        icon = Icons.Default.Coffee,
                        title = "Короткий перерыв",
                        subtitle = "${pomodoroPreferences.shortBreakDuration} минут",
                        value = pomodoroPreferences.shortBreakDuration.toFloat(),
                        valueRange = 1f..15f,
                        steps = 14,
                        onValueChange = {
                            val updatedSettings = pomodoroPreferences.copy(shortBreakDuration = it.toInt())
                            viewModel.onAction(SettingsAction.UpdatePomodoroSettings(updatedSettings))
                        }
                    )

                    // Длинный перерыв
                    SliderSettingItem(
                        icon = Icons.Default.HourglassTop,
                        title = "Длинный перерыв",
                        subtitle = "${pomodoroPreferences.longBreakDuration} минут",
                        value = pomodoroPreferences.longBreakDuration.toFloat(),
                        valueRange = 5f..30f,
                        steps = 5,
                        onValueChange = {
                            val updatedSettings = pomodoroPreferences.copy(longBreakDuration = it.toInt())
                            viewModel.onAction(SettingsAction.UpdatePomodoroSettings(updatedSettings))
                        }
                    )

                    // Циклы до длинного перерыва
                    SliderSettingItem(
                        icon = Icons.Default.Repeat,
                        title = "Циклов до длинного перерыва",
                        subtitle = "${pomodoroPreferences.pomodorosUntilLongBreak} циклов",
                        value = pomodoroPreferences.pomodorosUntilLongBreak.toFloat(),
                        valueRange = 2f..6f,
                        steps = 4,
                        onValueChange = {
                            val updatedSettings = pomodoroPreferences.copy(pomodorosUntilLongBreak = it.toInt())
                            viewModel.onAction(SettingsAction.UpdatePomodoroSettings(updatedSettings))
                        }
                    )

                    // Авто-старт перерывов
                    SwitchSettingItem(
                        icon = Icons.Default.PlayArrow,
                        title = "Авто-старт перерывов",
                        subtitle = if (pomodoroPreferences.autoStartBreaks) "Включено" else "Отключено",
                        isChecked = pomodoroPreferences.autoStartBreaks,
                        onCheckedChange = {
                            val updatedSettings = pomodoroPreferences.copy(autoStartBreaks = it)
                            viewModel.onAction(SettingsAction.UpdatePomodoroSettings(updatedSettings))
                        }
                    )

                    // Экран всегда включен
                    SwitchSettingItem(
                        icon = Icons.Default.Visibility,
                        title = "Не выключать экран",
                        subtitle = if (pomodoroPreferences.keepScreenOn) "Включено" else "Отключено",
                        isChecked = pomodoroPreferences.keepScreenOn,
                        onCheckedChange = {
                            val updatedSettings = pomodoroPreferences.copy(keepScreenOn = it)
                            viewModel.onAction(SettingsAction.UpdatePomodoroSettings(updatedSettings))
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Категории
                SettingsSection(
                    title = "Управление категориями",
                    actionButton = {
                        IconButton(onClick = {
                            viewModel.onAction(SettingsAction.ClearEditCategory)
                            viewModel.onAction(SettingsAction.ToggleCategoryDialog(true))
                        }) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Добавить категорию",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                ) {
                    if (categories.isEmpty()) {
                        Text(
                            "У вас пока нет категорий. Создайте их, чтобы удобно организовать задачи и привычки.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        categories.forEach { category ->
                            CategoryItem(
                                category = category,
                                onEditClick = {
                                    viewModel.onAction(SettingsAction.StartEditCategory(category))
                                },
                                onDeleteClick = {
                                    viewModel.onAction(SettingsAction.DeleteCategory(category.id))
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // О приложении и помощь
                SettingsSection(title = "О приложении") {
                    // Оценить приложение
                    SettingsItem(
                        icon = Icons.Default.Star,
                        title = "Оценить приложение",
                        subtitle = "Поддержите нас оценкой в Google Play",
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse("market://details?id=${context.packageName}")
                                setPackage("com.android.vending")
                            }
                            if (intent.resolveActivity(context.packageManager) != null) {
                                context.startActivity(intent)
                            } else {
                                context.startActivity(Intent(Intent.ACTION_VIEW,
                                    Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")))
                            }
                        }
                    )

                    // Поделиться с друзьями
                    SettingsItem(
                        icon = Icons.Default.Share,
                        title = "Поделиться с друзьями",
                        subtitle = "Рассказать друзьям о DHbt",
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "DHbt - приложение для управления задачами и привычками")
                                putExtra(Intent.EXTRA_TEXT,
                                    "Попробуйте DHbt - отличное приложение для управления задачами и развития полезных привычек! " +
                                            "https://play.google.com/store/apps/details?id=${context.packageName}")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Поделиться через"))
                        }
                    )

                    // Политика конфиденциальности
                    SettingsItem(
                        icon = Icons.Default.PrivacyTip,
                        title = "Политика конфиденциальности",
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW,
                                Uri.parse("https://example.com/privacy-policy"))
                            context.startActivity(intent)
                        }
                    )

                    // Справка и поддержка
                    SettingsItem(
                        icon = Icons.Default.Help,
                        title = "Справка и поддержка",
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW,
                                Uri.parse("https://example.com/help"))
                            context.startActivity(intent)
                        }
                    )

                    // О приложении
                    SettingsItem(
                        icon = Icons.Default.Info,
                        title = "О приложении",
                        subtitle = "Версия 1.0.0",
                        onClick = { /* TODO: Подробная информация о приложении */ }
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Диалоги
    if (showDeleteDialog) {
        ConfirmationDialog(
            title = "Сбросить все данные?",
            text = "Это действие удалит все ваши задачи, привычки и настройки. Данные нельзя будет восстановить.",
            confirmButtonText = "Сбросить",
            dismissButtonText = "Отмена",
            onConfirmClick = { viewModel.onAction(SettingsAction.ResetAllData) }, // Убрали круглые скобки
            onDismissClick = { viewModel.onAction(SettingsAction.ToggleDeleteDialog(false)) }
        )
    }

    if (showLanguageDialog) {
        LanguageSelectionDialog(
            options = viewModel.availableLanguages,
            selectedLanguage = userPreferences.language,
            onLanguageSelected = { viewModel.onAction(SettingsAction.UpdateLanguage(it)) },
            onDismissRequest = { viewModel.onAction(SettingsAction.ToggleLanguageDialog(false)) }
        )
    }

    if (showCategoryDialog) {
        val currentCategory = editCategory // currentCategory уже имеет тип Category?

        CategoryDialog(
            category = currentCategory,
            onSaveCategory = { name, color, emoji, type ->
                val categoryToSave = if (currentCategory != null) {
                    currentCategory.copy(name = name, color = color, iconEmoji = emoji, type = type)
                } else {
                    Category(
                        id = UUID.randomUUID().toString(),
                        name = name,
                        color = color,
                        iconEmoji = emoji,
                        type = type,
                        order = categories.size
                    )
                }

                if (currentCategory != null) {
                    viewModel.onAction(SettingsAction.UpdateCategory(categoryToSave))
                } else {
                    viewModel.onAction(SettingsAction.AddCategory(categoryToSave))
                }
            },
            onDismissRequest = { viewModel.onAction(SettingsAction.ToggleCategoryDialog(false)) }
        )
    }

    if (showThemeDialog) {
        ThemeSelectionDialog(
            selectedTheme = userPreferences.theme.value,
            onThemeSelected = { viewModel.onAction(SettingsAction.UpdateTheme(it)) },
            onDismissRequest = { viewModel.onAction(SettingsAction.ToggleThemeDialog(false)) }
        )
    }

    if (showQuietHoursDialog) {
        QuietHoursDialog(
            enabled = userPreferences.quietHoursEnabled,
            startTime = userPreferences.quietHoursStart ?: "22:00",
            endTime = userPreferences.quietHoursEnd ?: "08:00",
            onSaveQuietHours = { enabled, start, end ->
                viewModel.onAction(SettingsAction.UpdateQuietHours(enabled, start, end))
                viewModel.onAction(SettingsAction.ToggleQuietHoursDialog(false))
            },
            onDismissRequest = { viewModel.onAction(SettingsAction.ToggleQuietHoursDialog(false)) }
        )
    }
}

@Composable
fun ProfileSection(
    userData: UserData,
    onUpdateName: (String) -> Unit,
    onUpdateEmail: (String) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf(userData.name) }
    var email by remember { mutableStateOf(userData.email ?: "") }

    LaunchedEffect(userData) {
        name = userData.name
        email = userData.email ?: ""
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (userData.avatarUrl != null) {
                    // Здесь должна быть загрузка изображения, например с Coil
                    // AsyncImage или другой компонент для загрузки изображений
                    Text(
                        text = userData.name.take(1).uppercase(),
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                } else {
                    Text(
                        text = userData.name.takeIf { it.isNotEmpty() }?.take(1)?.uppercase() ?: "U",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isEditing) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Имя") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = { isEditing = false }
                    ) {
                        Text("Отмена")
                    }

                    Button(
                        onClick = {
                            onUpdateName(name)
                            onUpdateEmail(email)
                            isEditing = false
                        }
                    ) {
                        Text("Сохранить")
                    }
                }
            } else {
                Text(
                    text = userData.name.takeIf { it.isNotEmpty() } ?: "Пользователь",
                    style = MaterialTheme.typography.titleLarge
                )

                userData.email?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = { isEditing = true }
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Редактировать профиль",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Редактировать профиль")
                }
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    actionButton: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            actionButton?.invoke()
        }

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                content = content
            )
        }
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    titleColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Surface(
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = titleColor
                )

                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun SwitchSettingItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.clickable { onCheckedChange(!isChecked) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )

                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Switch(
                checked = isChecked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownSettingItem(
    icon: ImageVector,
    title: String,
    options: List<Pair<String, Int>>,
    selectedValue: Int,
    onSelectionChanged: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedOption = options.find { it.second == selectedValue }?.first ?: options.first().first

    Surface(
        modifier = Modifier.clickable { expanded = true }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )

                Text(
                    text = selectedOption,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Выбрать",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.first) },
                            onClick = {
                                onSelectionChanged(option.second)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SliderSettingItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit
) {
    var sliderValue by remember { mutableStateOf(value) }
    var isEditing by remember { mutableStateOf(false) }

    LaunchedEffect(value) {
        sliderValue = value
    }

    Surface(
        modifier = Modifier.clickable { isEditing = !isEditing }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = { isEditing = !isEditing }) {
                    Icon(
                        imageVector = if (isEditing) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isEditing) "Скрыть" else "Показать",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AnimatedVisibility(
                visible = isEditing,
                enter = slideInVertically(
                    initialOffsetY = { -it },
                    animationSpec = tween(durationMillis = 200)
                ) + fadeIn(animationSpec = tween(durationMillis = 200)),
                exit = fadeOut(animationSpec = tween(durationMillis = 200))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Slider(
                        value = sliderValue,
                        onValueChange = { sliderValue = it },
                        onValueChangeFinished = { onValueChange(sliderValue) },
                        valueRange = valueRange,
                        steps = steps,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = valueRange.start.toInt().toString(),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = valueRange.endInclusive.toInt().toString(),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryItem(
    category: Category,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Emoji или цветная точка
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color(android.graphics.Color.parseColor(category.color ?: "#6200EE"))),
            contentAlignment = Alignment.Center
        ) {
            if (category.iconEmoji != null) {
                Text(
                    text = category.iconEmoji,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = category.name,
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                text = when (category.type) {
                    CategoryType.TASK -> "Только задачи"
                    CategoryType.HABIT -> "Только привычки"
                    CategoryType.BOTH -> "Задачи и привычки"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Действия",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Редактировать") },
                    onClick = {
                        onEditClick()
                        showMenu = false
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null
                        )
                    }
                )
                DropdownMenuItem(
                    text = { Text("Удалить") },
                    onClick = {
                        onDeleteClick()
                        showMenu = false
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun ConfirmationDialog(
    title: String,
    text: String,
    confirmButtonText: String,
    dismissButtonText: String,
    onConfirmClick: () -> Unit,
    onDismissClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissClick,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(
                onClick = onConfirmClick,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(confirmButtonText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissClick) {
                Text(dismissButtonText)
            }
        }
    )
}

@Composable
fun LanguageSelectionDialog(
    options: List<LanguageOption>,
    selectedLanguage: String,
    onLanguageSelected: (String) -> Unit,
    onDismissRequest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Выберите язык") },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onLanguageSelected(option.code)
                                onDismissRequest()
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedLanguage == option.code,
                            onClick = {
                                onLanguageSelected(option.code)
                                onDismissRequest()
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(option.displayName)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Отмена")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDialog(
    category: Category?,
    onSaveCategory: (name: String, color: String, emoji: String?, type: CategoryType) -> Unit,
    onDismissRequest: () -> Unit
) {
    val isEditing = category != null
    var name by remember { mutableStateOf(category?.name ?: "") }
    var color by remember { mutableStateOf(category?.color ?: "#6200EE") }
    var emoji by remember { mutableStateOf(category?.iconEmoji) }
    var type by remember { mutableStateOf(category?.type ?: CategoryType.BOTH) }

    // Предопределенные цвета для выбора
    val colors = listOf(
        "#6200EE", "#03DAC5", "#018786", "#B00020", "#3700B3",
        "#03A9F4", "#009688", "#4CAF50", "#CDDC39", "#FF5722"
    )

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(if (isEditing) "Редактирование категории" else "Новая категория") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название категории") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Цвет",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Выбор цвета
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(colors) { colorHex ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(colorHex)))
                                .border(
                                    width = 2.dp,
                                    color = if (color == colorHex) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { color = colorHex }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Эмодзи (простое текстовое поле для демонстрации)
                OutlinedTextField(
                    value = emoji ?: "",
                    onValueChange = {
                        if (it.isEmpty()) emoji = null
                        else emoji = it.take(2) // Простое ограничение для демо
                    },
                    label = { Text("Эмодзи (опционально)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Тип категории
                Text(
                    "Тип категории",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = type == CategoryType.TASK,
                        onClick = { type = CategoryType.TASK }
                    )
                    Text(
                        "Только задачи",
                        modifier = Modifier
                            .clickable { type = CategoryType.TASK }
                            .padding(start = 4.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = type == CategoryType.HABIT,
                        onClick = { type = CategoryType.HABIT }
                    )
                    Text(
                        "Только привычки",
                        modifier = Modifier
                            .clickable { type = CategoryType.HABIT }
                            .padding(start = 4.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = type == CategoryType.BOTH,
                        onClick = { type = CategoryType.BOTH }
                    )
                    Text(
                        "Задачи и привычки",
                        modifier = Modifier
                            .clickable { type = CategoryType.BOTH }
                            .padding(start = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSaveCategory(name, color, emoji, type)
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text(if (isEditing) "Обновить" else "Добавить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Отмена")
            }
        }
    )
}

@Composable
fun ThemeSelectionDialog(
    selectedTheme: Int,
    onThemeSelected: (Int) -> Unit,
    onDismissRequest: () -> Unit
) {
    val themes = listOf(
        AppTheme.LIGHT.value to "Светлая",
        AppTheme.DARK.value to "Темная",
        AppTheme.SYSTEM.value to "Системная"
    )

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Выберите тему") },
        text = {
            Column {
                themes.forEach { (value, name) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onThemeSelected(value)
                                onDismissRequest()
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedTheme == value,
                            onClick = {
                                onThemeSelected(value)
                                onDismissRequest()
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(name)

                        if (value == AppTheme.LIGHT.value) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.LightMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        } else if (value == AppTheme.DARK.value) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.DarkMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Отмена")
            }
        }
    )
}
@Composable
fun QuietHoursDialog(
    enabled: Boolean,
    startTime: String,
    endTime: String,
    onSaveQuietHours: (Boolean, String?, String?) -> Unit,
    onDismissRequest: () -> Unit
) {
    var isEnabled by remember { mutableStateOf(enabled) }
    var start by remember { mutableStateOf(startTime) }
    var end by remember { mutableStateOf(endTime) }

    // Добавим простую валидацию формата времени
    var startError by remember { mutableStateOf(false) }
    var endError by remember { mutableStateOf(false) }

    fun validateTime(time: String): Boolean {
        val pattern = Regex("^([01]?[0-9]|2[0-3]):([0-5][0-9])$")
        return pattern.matches(time)
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Тихие часы") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Включить тихие часы",
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { isEnabled = it }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = start,
                    onValueChange = {
                        start = it
                        startError = !validateTime(it)
                    },
                    label = { Text("Время начала (ЧЧ:ММ)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = startError,
                    supportingText = {
                        if (startError) {
                            Text("Неверный формат. Используйте ЧЧ:ММ")
                        }
                    },
                    enabled = isEnabled
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = end,
                    onValueChange = {
                        end = it
                        endError = !validateTime(it)
                    },
                    label = { Text("Время окончания (ЧЧ:ММ)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = endError,
                    supportingText = {
                        if (endError) {
                            Text("Неверный формат. Используйте ЧЧ:ММ")
                        }
                    },
                    enabled = isEnabled
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "В этот период времени не будут отправляться уведомления.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isEnabled && (!startError && !endError)) {
                        onSaveQuietHours(isEnabled, start, end)
                    } else if (!isEnabled) {
                        onSaveQuietHours(false, null, null)
                    }
                },
                enabled = !isEnabled || (!startError && !endError)
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Отмена")
            }
        }
    )
}