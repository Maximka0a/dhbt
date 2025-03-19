package com.example.dhbt.presentation.navigation

import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.dhbt.R

//sealed class BottomNavItem(val route: String, val iconResId: Int, val labelResId: Int) {
//    object Dashboard : BottomNavItem("Dashboard", R.drawable.ic_dashboard, R.string.dashboard)
//    object Tasks : BottomNavItem("Tasks", R.drawable.ic_tasks, R.string.tasks)
//    object Habits : BottomNavItem("Habits", R.drawable.ic_habits, R.string.habits)
//    object Statistics : BottomNavItem("Statistics", R.drawable.ic_statistics, R.string.statistics)
//    object More : BottomNavItem("More", R.drawable.ic_more, R.string.more)
//}

sealed class BottomNavItem(val route: String, val iconResId: Int, val labelResId: Int) {
    object Dashboard : BottomNavItem("Dashboard", R.drawable.ic_launcher_foreground, R.string.dashboard)
    object Tasks : BottomNavItem("Tasks", R.drawable.ic_launcher_foreground, R.string.tasks)
    object Habits : BottomNavItem("Habits", R.drawable.ic_launcher_foreground, R.string.habits)
    object Statistics : BottomNavItem("Statistics", R.drawable.ic_launcher_foreground, R.string.statistics)
    object More : BottomNavItem("More", R.drawable.ic_launcher_foreground, R.string.more)
}

@Composable
fun DHbtBottomNavigation(navController: NavController) {
    val items = listOf(
        BottomNavItem.Dashboard,
        BottomNavItem.Tasks,
        BottomNavItem.Habits,
        BottomNavItem.Statistics,
        BottomNavItem.More
    )

    BottomNavigation {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { item ->
            BottomNavigationItem(
                icon = { Icon(painter = painterResource(id = item.iconResId), contentDescription = null) },
                label = { Text(text = stringResource(id = item.labelResId)) },
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        // Избегаем создания несколько копий одного назначения в стеке
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        // Избегаем повторного создания одного и того же назначения
                        launchSingleTop = true
                        // Сохраняем состояние при навигации
                        restoreState = true
                    }
                }
            )
        }
    }
}