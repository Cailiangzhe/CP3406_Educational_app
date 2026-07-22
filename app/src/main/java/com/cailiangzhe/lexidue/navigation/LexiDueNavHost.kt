package com.cailiangzhe.lexidue.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.cailiangzhe.lexidue.feature.home.HomeScreen
import com.cailiangzhe.lexidue.feature.practice.PracticeScreen
import com.cailiangzhe.lexidue.feature.settings.SettingsScreen
import com.cailiangzhe.lexidue.feature.statistics.StatisticsScreen
import java.util.UUID

object LexiDueTestTags {
    const val HOME_SCREEN = "home_screen"
    const val PRACTICE_SCREEN = "practice_screen"
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
            HomeScreen(
                onStartPractice = {
                    navController.navigate(PracticeRoute(sessionId = UUID.randomUUID().toString()))
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
        composable<PracticeRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<PracticeRoute>()
            key(route.sessionId) {
                PracticeScreen(
                    onExit = navController::navigateUp,
                    modifier = Modifier.testTag(LexiDueTestTags.PRACTICE_SCREEN),
                )
            }
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
