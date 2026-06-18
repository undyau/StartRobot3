package com.undy.startrobot3.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.undy.startrobot3.ui.config.ConfigScreen
import com.undy.startrobot3.ui.run.RunScreen
import com.undy.startrobot3.ui.startlist.StartListScreen

private data class TopLevelRoute(val label: String, val route: String, val icon: @Composable () -> Unit)

private val routes = listOf(
    TopLevelRoute("Run", "run") { Icon(Icons.Default.PlayArrow, "Run") },
    TopLevelRoute("Start List", "startlist") { Icon(Icons.AutoMirrored.Filled.List, "Start List") },
    TopLevelRoute("Config", "config") { Icon(Icons.Default.Settings, "Config") },
)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val current = backStack?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                routes.forEach { r ->
                    NavigationBarItem(
                        selected = current?.hierarchy?.any { it.route == r.route } == true,
                        onClick = {
                            navController.navigate(r.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = r.icon,
                        label = { Text(r.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(navController, startDestination = "run",
            Modifier.padding(padding)) {
            composable("run") { RunScreen() }
            composable("startlist") { StartListScreen() }
            composable("config") { ConfigScreen() }
        }
    }
}
