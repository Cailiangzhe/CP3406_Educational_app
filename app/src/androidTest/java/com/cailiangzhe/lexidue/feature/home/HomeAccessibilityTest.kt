package com.cailiangzhe.lexidue.feature.home

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cailiangzhe.lexidue.core.designsystem.LexiDueTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeAccessibilityTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun twoHundredPercentFont_keepsHeadingAndMinimumTouchTarget() {
        composeRule.setContent {
            val deviceDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides
                    Density(
                        density = deviceDensity.density,
                        fontScale = 2f,
                    ),
            ) {
                LexiDueTheme {
                    HomeScreen(
                        onStartPractice = {},
                        onOpenStatistics = {},
                        onOpenSettings = {},
                    )
                }
            }
        }

        composeRule
            .onNodeWithText("Home")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
            .assertIsDisplayed()
        composeRule
            .onNodeWithText("Start practice")
            .assertHasClickAction()
            .assertHeightIsAtLeast(48.dp)
            .assertIsDisplayed()
    }
}
