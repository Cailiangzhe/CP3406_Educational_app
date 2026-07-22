package com.cailiangzhe.lexidue

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.rememberNavigationSuiteScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cailiangzhe.lexidue.navigation.LexiDueNavHost
import com.cailiangzhe.lexidue.navigation.LexiDueTestTags
import com.cailiangzhe.lexidue.navigation.LexiDueTopLevelDestination
import com.cailiangzhe.lexidue.navigation.navigateToTopLevel

@Composable
fun LexiDueApp(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val navigationSuiteState = rememberNavigationSuiteScaffoldState()
    val showTopLevelNavigation =
        currentDestination == null ||
            LexiDueTopLevelDestination.entries.any { it.matches(currentDestination) }

    LaunchedEffect(showTopLevelNavigation) {
        if (showTopLevelNavigation) {
            navigationSuiteState.show()
        } else {
            navigationSuiteState.hide()
        }
    }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            LexiDueTopLevelDestination.entries.forEach { destination ->
                item(
                    selected = destination.matches(currentDestination),
                    onClick = { navController.navigateToTopLevel(destination) },
                    icon = {
                        Icon(
                            imageVector = destination.icon,
                            contentDescription = null,
                        )
                    },
                    modifier = Modifier.testTag(destination.testTag),
                    label = { Text(stringResource(destination.labelResourceId)) },
                )
            }
        },
        modifier = modifier.fillMaxSize(),
        state = navigationSuiteState,
    ) {
        LexiDueNavHost(
            navController = navController,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

private val LexiDueTopLevelDestination.testTag: String
    get() =
        when (this) {
            LexiDueTopLevelDestination.HOME -> LexiDueTestTags.HOME_NAVIGATION
            LexiDueTopLevelDestination.STATISTICS -> LexiDueTestTags.STATISTICS_NAVIGATION
            LexiDueTopLevelDestination.SETTINGS -> LexiDueTestTags.SETTINGS_NAVIGATION
        }
