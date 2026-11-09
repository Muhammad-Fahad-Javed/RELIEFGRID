package com.mohallaops.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mohallaops.R
import com.mohallaops.ui.screens.ActivityScreen
import com.mohallaops.ui.screens.DrillScreen
import com.mohallaops.ui.screens.HomeScreen
import com.mohallaops.ui.screens.MapScreen
import com.mohallaops.ui.screens.RequestSheet
import com.mohallaops.ui.screens.ResultsScreen
import com.mohallaops.ui.screens.StationsScreen
import com.mohallaops.ui.theme.MohallaColors

private data class Tab(val route: String, val label: Int, val icon: ImageVector)

private val tabs = listOf(
    Tab("home", R.string.nav_home, Icons.Filled.Home),
    Tab("stations", R.string.nav_stations, Icons.Filled.Menu),
    Tab("drill", R.string.nav_drill, Icons.Filled.Settings),
    Tab("map", R.string.nav_map, Icons.Filled.Place),
    Tab("activity", R.string.nav_activity, Icons.Filled.Info)
)

@Composable
fun AppRoot(vm: MainViewModel) {
    val nav = rememberNavController()
    val snack = remember { SnackbarHostState() }
    val message by vm.message.collectAsStateWithLifecycle()
    var showRequest by remember { mutableStateOf(false) }

    LaunchedEffect(message) {
        message?.let {
            snack.showSnackbar(it)
            vm.consumeMessage()
        }
    }

    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route

    Scaffold(
        containerColor = MohallaColors.Background,
        snackbarHost = { SnackbarHost(snack) },
        bottomBar = {
            NavigationBar(containerColor = MohallaColors.Surface) {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = route == tab.route,
                        onClick = {
                            nav.navigate(tab.route) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(stringResource(tab.label)) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MohallaColors.Navy,
                            selectedTextColor = MohallaColors.Navy,
                            indicatorColor = MohallaColors.Teal.copy(alpha = 0.18f)
                        )
                    )
                }
            }
        }
    ) { pad ->
        NavHost(nav, startDestination = "home", modifier = Modifier.padding(pad)) {
            composable("home") {
                HomeScreen(
                    vm,
                    onNewRequest = { showRequest = true },
                    onOpen = { dest ->
                        nav.navigate(dest) {
                            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable("stations") { StationsScreen(vm, onNewRequest = { showRequest = true }) }
            composable("drill") { DrillScreen(vm, onResults = { nav.navigate("results") }) }
            composable("map") { MapScreen(vm) }
            composable("activity") { ActivityScreen(vm) }
            composable("results") { ResultsScreen(vm) }
        }
    }

    if (showRequest) RequestSheet(vm) { showRequest = false }
}
