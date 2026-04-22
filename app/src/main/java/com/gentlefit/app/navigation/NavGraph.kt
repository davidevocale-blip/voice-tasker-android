package com.gentlefit.app.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.gentlefit.app.ui.screen.coach.CoachScreen
import com.gentlefit.app.ui.screen.goals.GoalsScreen
import com.gentlefit.app.ui.screen.home.HomeScreen
import com.gentlefit.app.ui.screen.news.NewsAdminScreen
import com.gentlefit.app.ui.screen.news.NewsScreen
import com.gentlefit.app.ui.screen.onboarding.OnboardingScreen
import com.gentlefit.app.ui.screen.premium.PremiumScreen
import com.gentlefit.app.ui.screen.profile.ProfileScreen
import com.gentlefit.app.ui.screen.progress.ProgressScreen

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val COACH = "coach"
    const val PROGRESS = "progress"
    const val GOALS = "goals"
    const val NEWS = "news"
    const val NEWS_ADMIN = "news_admin"
    const val PROFILE = "profile"
    const val PREMIUM = "premium"
}

@Composable
fun GentleFitNavGraph(
    navController: NavHostController,
    startDestination: String,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    NavHost(navController = navController, startDestination = startDestination) {

        composable(Routes.ONBOARDING) {
            OnboardingScreen(onComplete = {
                navController.navigate(Routes.HOME) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
            })
        }

        composable(Routes.HOME) {
            HomeScreen(onNavigateToProfile = { navController.navigate(Routes.PROFILE) }, contentPadding = contentPadding)
        }

        composable(Routes.COACH) { CoachScreen(contentPadding = contentPadding) }

        composable(Routes.PROGRESS) { ProgressScreen(contentPadding = contentPadding) }

        composable(Routes.GOALS) { GoalsScreen(contentPadding = contentPadding) }

        composable(Routes.NEWS) {
            NewsScreen(onNavigateToAdmin = { navController.navigate(Routes.NEWS_ADMIN) }, contentPadding = contentPadding)
        }

        composable(Routes.NEWS_ADMIN) {
            NewsAdminScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.PROFILE) {
            ProfileScreen(
                onBack = { navController.popBackStack() },
                onNavigateToPremium = { navController.navigate(Routes.PREMIUM) }
            )
        }

        composable(Routes.PREMIUM) {
            PremiumScreen(onBack = { navController.popBackStack() })
        }
    }
}
