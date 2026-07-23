package com.cailiangzhe.lexidue.feature.home

import com.cailiangzhe.lexidue.domain.model.LearningStatistics
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
                    startPracticeSession = startPracticeSession,
                    timeProvider = TimeProvider { 5_000L },
                )

            advanceUntilIdle()
            assertFalse(viewModel.uiState.value.isLoading)
            assertEquals(8, viewModel.uiState.value.dueWordCount)
            assertEquals(2, viewModel.uiState.value.masteredWordCount)

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
        }
}
