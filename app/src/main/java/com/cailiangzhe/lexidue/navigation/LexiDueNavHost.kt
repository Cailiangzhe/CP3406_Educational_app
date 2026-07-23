package com.cailiangzhe.lexidue.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.cailiangzhe.lexidue.feature.home.HomeRouteScreen
import com.cailiangzhe.lexidue.feature.practice.PracticeRouteScreen
import com.cailiangzhe.lexidue.feature.practice.PracticeSummaryRouteScreen
import com.cailiangzhe.lexidue.feature.settings.SettingsScreen
import com.cailiangzhe.lexidue.feature.statistics.StatisticsScreen

object LexiDueTestTags {
    const val HOME_SCREEN = "home_screen"
    const val PRACTICE_SCREEN = "practice_screen"
    const val PRACTICE_SUMMARY_SCREEN = "practice_summary_screen"
    const val STATISTICS_SCREEN = "statistics_screen"
    const val SETTINGS_SCREEN = "settings_screen"
    const val HOME_NAVIGATION = "home_navigation"
    const val STATISTICS_NAVIGATION = "statistics_navigation"
    const val SETTINGS_NAVIGATION = "settings_navigation"
}

@Composable
fun LexiDueNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        modifier = modifier,
    ) {
        composable<HomeRoute> {
            HomeRouteScreen(
                onOpenPractice = { sessionId ->
                    navController.navigate(PracticeRoute(sessionId = sessionId))
                },
                onOpenStatistics = {
                    navController.navigateToTopLevel(LexiDueTopLevelDestination.STATISTICS)
                },
                onOpenSettings = {
                    navController.navigateToTopLevel(LexiDueTopLevelDestination.SETTINGS)
                },
                modifier = Modifier.testTag(LexiDueTestTags.HOME_SCREEN),
            )
        }
        composable<PracticeRoute> {
            PracticeRouteScreen(
                onOpenSummary = { sessionId ->
                    navController.navigate(PracticeSummaryRoute(sessionId)) {
                        popUpTo<PracticeRoute> { inclusive = true }
                    }
                },
                onReturnHome = {
                    navController.navigateToTopLevel(LexiDueTopLevelDestination.HOME)
                },
                modifier = Modifier.testTag(LexiDueTestTags.PRACTICE_SCREEN),
            )
        }
        composable<PracticeSummaryRoute> {
            PracticeSummaryRouteScreen(
                onDone = {
                    navController.navigateToTopLevel(LexiDueTopLevelDestination.HOME)
                },
                onOpenStatistics = {
                    navController.navigateToTopLevel(LexiDueTopLevelDestination.STATISTICS)
                },
                modifier = Modifier.testTag(LexiDueTestTags.PRACTICE_SUMMARY_SCREEN),
            )
        }
        composable<StatisticsRoute> {
            StatisticsScreen(
                modifier = Modifier.testTag(LexiDueTestTags.STATISTICS_SCREEN),
            )
        }
        composable<SettingsRoute> {
            SettingsScreen(
                modifier = Modifier.testTag(LexiDueTestTags.SETTINGS_SCREEN),
            )
        }
    }
}
