package com.cailiangzhe.lexidue.feature.practice

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
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
class PracticeAccessibilityTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun questionAtTwoHundredPercentFont_keepsInstructionsAndLargeAnswerTargets() {
        setContentAtFontScale(
            uiState =
                PracticeUiState(
                    sessionId = "session-1",
                    content = PracticeContent.Question(question),
                ),
        )

        composeRule
            .onNodeWithText("Which definition best matches this word?")
            .assertIsDisplayed()
        composeRule
            .onNodeWithTag(PracticeTestTags.CHOICE_PREFIX + 0)
            .performScrollTo()
            .assertHasClickAction()
            .assertHeightIsAtLeast(48.dp)
            .assertIsDisplayed()
    }

    @Test
    fun feedback_isAnnouncedAndContinueHasLargeTarget() {
        composeRule.setContent {
            LexiDueTheme {
                PracticeScreen(
                    uiState =
                        PracticeUiState(
                            sessionId = "session-1",
                            content =
                                PracticeContent.Feedback(
                                    question = question,
                                    selectedChoiceId = "answer",
                                    feedback =
                                        PracticeFeedback(
                                            kind = PracticeFeedbackKind.CORRECT,
                                            message = "That answer matches the saved meaning.",
                                            correctChoiceId = "answer",
                                        ),
                                ),
                        ),
                )
            }
        }

        composeRule
            .onNodeWithTag(PracticeTestTags.FEEDBACK)
            .performScrollTo()
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.LiveRegion,
                    LiveRegionMode.Polite,
                ),
            ).assertIsDisplayed()
        composeRule
            .onNodeWithTag(PracticeTestTags.CONTINUE)
            .performScrollTo()
            .assertHasClickAction()
            .assertHeightIsAtLeast(48.dp)
            .assertIsDisplayed()
    }

    @Test
    fun summaryAtTwoHundredPercentFont_keepsReturnHomeActionReachable() {
        composeRule.setContent {
            val deviceDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(deviceDensity.density, fontScale = 2f),
            ) {
                LexiDueTheme {
                    PracticeSummaryScreen(
                        onDone = {},
                        onOpenStatistics = {},
                        uiState =
                            PracticeSummaryUiState(
                                isLoading = false,
                                correctCount = 4,
                                incorrectCount = 1,
                                accuracyPercent = 80,
                                reviewWords = listOf("analyse"),
                            ),
                    )
                }
            }
        }

        composeRule
            .onNodeWithText("Session summary")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
            .assertIsDisplayed()
        composeRule
            .onNodeWithTag(PracticeSummaryTestTags.DONE)
            .performScrollTo()
            .assertHasClickAction()
            .assertHeightIsAtLeast(48.dp)
            .assertIsDisplayed()
    }

    private fun setContentAtFontScale(uiState: PracticeUiState) {
        composeRule.setContent {
            val deviceDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(deviceDensity.density, fontScale = 2f),
            ) {
                LexiDueTheme {
                    PracticeScreen(uiState = uiState)
                }
            }
        }
    }

    private companion object {
        val question =
            PracticeQuestionUi(
                id = "question-1",
                questionNumber = 1,
                totalQuestions = 5,
                prompt = "analyse",
                mode = PracticePromptMode.WORD_TO_DEFINITION,
                choices =
                    listOf(
                        PracticeChoice("answer", "Examine something methodically."),
                        PracticeChoice("other-1", "Make something smaller."),
                        PracticeChoice("other-2", "State a result without evidence."),
                    ),
            )
    }
}
