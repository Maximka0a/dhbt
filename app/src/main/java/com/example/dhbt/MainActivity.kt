package com.example.dhbt.presentation

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
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
import com.example.dhbt.utils.NotificationDebugHelper
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private var currentLanguage: String = "ru"
    // Add this flag to prevent multiple recreations
    private var isRecreating = false

    @Inject
    lateinit var notificationDebugHelper: NotificationDebugHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        installSplashScreen().apply {
            setKeepOnScreenCondition { viewModel.state.value.isLoading }
        }
        lifecycleScope.launch {
            notificationDebugHelper.testNotificationRepository()
        }
        // Observe UI events from the ViewModel
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.event.collectLatest { event ->
                    when (event) {
                        is MainEvent.ApplyLanguage -> {
                            if (currentLanguage != event.language && !isRecreating) {
                                isRecreating = true // Prevent multiple recreations
                                currentLanguage = event.language
                                updateLocale(event.language)

                                // Use a handler with a slight delay to ensure smooth transition
                                Handler(Looper.getMainLooper()).postDelayed({
                                    recreate()
                                }, 100)
                            }
                        }
                        is MainEvent.RefreshApp -> {
                            if (!isRecreating) {
                                isRecreating = true
                                recreate()
                            }
                        }
                        else -> {
                            // Handle other events
                        }
                    }
                }
            }
        }

        setContent {
            DHbtApp(viewModel = viewModel)
        }
    }

    override fun onResume() {
        super.onResume()
        // Reset the recreation flag when activity is fully resumed
        isRecreating = false
    }

    private fun updateLocale(languageCode: String) {
        LocaleHelper.setLocale(this, languageCode)
    }

    override fun attachBaseContext(newBase: Context) {
        // Get the language code from somewhere - here using preferences directly
        val sharedPrefs = newBase.getSharedPreferences("user_preferences", Context.MODE_PRIVATE)
        val languageCode = sharedPrefs.getString("language", "ru") ?: "ru"
        currentLanguage = languageCode

        val context = LocaleHelper.setLocale(newBase, languageCode)
        super.attachBaseContext(context)
    }
}

@Composable
fun DHbtApp(viewModel: MainViewModel = hiltViewModel()) {
    val uiState by viewModel.state.collectAsState()
    val context = LocalContext.current
    // Track if we've handled the language change
    val languageChangeHandled = remember { mutableStateOf(false) }

    // Apply language changes only once when app starts or changes
    LaunchedEffect(uiState.language) {
        if (!languageChangeHandled.value) {
            viewModel.sendEvent(MainEvent.ApplyLanguage(uiState.language))
            languageChangeHandled.value = true
        }
    }

    // Reset the flag if language actually changes
    if (languageChangeHandled.value && uiState.language != context.resources.configuration.locales[0].language) {
        languageChangeHandled.value = false
    }

    DHbtTheme(darkTheme = when (uiState.theme) {
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
        AppTheme.SYSTEM -> isSystemInDarkTheme()
    }) {
        val startDestination = if (!uiState.hasCompletedOnboarding) {
            Onboarding
        } else {
            // Use the selected start screen from preferences
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

        val mainRoutes = listOf(
            Dashboard::class.qualifiedName,
            Tasks::class.qualifiedName,
            Habits::class.qualifiedName,
            Statistics::class.qualifiedName,
            More::class.qualifiedName
        )

        val showBottomNav = currentRoute in mainRoutes

        // Вычисляем нужную высоту для отступа
        val bottomNavHeight = 80.dp

        Box(modifier = Modifier.fillMaxSize()) {
            // Основной контент
            Box(
                modifier = Modifier
                    // Используем imePadding для правильной работы с клавиатурой
                    .imePadding()
                    // Условный отступ только когда нужно
                    .then(
                        if (showBottomNav) {
                            Modifier.padding(bottom = bottomNavHeight)
                        } else {
                            Modifier
                        }
                    )
                    .fillMaxSize()
            ) {
                DHbtNavHost(
                    navController = navController,
                    startDestination = startDestination,
                    modifier = Modifier.fillMaxSize(),
                    mainViewModel = viewModel
                )
            }

            // Нижняя навигация
            AnimatedVisibility(
                visible = showBottomNav,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding() // Учитываем системную навигацию
                ) {
                    DHbtBottomNavigation(navController)
                }
            }
        }
    }
}