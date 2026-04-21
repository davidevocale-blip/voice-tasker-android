package com.gentlefit.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gentlefit.app.data.preferences.UserPreferences
import com.gentlefit.app.navigation.GentleFitNavGraph
import com.gentlefit.app.navigation.Routes
import com.gentlefit.app.ui.components.GentleFitBottomNav
import com.gentlefit.app.ui.theme.GentleFitTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var userPreferences: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val hasOnboarded = runBlocking { userPreferences.hasCompletedOnboarding.first() }

        setContent {
            GentleFitTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route ?: ""

                val showBottomNav = currentRoute in listOf(
                    Routes.HOME, Routes.COACH, Routes.PROGRESS, Routes.GOALS, Routes.NEWS
                )

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (showBottomNav) {
                            GentleFitBottomNav(
                                currentRoute = currentRoute,
                                onNavigate = { route ->
                                    navController.navigate(route) {
                                        popUpTo(Routes.HOME) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                ) { innerPadding ->
                    GentleFitNavGraph(
                        navController = navController,
                        startDestination = if (hasOnboarded) Routes.HOME else Routes.ONBOARDING
                    )
                }
            }
        }
    }
}
