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
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.annotation.StringRes
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.voicetasker.app.R
import com.voicetasker.app.ui.component.VoiceTaskerBottomBar
import com.voicetasker.app.ui.component.VoiceTaskerBottomBarItem
import com.voicetasker.app.ui.screen.addnote.AddNoteScreen
import com.voicetasker.app.ui.screen.calendar.CalendarScreen
import com.voicetasker.app.ui.screen.categories.CategoriesScreen
import com.voicetasker.app.ui.screen.home.HomeScreen
import com.voicetasker.app.ui.screen.login.LoginScreen
import com.voicetasker.app.ui.screen.notedetail.NoteDetailScreen
import com.voicetasker.app.ui.screen.paywall.PaywallScreen
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
    data object Login : Screen("login")
    data object Paywall : Screen("paywall/{trigger}") { fun createRoute(trigger: String) = "paywall/$trigger" }
}

data class BottomNavItem(val screen: Screen, @StringRes val labelRes: Int, val selected: ImageVector, val unselected: ImageVector)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Home, R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem(Screen.Calendar, R.string.nav_calendar, Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
    BottomNavItem(Screen.Categories, R.string.nav_categories, Icons.Filled.Category, Icons.Outlined.Category),
    BottomNavItem(Screen.Settings, R.string.nav_settings, Icons.Filled.Settings, Icons.Outlined.Settings)
)

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    val entry by navController.currentBackStackEntryAsState()
    val currentRoute = entry?.destination?.route
    val showBar = currentRoute in bottomNavItems.map { it.screen.route }
    val renderedBottomNavItems = bottomNavItems.map { item ->
        VoiceTaskerBottomBarItem(
            destination = item.screen,
            label = stringResource(item.labelRes),
            selectedIcon = item.selected,
            unselectedIcon = item.unselected
        )
    }

    Scaffold(bottomBar = {
        if (showBar) {
            VoiceTaskerBottomBar(
                items = renderedBottomNavItems,
                selectedItem = renderedBottomNavItems.firstOrNull {
                    currentRoute == it.destination.route
                },
                onItemSelected = { item ->
                    navController.navigate(item.destination.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }) { innerPadding ->
        NavHost(navController, Screen.Home.route, Modifier.padding(innerPadding)) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToRecord = { navController.navigate(Screen.Record.route) },
                    onNavigateToAddNote = { navController.navigate(Screen.AddNote.route) },
                    onNavigateToNoteDetail = { navController.navigate(Screen.NoteDetail.createRoute(it)) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onNavigateToLogin = { navController.navigate(Screen.Login.route) },
                    onNavigateToPaywall = { trigger -> navController.navigate(Screen.Paywall.createRoute(trigger)) }
                )
            }
            composable(Screen.Record.route) {
                RecordScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToLogin = { navController.navigate(Screen.Login.route) }
                )
            }
            composable(Screen.Calendar.route) { CalendarScreen(onNavigateToNoteDetail = { navController.navigate(Screen.NoteDetail.createRoute(it)) }) }
            composable(Screen.NoteDetail.route, arguments = listOf(navArgument("noteId") { type = NavType.LongType })) {
                NoteDetailScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPaywall = { trigger -> navController.navigate(Screen.Paywall.createRoute(trigger)) }
                )
            }
            composable(Screen.AddNote.route) {
                AddNoteScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToLogin = { navController.navigate(Screen.Login.route) }
                )
            }
            composable(Screen.Categories.route) { CategoriesScreen() }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateToLogin = { navController.navigate(Screen.Login.route) },
                    onNavigateToPaywall = { navController.navigate(Screen.Paywall.createRoute("upgrade")) }
                )
            }
            composable(Screen.Login.route) {
                LoginScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onLoginSuccess = { navController.popBackStack() }
                )
            }
            composable(
                Screen.Paywall.route,
                arguments = listOf(navArgument("trigger") { type = NavType.StringType })
            ) { backStackEntry ->
                val trigger = backStackEntry.arguments?.getString("trigger") ?: "upgrade"
                PaywallScreen(
                    trigger = trigger,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToLogin = { navController.navigate(Screen.Login.route) }
                )
            }
        }
    }
}
