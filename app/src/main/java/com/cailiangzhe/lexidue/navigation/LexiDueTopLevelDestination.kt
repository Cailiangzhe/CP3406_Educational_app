package com.cailiangzhe.lexidue.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import com.cailiangzhe.lexidue.R

enum class LexiDueTopLevelDestination(
    @param:StringRes val labelResourceId: Int,
    val icon: ImageVector,
) {
    HOME(
        labelResourceId = R.string.home_title,
        icon = Icons.Default.Home,
    ),
    STATISTICS(
        labelResourceId = R.string.statistics_title,
        icon = Icons.Default.Info,
    ),
    SETTINGS(
        labelResourceId = R.string.settings_title,
        icon = Icons.Default.Settings,
    ),
    ;

    fun matches(destination: NavDestination?): Boolean =
        destination?.hierarchy?.any { candidate ->
            when (this) {
                HOME -> candidate.hasRoute<HomeRoute>()
                STATISTICS -> candidate.hasRoute<StatisticsRoute>()
                SETTINGS -> candidate.hasRoute<SettingsRoute>()
            }
        } == true
}

fun NavHostController.navigateToTopLevel(destination: LexiDueTopLevelDestination) {
    val route =
        when (destination) {
            LexiDueTopLevelDestination.HOME -> HomeRoute
            LexiDueTopLevelDestination.STATISTICS -> StatisticsRoute
            LexiDueTopLevelDestination.SETTINGS -> SettingsRoute
        }

    navigate(route) {
        popUpTo<HomeRoute> {
            saveState = destination != LexiDueTopLevelDestination.HOME
        }
        launchSingleTop = true
        restoreState = destination != LexiDueTopLevelDestination.HOME
    }
}
