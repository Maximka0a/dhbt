package com.example.dhbt.presentation

import android.os.Bundle
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.dhbt.domain.model.AppTheme
import com.example.dhbt.presentation.navigation.DHbtBottomNavigation
import com.example.dhbt.presentation.navigation.DHbtNavHost
import com.example.dhbt.presentation.navigation.Dashboard
import com.example.dhbt.presentation.navigation.Habits
import com.example.dhbt.presentation.navigation.More
import com.example.dhbt.presentation.navigation.Onboarding
import com.example.dhbt.presentation.navigation.Statistics
import com.example.dhbt.presentation.navigation.Tasks
import com.example.dhbt.presentation.theme.DHbtTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        installSplashScreen().apply {
            setKeepOnScreenCondition { viewModel.state.value.isLoading }
        }
        setContent {
            DHbtApp()
        }
    }
}

@Composable
fun DHbtApp() {
    val mainViewModel: MainViewModel = hiltViewModel()
    val uiState by mainViewModel.state.collectAsState()

    DHbtTheme(darkTheme = when (uiState.theme) {
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
        AppTheme.SYSTEM -> isSystemInDarkTheme()
    }) {
        val startDestination = if (uiState.hasCompletedOnboarding) {
            Dashboard
        } else {
            Onboarding
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
                    mainViewModel = mainViewModel
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