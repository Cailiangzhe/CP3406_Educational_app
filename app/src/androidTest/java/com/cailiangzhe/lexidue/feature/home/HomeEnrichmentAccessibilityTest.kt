package com.cailiangzhe.lexidue.feature.home

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
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cailiangzhe.lexidue.core.designsystem.LexiDueTheme
import com.cailiangzhe.lexidue.domain.model.PartOfSpeech
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeEnrichmentAccessibilityTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun enrichmentAtTwoHundredPercentFont_keepsStatusAndSavedSourceAccessible() {
        composeRule.setContent {
            val deviceDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(deviceDensity.density, fontScale = 2f),
            ) {
                LexiDueTheme {
                    HomeScreen(
                        onStartPractice = {},
                        onOpenStatistics = {},
                        onOpenSettings = {},
                        uiState = enrichmentFailureState,
                    )
                }
            }
        }

        composeRule
            .onNodeWithText("Refresh up to 5 due words")
            .performScrollTo()
            .assertHasClickAction()
            .assertHeightIsAtLeast(48.dp)
            .assertIsDisplayed()
        composeRule
            .onNodeWithText(
                "Refresh could not update any selected words. Unavailable: 1. " +
                    "Saved content is still shown and practice remains available.",
            ).performScrollTo()
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.LiveRegion,
                    LiveRegionMode.Polite,
                ),
            ).assertIsDisplayed()
        composeRule
            .onNodeWithText("analyse")
            .performScrollTo()
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
            .assertIsDisplayed()
        composeRule
            .onNodeWithText(
                "Provider: Free Dictionary API",
                substring = true,
            ).performScrollTo()
            .assertIsDisplayed()
        composeRule
            .onNodeWithText(
                "Source: https://api.dictionaryapi.dev/api/v2/entries/en/analyse",
                substring = true,
            ).performScrollTo()
            .assertIsDisplayed()
    }

    private companion object {
        val enrichmentFailureState =
            HomeUiState(
                dueWordCount = 3,
                localDataReady = true,
                hasLoadedSettings = true,
                isLoadingEnrichmentCache = false,
                enrichmentStatus = HomeEnrichmentStatus.Failed(failedWordCount = 1),
                enrichmentCards =
                    listOf(
                        HomeEnrichmentCard(
                            wordId = "word-analyse",
                            displayWord = "analyse",
                            senses =
                                listOf(
                                    HomeEnrichmentSense(
                                        partOfSpeech = PartOfSpeech.VERB,
                                        definition = "Examine something methodically.",
                                        example = "Researchers analyse the results.",
                                        phonetic = "/ˈænəlaɪz/",
                                        provider = "Free Dictionary API",
                                        source =
                                            "https://api.dictionaryapi.dev/api/v2/entries/en/analyse",
                                    ),
                                ),
                            fetchedAtEpochMillis = 1_700_000_000_000L,
                            isStale = false,
                        ),
                    ),
            )
    }
}
