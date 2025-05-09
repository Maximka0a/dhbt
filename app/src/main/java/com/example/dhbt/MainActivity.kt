package com.example.dhbt.presentation

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.dhbt.domain.model.AppTheme
import com.example.dhbt.domain.model.StartScreen
import com.example.dhbt.presentation.MainViewModel.MainEvent
import com.example.dhbt.presentation.navigation.DHbtBottomNavigation
import com.example.dhbt.presentation.navigation.DHbtNavHost
import com.example.dhbt.presentation.navigation.Dashboard
import com.example.dhbt.presentation.navigation.Habits
import com.example.dhbt.presentation.navigation.More
import com.example.dhbt.presentation.navigation.Onboarding
import com.example.dhbt.presentation.navigation.Statistics
import com.example.dhbt.presentation.navigation.Tasks
import com.example.dhbt.presentation.theme.DHbtTheme
import com.example.dhbt.util.LocaleHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

private const val TAG = "MainActivity"

/**
 * Главная активность приложения, управляющая экранами, темами и языковыми настройками.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private var currentLanguage: String = "ru"
    private var isRecreating = false

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.tag(TAG).d("onCreate: начало инициализации")

        // Включаем edge-to-edge режим для современного дизайна
        enableEdgeToEdge()

        // ВАЖНО: Настраиваем сплеш-скрин ДО вызова super.onCreate
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { viewModel.state.value.isLoading }

        super.onCreate(savedInstanceState)

        // Настройка полноэкранного режима и прозрачных системных полос
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)


        // Наблюдение за событиями от ViewModel
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.event.collectLatest { event ->
                    when (event) {
                        is MainEvent.ApplyLanguage -> {
                            Timber.tag(TAG).d("Получено событие ApplyLanguage: ${event.language}, текущий язык: $currentLanguage")

                            if (currentLanguage != event.language && !isRecreating) {
                                isRecreating = true
                                currentLanguage = event.language

                                // ВАЖНО: Сначала синхронно сохраняем язык, используя commit() вместо apply()
                                val sharedPrefs = getSharedPreferences("user_preferences", Context.MODE_PRIVATE)
                                sharedPrefs.edit().putString("language", event.language).commit()

                                // Затем обновляем локаль активити
                                updateLocale(event.language)

                                // Более длительная задержка для гарантированного сохранения настроек
                                Handler(Looper.getMainLooper()).postDelayed({
                                    Timber.tag(TAG).d("Пересоздание активности для применения языка")
                                    // Используем флаг, чтобы предотвратить циклическое пересоздание
                                    recreate()
                                }, 300)
                            }
                        }
                        is MainEvent.RefreshApp -> {
                            Timber.tag(TAG).d("Получено событие RefreshApp")

                            if (!isRecreating) {
                                isRecreating = true
                                recreate()
                            }
                        }
                        is MainEvent.ShowError -> {
                            Timber.tag(TAG).e("Получено событие ShowError: ${event.message}")
                            // Ошибки обрабатываются через Compose UI
                        }
                        is MainEvent.ShowMessage -> {
                            Timber.tag(TAG).d("Получено событие ShowMessage: ${event.message}")
                            // Сообщения обрабатываются через Compose UI
                        }
                        else -> { /* Игнорируем другие события */ }
                    }
                }
            }
        }

        // Устанавливаем главный Composable UI
        setContent {
            DHbtApp(viewModel = viewModel)
        }

        Timber.tag(TAG).d("onCreate: инициализация завершена")
    }


    override fun onResume() {
        super.onResume()
        isRecreating = false
        Timber.tag(TAG).d("onResume")
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.tag(TAG).d("onDestroy")
    }

    /**
     * Обновляет локаль приложения.
     *
     * @param languageCode Код языка (например, "ru", "en")
     */
    private fun updateLocale(languageCode: String) {
        LocaleHelper.setLocale(this, languageCode)
        Timber.tag(TAG).d("Локаль обновлена на: $languageCode")
    }

    /**
     * Переопределяет базовый контекст для применения языковых настроек.
     */
    override fun attachBaseContext(newBase: Context) {
        // Получаем сохраненный язык из SharedPreferences
        val sharedPrefs = newBase.getSharedPreferences("user_preferences", Context.MODE_PRIVATE)
        val languageCode = sharedPrefs.getString("language", "ru") ?: "ru"
        currentLanguage = languageCode

        Timber.tag(TAG).d("attachBaseContext: установка локали $languageCode")

        // Применяем локаль к контексту
        val context = LocaleHelper.setLocale(newBase, languageCode)
        super.attachBaseContext(context)
    }
}

/**
 * Главный Composable для приложения, содержащий все экраны и навигацию.
 */
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter", "LocalContextConfigurationRead")
@Composable
fun DHbtApp(
    viewModel: MainViewModel = hiltViewModel()
) {
    val uiState by viewModel.state.collectAsState()
    val context = LocalContext.current
    val languageChangeHandled = remember { mutableStateOf(false) }
    val currentLocaleLanguage = remember { mutableStateOf(context.resources.configuration.locales[0].language) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

// Логируем текущий язык локали при каждой рекомпозиции
    LaunchedEffect(Unit) {
        Timber.tag("DHbtApp").d(
            "Текущий язык локали: ${context.resources.configuration.locales[0].language}, " +
                    "язык в состоянии: ${uiState.language}"
        )
    }

// Применяем изменения языка
    LaunchedEffect(uiState.language) {
        // Проверяем, действительно ли язык в UI состоянии не соответствует текущему языку локали
        val localeLanguage = context.resources.configuration.locales[0].language
        val stateLanguage = uiState.language

        Timber.tag("DHbtApp").d("LaunchedEffect: locale=$localeLanguage, state=$stateLanguage, handled=${languageChangeHandled.value}")

        // Отправляем событие ТОЛЬКО если языки действительно отличаются и изменение еще не обрабатывалось
        if (localeLanguage != stateLanguage && !languageChangeHandled.value) {
            Timber.tag(TAG).d("Применение языка: $stateLanguage (текущий локальный: $localeLanguage)")
            languageChangeHandled.value = true
            viewModel.sendEvent(MainEvent.ApplyLanguage(stateLanguage))
        }
    }
    // Наблюдаем за жизненным циклом
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    Timber.tag(TAG).d("Lifecycle: ON_START")
                }
                Lifecycle.Event.ON_STOP -> {
                    Timber.tag(TAG).d("Lifecycle: ON_STOP")
                }
                else -> { /* Игнорируем другие события */ }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Применяем изменения языка
    LaunchedEffect(uiState.language) {
        if (!languageChangeHandled.value) {
            Timber.tag(TAG).d("Применение языка: ${uiState.language}")
            viewModel.sendEvent(MainEvent.ApplyLanguage(uiState.language))
            languageChangeHandled.value = true
        }
    }

    // Сброс флага при изменении языка
    if (languageChangeHandled.value && uiState.language != context.resources.configuration.locales[0].language) {
        languageChangeHandled.value = false
    }
    // Применяем тему в соответствии с настройками
    DHbtTheme(darkTheme = when (uiState.theme) {
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
        AppTheme.SYSTEM -> isSystemInDarkTheme()
    }) {
        // Если идет загрузка данных, показываем пустой экран (сплеш-скрин будет поверх)
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            )
            return@DHbtTheme
        }

        // Определяем стартовый экран
        val startDestination = if (!uiState.hasCompletedOnboarding) {
            Timber.tag(TAG).d("Показываем экран онбординга")
            Onboarding
        } else {
            Timber.tag(TAG).d("Показываем основной экран: ${uiState.startScreen}")
            when (uiState.startScreen) {
                StartScreen.DASHBOARD -> Dashboard
                StartScreen.TASKS -> Tasks
                StartScreen.HABITS -> Habits
                else -> Dashboard
            }
        }

        val navController = rememberNavController()
        val currentBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = currentBackStackEntry?.destination?.route

        // Маршруты, на которых показываем нижнюю навигацию
        val mainRoutes = listOf(
            Dashboard::class.qualifiedName,
            Tasks::class.qualifiedName,
            Habits::class.qualifiedName,
            Statistics::class.qualifiedName,
            More::class.qualifiedName
        )

        val showBottomNav = currentRoute in mainRoutes
        val bottomNavHeight = 80.dp

        // Логируем изменение маршрутов
        DisposableEffect(currentRoute) {
            Timber.tag(TAG).d("Текущий маршрут: $currentRoute")
            onDispose { }
        }

        // Scaffold с надежным управлением Snackbar
        Scaffold(
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.padding(bottom = if (showBottomNav) bottomNavHeight else 0.dp)
                )
            }
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Основной контент с анимацией
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding()
                        .then(
                            if (showBottomNav) {
                                Modifier.padding(bottom = bottomNavHeight)
                            } else {
                                Modifier
                            }
                        )
                ) {
                    DHbtNavHost(
                        navController = navController,
                        startDestination = startDestination,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Нижняя панель навигации с плавной анимацией
                AnimatedVisibility(
                    visible = showBottomNav,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(300)
                    ) + fadeIn(animationSpec = tween(300)),
                    exit = slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(300)
                    ) + fadeOut(animationSpec = tween(300)),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 4.dp,
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                    ) {
                        DHbtBottomNavigation(navController)
                    }
                }
            }
        }
    }
}