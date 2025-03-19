package com.example.dhbt.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.dhbt.domain.model.AppTheme
import com.example.dhbt.presentation.navigation.DHbtBottomNavigation
import com.example.dhbt.presentation.navigation.DHbtNavHost
import com.example.dhbt.presentation.navigation.Onboarding
import com.example.dhbt.presentation.theme.DHbtTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            DHbtApp()
        }
    }
}

@Composable
fun DHbtApp() {
    val mainViewModel: MainViewModel = hiltViewModel()
    val uiState by mainViewModel.state.collectAsState()

    LaunchedEffect(key1 = true) {
        mainViewModel.checkInitialState()
    }

    DHbtTheme(darkTheme = when (uiState.theme) {
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
        AppTheme.SYSTEM -> isSystemInDarkTheme()
    }) {
        if (uiState.isLoading) {
            SplashScreen()
        } else {
            val startDestination = if (uiState.hasCompletedOnboarding) {
                "Dashboard"
            } else {
                "Onboarding"
            }

            val navController = rememberNavController()

            Scaffold(
                bottomBar = {
                    // Отображаем нижнюю панель навигации только на основных экранах
                    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
                    val mainRoutes = listOf("Dashboard", "Tasks", "Habits", "Statistics", "More")

                    if (currentRoute in mainRoutes) {
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
}