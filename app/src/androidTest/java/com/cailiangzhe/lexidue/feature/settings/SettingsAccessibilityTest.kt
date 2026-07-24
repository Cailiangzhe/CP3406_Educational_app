package com.cailiangzhe.lexidue.feature.settings

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cailiangzhe.lexidue.core.designsystem.LexiDueTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsAccessibilityTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun settingsAtTwoHundredPercentFont_keepsChoiceAndSwitchSemanticsReachable() {
        composeRule.setContent {
            val deviceDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(deviceDensity.density, fontScale = 2f),
            ) {
                LexiDueTheme {
                    SettingsScreen(
                        uiState =
                            SettingsUiState(
                                isLoading = false,
                                hasLoadedSettings = true,
                            ),
                        onAction = {},
                    )
                }
            }
        }

        composeRule
            .onNodeWithText("Settings")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
            .assertIsDisplayed()
        composeRule
            .onNodeWithText("10 words - selected")
            .performScrollTo()
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.Role,
                    Role.RadioButton,
                ),
            ).assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.Selected,
                    true,
                ),
            ).assertHasClickAction()
            .assertHeightIsAtLeast(48.dp)
            .assertIsDisplayed()
        composeRule
            .onNodeWithContentDescription("Sound")
            .performScrollTo()
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.Role,
                    Role.Switch,
                ),
            ).assertHasClickAction()
            .assertHeightIsAtLeast(48.dp)
            .assertIsOff()
            .assertIsDisplayed()
    }
}
