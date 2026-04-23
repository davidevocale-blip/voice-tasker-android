package com.voicetasker.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.voicetasker.app.ui.screen.addnote.AddNoteScreen
import com.voicetasker.app.ui.screen.calendar.CalendarScreen
import com.voicetasker.app.ui.screen.categories.CategoriesScreen
import com.voicetasker.app.ui.screen.home.HomeScreen
import com.voicetasker.app.ui.screen.notedetail.NoteDetailScreen
import com.voicetasker.app.ui.screen.record.RecordScreen
import com.voicetasker.app.ui.screen.settings.SettingsScreen

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Record : Screen("record")
    data object Calendar : Screen("calendar")
    data object NoteDetail : Screen("note/{noteId}") { fun createRoute(id: Long) = "note/$id" }
    data object AddNote : Screen("add_note")
    data object Categories : Screen("categories")
    data object Settings : Screen("settings")
}

data class BottomNavItem(val screen: Screen, val label: String, val selected: ImageVector, val unselected: ImageVector)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Home, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem(Screen.Calendar, "Calendario", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
    BottomNavItem(Screen.Categories, "Categorie", Icons.Filled.Category, Icons.Outlined.Category),
    BottomNavItem(Screen.Settings, "Impostazioni", Icons.Filled.Settings, Icons.Outlined.Settings)
)

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    val entry by navController.currentBackStackEntryAsState()
    val currentRoute = entry?.destination?.route
    val showBar = currentRoute in bottomNavItems.map { it.screen.route }

    Scaffold(bottomBar = {
        if (showBar) NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
            bottomNavItems.forEach { item ->
                val sel = currentRoute == item.screen.route
                NavigationBarItem(sel, onClick = { navController.navigate(item.screen.route) { popUpTo(navController.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true } },
                    icon = { Icon(if (sel) item.selected else item.unselected, item.label) },
                    label = { Text(item.label, style = MaterialTheme.typography.labelSmall) },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = MaterialTheme.colorScheme.primary, selectedTextColor = MaterialTheme.colorScheme.primary, indicatorColor = MaterialTheme.colorScheme.primaryContainer))
            }
        }
    }) { innerPadding ->
        NavHost(navController, Screen.Home.route, Modifier.padding(innerPadding)) {
            composable(Screen.Home.route) { HomeScreen(onNavigateToRecord = { navController.navigate(Screen.Record.route) }, onNavigateToAddNote = { navController.navigate(Screen.AddNote.route) }, onNavigateToNoteDetail = { navController.navigate(Screen.NoteDetail.createRoute(it)) }, onNavigateToSettings = { navController.navigate(Screen.Settings.route) }) }
            composable(Screen.Record.route) { RecordScreen(onNavigateBack = { navController.popBackStack() }) }
            composable(Screen.Calendar.route) { CalendarScreen(onNavigateToNoteDetail = { navController.navigate(Screen.NoteDetail.createRoute(it)) }) }
            composable(Screen.NoteDetail.route, arguments = listOf(navArgument("noteId") { type = NavType.LongType })) { NoteDetailScreen(onNavigateBack = { navController.popBackStack() }) }
            composable(Screen.AddNote.route) { AddNoteScreen(onNavigateBack = { navController.popBackStack() }) }
            composable(Screen.Categories.route) { CategoriesScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }
        }
    }
}
