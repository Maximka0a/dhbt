package com.example.dhbt.presentation.navigation

import android.util.Log
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.dhbt.R

/**
 * Компонент нижней навигации приложения
 */
@Composable
fun DHbtBottomNavigation(navController: NavController) {
    val items = listOf(
        NavItem(
            route = Dashboard,
            iconResId = R.drawable.ic_launcher_foreground,
            titleResId = R.string.dashboard
        ),
        NavItem(
            route = Tasks,
            iconResId = R.drawable.ic_launcher_foreground,
            titleResId = R.string.tasks
        ),
        NavItem(
            route = Habits,
            iconResId = R.drawable.ic_launcher_foreground,
            titleResId = R.string.habits
        ),
        NavItem(
            route = Statistics,
            iconResId = R.drawable.ic_launcher_foreground,
            titleResId = R.string.statistics
        ),
        NavItem(
            route = More,
            iconResId = R.drawable.ic_launcher_foreground,
            titleResId = R.string.more
        )
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar {
        items.forEach { item ->
            val isSelected = currentDestination?.route?.contains(item.route::class.qualifiedName ?: "") == true

            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = item.iconResId),
                        contentDescription = stringResource(id = item.titleResId)
                    )
                },
                label = { Text(stringResource(id = item.titleResId)) },
                selected = isSelected,
                onClick = {
                    if (!isSelected) {
                        Log.d("Navigation", "Navigating to: ${item.route::class.qualifiedName}")
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    }
}

/**
 * Элемент навигации
 */
data class NavItem(
    val route: Any,
    val iconResId: Int,
    val titleResId: Int
)