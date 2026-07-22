package com.cailiangzhe.lexidue.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cailiangzhe.lexidue.MainActivity
import com.cailiangzhe.lexidue.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LexiDueNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun requiredTopLevelScreens_areReachable() {
        composeRule.onNodeWithTag(LexiDueTestTags.HOME_SCREEN).assertIsDisplayed()

        composeRule.onNodeWithTag(LexiDueTestTags.STATISTICS_NAVIGATION).performClick()
        composeRule.onNodeWithTag(LexiDueTestTags.STATISTICS_SCREEN).assertIsDisplayed()

        composeRule.onNodeWithTag(LexiDueTestTags.SETTINGS_NAVIGATION).performClick()
        composeRule.onNodeWithTag(LexiDueTestTags.SETTINGS_SCREEN).assertIsDisplayed()

        composeRule.onNodeWithTag(LexiDueTestTags.HOME_NAVIGATION).performClick()
        composeRule.onNodeWithTag(LexiDueTestTags.HOME_SCREEN).assertIsDisplayed()
    }

    @Test
    fun practiceSystemBack_returnsToHome() {
        composeRule
            .onNodeWithText(composeRule.activity.getString(R.string.action_start_practice))
            .performClick()
        composeRule.onNodeWithTag(LexiDueTestTags.PRACTICE_SCREEN).assertIsDisplayed()

        pressBack()

        composeRule.onNodeWithTag(LexiDueTestTags.HOME_SCREEN).assertIsDisplayed()
    }
}
