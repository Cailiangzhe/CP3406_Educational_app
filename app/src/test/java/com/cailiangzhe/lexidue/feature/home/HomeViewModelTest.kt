package com.cailiangzhe.lexidue.feature.home

import com.cailiangzhe.lexidue.domain.model.DictionaryRefreshFailure
import com.cailiangzhe.lexidue.domain.model.DictionaryRefreshFailureReason
import com.cailiangzhe.lexidue.domain.model.DictionaryRefreshResult
import com.cailiangzhe.lexidue.domain.model.LearningStatistics
import com.cailiangzhe.lexidue.domain.model.PracticeDifficulty
import com.cailiangzhe.lexidue.domain.model.SavedDictionaryEnrichment
import com.cailiangzhe.lexidue.domain.model.ThemeMode
import com.cailiangzhe.lexidue.domain.model.UserSettings
import com.cailiangzhe.lexidue.domain.repository.DictionaryEnrichmentRepository
import com.cailiangzhe.lexidue.domain.repository.SettingsRepository
import com.cailiangzhe.lexidue.domain.usecase.StartPracticeSession
import com.cailiangzhe.lexidue.domain.usecase.TimeProvider
import com.cailiangzhe.lexidue.testing.FakeStatisticsRepository
import com.cailiangzhe.lexidue.testing.FakeWordRepository
import com.cailiangzhe.lexidue.testing.MainDispatcherRule
import com.cailiangzhe.lexidue.testing.RecordingPracticeSessionRepository
import com.cailiangzhe.lexidue.testing.SequenceIdProvider
import com.cailiangzhe.lexidue.testing.testWord
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun startDoubleTap_createsOneSession_andEmitsOneNavigationEffect() =
        runTest(mainDispatcherRule.dispatcher) {
            val words = (1..12).map(::testWord)
            val wordRepository = FakeWordRepository(words)
            val sessionRepository = RecordingPracticeSessionRepository()
            val createGate = CompletableDeferred<Unit>()
            sessionRepository.beforeCreate = { createGate.await() }
            val statisticsRepository =
                FakeStatisticsRepository(
                    LearningStatistics(
                        totalSessions = 0,
                        completedSessions = 0,
                        totalAttempts = 0,
                        correctAttempts = 0,
                        incorrectAttempts = 0,
                        skippedAttempts = 0,
                        dueWords = 8,
                        masteredWords = 2,
                    ),
                )
            val startPracticeSession =
                StartPracticeSession(
                    wordRepository = wordRepository,
                    practiceSessionRepository = sessionRepository,
                    timeProvider = TimeProvider { 5_000L },
                    idProvider =
                        SequenceIdProvider(
                            listOf("session-home") + (1..10).map { "home-question-$it" },
                        ),
                )
            val viewModel =
                HomeViewModel(
                    wordRepository = wordRepository,
                    statisticsRepository = statisticsRepository,
                    dictionaryEnrichmentRepository = FakeDictionaryEnrichmentRepository(),
                    settingsRepository =
                        FakeHomeSettingsRepository(
                            UserSettings(
                                sessionLength = 5,
                                difficulty = PracticeDifficulty.CHALLENGE,
                            ),
                        ),
                    startPracticeSession = startPracticeSession,
                    timeProvider = TimeProvider { 5_000L },
                )

            advanceUntilIdle()
            assertFalse(viewModel.uiState.value.isLoading)
            assertEquals(8, viewModel.uiState.value.dueWordCount)
            assertEquals(2, viewModel.uiState.value.masteredWordCount)
            assertEquals(5, viewModel.uiState.value.sessionLength)
            assertEquals(PracticeDifficulty.CHALLENGE, viewModel.uiState.value.difficulty)

            val effect =
                backgroundScope.async(start = CoroutineStart.UNDISPATCHED) {
                    viewModel.effects.first()
                }
            viewModel.onAction(HomeUiAction.StartPractice)
            viewModel.onAction(HomeUiAction.StartPractice)
            runCurrent()

            assertTrue(viewModel.uiState.value.isStarting)
            assertEquals(1, sessionRepository.createSessionCallCount)

            createGate.complete(Unit)
            advanceUntilIdle()

            assertEquals(HomeEffect.OpenPractice("session-home"), effect.await())
            assertEquals(1, sessionRepository.createSessionCallCount)
            assertFalse(viewModel.uiState.value.isStarting)
            assertEquals(5, sessionRepository.createdSession?.plannedWordCount)
            assertEquals(PracticeDifficulty.CHALLENGE, sessionRepository.createdSession?.difficulty)
        }

    @Test
    fun enrichDoubleTap_runsOneBoundedRefresh_andReportsPartialFailure() =
        runTest(mainDispatcherRule.dispatcher) {
            val words = (1..8).map(::testWord)
            val wordRepository = FakeWordRepository(words)
            val sessionRepository = RecordingPracticeSessionRepository()
            val refreshGate = CompletableDeferred<Unit>()
            val enrichmentRepository =
                FakeDictionaryEnrichmentRepository().apply {
                    beforeRefresh = { refreshGate.await() }
                    refreshResult =
                        DictionaryRefreshResult(
                            selectedWordCount = 5,
                            refreshedWordCount = 3,
                            freshWordCount = 1,
                            failures =
                                listOf(
                                    DictionaryRefreshFailure(
                                        displayWord = "Word 8",
                                        reason = DictionaryRefreshFailureReason.NETWORK,
                                    ),
                                ),
                        )
                }
            val viewModel =
                HomeViewModel(
                    wordRepository = wordRepository,
                    statisticsRepository =
                        FakeStatisticsRepository(
                            LearningStatistics(
                                totalSessions = 0,
                                completedSessions = 0,
                                totalAttempts = 0,
                                correctAttempts = 0,
                                incorrectAttempts = 0,
                                skippedAttempts = 0,
                                dueWords = 8,
                                masteredWords = 0,
                            ),
                        ),
                    dictionaryEnrichmentRepository = enrichmentRepository,
                    settingsRepository = FakeHomeSettingsRepository(),
                    startPracticeSession =
                        StartPracticeSession(
                            wordRepository = wordRepository,
                            practiceSessionRepository = sessionRepository,
                            timeProvider = TimeProvider { 5_000L },
                            idProvider = SequenceIdProvider((1..20).map { "unused-$it" }),
                        ),
                    timeProvider = TimeProvider { 5_000L },
                )
            advanceUntilIdle()

            viewModel.onAction(HomeUiAction.EnrichDeck)
            viewModel.onAction(HomeUiAction.EnrichDeck)
            runCurrent()

            assertTrue(viewModel.uiState.value.isEnriching)
            assertEquals(1, enrichmentRepository.refreshCallCount)

            refreshGate.complete(Unit)
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isEnriching)
            assertEquals(
                HomeEnrichmentStatus.PartialFailure(
                    refreshedWordCount = 3,
                    freshWordCount = 1,
                    failedWordCount = 1,
                ),
                viewModel.uiState.value.enrichmentStatus,
            )
        }

    private class FakeDictionaryEnrichmentRepository : DictionaryEnrichmentRepository {
        val saved = MutableStateFlow<List<SavedDictionaryEnrichment>>(emptyList())
        var refreshResult = DictionaryRefreshResult(0, 0, 0, emptyList())
        var refreshCallCount = 0
            private set
        var beforeRefresh: suspend () -> Unit = {}

        override fun observeSavedEnrichment(limit: Int): Flow<List<SavedDictionaryEnrichment>> = saved

        override suspend fun refreshDueWords(
            nowEpochMillis: Long,
            limit: Int,
        ): DictionaryRefreshResult {
            refreshCallCount += 1
            beforeRefresh()
            return refreshResult
        }
    }

    private class FakeHomeSettingsRepository(
        initialSettings: UserSettings = UserSettings(),
    ) : SettingsRepository {
        private val settings = MutableStateFlow(initialSettings)

        override fun observeSettings(): Flow<UserSettings> = settings

        override suspend fun setSessionLength(wordCount: Int) {
            settings.value = settings.value.copy(sessionLength = wordCount)
        }

        override suspend fun setDifficulty(difficulty: PracticeDifficulty) {
            settings.value = settings.value.copy(difficulty = difficulty)
        }

        override suspend fun setThemeMode(themeMode: ThemeMode) {
            settings.value = settings.value.copy(themeMode = themeMode)
        }

        override suspend fun setSoundEnabled(enabled: Boolean) {
            settings.value = settings.value.copy(soundEnabled = enabled)
        }

        override suspend fun setHapticsEnabled(enabled: Boolean) {
            settings.value = settings.value.copy(hapticsEnabled = enabled)
        }

        override suspend fun setReducedMotion(enabled: Boolean) {
            settings.value = settings.value.copy(reducedMotion = enabled)
        }

        override suspend fun setOnboardingCompleted(completed: Boolean) {
            settings.value = settings.value.copy(onboardingCompleted = completed)
        }
    }
}
