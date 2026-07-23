package com.cailiangzhe.lexidue.navigation

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.waitUntilAtLeastOneExists
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cailiangzhe.lexidue.MainActivity
import com.cailiangzhe.lexidue.R
import com.cailiangzhe.lexidue.feature.practice.PracticeSummaryTestTags
import com.cailiangzhe.lexidue.feature.practice.PracticeTestTags
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
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
        startPractice()
        composeRule.onNodeWithTag(LexiDueTestTags.PRACTICE_SCREEN).assertIsDisplayed()

        pressBack()

        composeRule
            .onNodeWithText(composeRule.activity.getString(R.string.practice_exit_dialog_title))
            .assertIsDisplayed()
        composeRule
            .onNodeWithText(composeRule.activity.getString(R.string.practice_exit_dialog_confirm))
            .performClick()

        composeRule.waitUntilAtLeastOneExists(
            hasTestTag(LexiDueTestTags.HOME_SCREEN),
            timeoutMillis = STATE_TIMEOUT_MILLIS,
        )
        composeRule.onNodeWithTag(LexiDueTestTags.HOME_SCREEN).assertIsDisplayed()
    }

    @Test
    fun completeLocalSession_opensSavedSummary_thenReturnsHome() {
        startPractice()
        composeRule.onNodeWithTag(LexiDueTestTags.PRACTICE_SCREEN).assertIsDisplayed()

        var answeredQuestions = 0
        while (
            composeRule
                .onAllNodes(hasTestTag(LexiDueTestTags.PRACTICE_SUMMARY_SCREEN))
                .fetchSemanticsNodes()
                .isEmpty() &&
            answeredQuestions < MAX_SESSION_QUESTIONS
        ) {
            val enabledChoice = hasAnyChoiceTag().and(isEnabled())
            composeRule.waitUntilAtLeastOneExists(enabledChoice, timeoutMillis = STATE_TIMEOUT_MILLIS)
            composeRule.onAllNodes(enabledChoice)[0].performClick()
            composeRule.waitUntilAtLeastOneExists(
                hasTestTag(PracticeTestTags.FEEDBACK),
                timeoutMillis = STATE_TIMEOUT_MILLIS,
            )
            composeRule.onNodeWithTag(PracticeTestTags.CONTINUE).performClick()
            answeredQuestions += 1
            composeRule.waitForIdle()
        }

        composeRule
            .onNodeWithTag(LexiDueTestTags.PRACTICE_SUMMARY_SCREEN)
            .assertIsDisplayed()
        composeRule.waitUntilAtLeastOneExists(
            hasTestTag(PracticeSummaryTestTags.CONTENT),
            timeoutMillis = STATE_TIMEOUT_MILLIS,
        )
        composeRule.onNodeWithTag(PracticeSummaryTestTags.DONE).performClick()
        composeRule.waitUntilAtLeastOneExists(
            hasTestTag(LexiDueTestTags.HOME_SCREEN),
            timeoutMillis = STATE_TIMEOUT_MILLIS,
        )
        composeRule.onNodeWithTag(LexiDueTestTags.HOME_SCREEN).assertIsDisplayed()
    }

    private fun startPractice() {
        val startLabel = composeRule.activity.getString(R.string.action_start_practice)
        val enabledStart = hasText(startLabel).and(isEnabled())
        composeRule.waitUntilAtLeastOneExists(enabledStart, timeoutMillis = STATE_TIMEOUT_MILLIS)
        composeRule.onNode(enabledStart).performClick()
        composeRule.waitUntilAtLeastOneExists(
            hasTestTag(LexiDueTestTags.PRACTICE_SCREEN),
            timeoutMillis = STATE_TIMEOUT_MILLIS,
        )
    }

    private fun hasAnyChoiceTag(): SemanticsMatcher =
        (0 until MAX_ANSWER_CHOICES)
            .map { index -> hasTestTag(PracticeTestTags.CHOICE_PREFIX + index) }
            .reduce { matcher, next -> matcher.or(next) }

    private companion object {
        const val STATE_TIMEOUT_MILLIS = 15_000L
        const val MAX_SESSION_QUESTIONS = 30
        const val MAX_ANSWER_CHOICES = 4
    }
}
