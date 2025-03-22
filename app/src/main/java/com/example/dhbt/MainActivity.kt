package com.example.dhbt.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
import com.example.dhbt.presentation.task.edit.EditTaskScreen
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
            EditTaskScreen(
                navController = rememberNavController()
            )
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
        // Выбираем начальный экран на основе состояния
        val startDestination = if (uiState.hasCompletedOnboarding) {
            Dashboard
        } else {
            Onboarding
        }

        val navController = rememberNavController()

        Scaffold(
            bottomBar = {
                val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

                // Список экранов, на которых отображается нижняя панель навигации
                val mainRoutes = listOf(
                    Dashboard::class.qualifiedName,
                    Tasks::class.qualifiedName,
                    Habits::class.qualifiedName,
                    Statistics::class.qualifiedName,
                    More::class.qualifiedName
                )

                val showBottomNav = currentRoute in mainRoutes

                if (showBottomNav) {
                    DHbtBottomNavigation(navController)
                }
            }
        ) { paddingValues ->
            DHbtNavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier.padding(paddingValues),
                mainViewModel = mainViewModel
            )
        }
    }
}