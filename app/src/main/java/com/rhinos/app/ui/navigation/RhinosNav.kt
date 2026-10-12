package com.rhinos.app.ui.navigation

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.rhinos.app.AppContainer
import com.rhinos.app.ui.daily.DailyPnlScreen
import com.rhinos.app.ui.home.HomeScreen
import com.rhinos.app.ui.margin.MarginScreen
import com.rhinos.app.ui.more.MoreScreen
import com.rhinos.app.ui.notes.NotesScreen
import com.rhinos.app.ui.reports.ReportsScreen
import com.rhinos.app.ui.trades.TradesScreen
import kotlin.math.abs

/** Main sections. Bottom bar keeps 5 slots; Reports/Notes live behind "More". */
enum class Section(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    HOME("home", "Home", Icons.Filled.Home),
    TRADES("trades", "Trades", Icons.Filled.ShowChart),
    DAILY("daily", "Daily P&L", Icons.Filled.Analytics),
    MARGIN("margin", "Expiry", Icons.Filled.Timer),
    MORE("more", "More", Icons.Filled.MoreHoriz)
}

@Composable
fun RhinosRoot(container: AppContainer) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val currentSection = Section.entries.firstOrNull { section ->
        currentDestination?.hierarchy?.any {
            it.route == section.route || (section == Section.MORE && (
                it.route == "reports" || it.route == "notes" || it.route == "settings"))
        } == true
    } ?: Section.HOME

    fun navigateToSection(section: Section) {
        navController.navigate(section.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val settings by container.settingsRepository.settings.collectAsStateWithLifecycle()
    if (!settings.onboarded) {
        OnboardingScreen(onGetStarted = { container.settingsRepository.setOnboarded() })
        return
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                Section.entries.forEach { section ->
                    val selected = currentDestination?.hierarchy?.any {
                        it.route == section.route || (section == Section.MORE && (
                            it.route == "reports" || it.route == "notes" || it.route == "settings"))
                    } == true
                    NavigationBarItem(
                        modifier = Modifier.weight(1f),
                        selected = selected,
                        onClick = { navigateToSection(section) },
                        icon = { Icon(section.icon, contentDescription = section.label) },
                        label = {
                            Text(
                                section.label,
                                maxLines = 1,
                                softWrap = false,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp
                            )
                        },
                        alwaysShowLabel = false,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Section.HOME.route,
            modifier = Modifier
                .padding(padding)
                .pointerInput(currentDestination?.route) {
                    var dragDistance = 0f
                    val swipeThreshold = 72.dp.toPx()
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { _, dragAmount ->
                            dragDistance += dragAmount
                        },
                        onDragEnd = {
                            if (abs(dragDistance) >= swipeThreshold) {
                                val direction = if (dragDistance < 0f) 1 else -1
                                val destinationIndex = (currentSection.ordinal + direction)
                                    .coerceIn(Section.entries.indices)
                                navigateToSection(Section.entries[destinationIndex])
                            }
                            dragDistance = 0f
                        },
                        onDragCancel = { dragDistance = 0f }
                    )
                }
        ) {
            composable(Section.HOME.route) { HomeScreen(container, onOpenTrades = { navController.navigate("trades") }) }
            composable(Section.TRADES.route) { TradesScreen(container) }
            composable(Section.DAILY.route) { DailyPnlScreen(container) }
            composable(Section.MARGIN.route) { MarginScreen(container) }
            composable(Section.MORE.route) { MoreScreen(container, navController) }
            composable("reports") { ReportsScreen(container) }
            composable("notes") { NotesScreen(container) }
            composable("settings") { com.rhinos.app.ui.settings.SettingsScreen(container) }
        }
    }
}
