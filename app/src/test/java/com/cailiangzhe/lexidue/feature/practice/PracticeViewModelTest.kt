package com.cailiangzhe.lexidue.feature.practice

import androidx.lifecycle.SavedStateHandle
import com.cailiangzhe.lexidue.domain.model.Attempt
import com.cailiangzhe.lexidue.domain.model.AttemptOutcome
import com.cailiangzhe.lexidue.domain.model.CanonicalMeaning
import com.cailiangzhe.lexidue.domain.model.PartOfSpeech
import com.cailiangzhe.lexidue.domain.model.PracticeDifficulty
import com.cailiangzhe.lexidue.domain.model.PracticeSession
import com.cailiangzhe.lexidue.domain.model.QuestionType
import com.cailiangzhe.lexidue.domain.model.RecordAttemptResult
import com.cailiangzhe.lexidue.domain.model.SessionQuestion
import com.cailiangzhe.lexidue.domain.model.SessionQuestionState
import com.cailiangzhe.lexidue.domain.model.SessionSnapshot
import com.cailiangzhe.lexidue.domain.model.SessionStatus
import com.cailiangzhe.lexidue.domain.model.StarterDeckImportResult
import com.cailiangzhe.lexidue.domain.model.Word
import com.cailiangzhe.lexidue.domain.repository.PracticeSessionRepository
import com.cailiangzhe.lexidue.domain.repository.WordRepository
import com.cailiangzhe.lexidue.domain.usecase.TimeProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class PracticeViewModelTest {
    @get:Rule
    val mainDispatcherRule = PracticeVmMainDispatcherRule()

    private val deck = testDeck()

    @Test
    fun savedUnansweredQuestionIsReconstructedFromSessionAndDeck() =
        runTest {
            val repository = FakePracticeSessionRepository(snapshot(questionCount = 1))
            val viewModel = viewModel(repository)

            val content =
                viewModel.uiState.first { it.content is PracticeContent.Question }.content
                    as PracticeContent.Question

            assertEquals("q1", content.question.id)
            assertEquals(1, content.question.questionNumber)
            assertEquals(1, content.question.totalQuestions)
            assertEquals("analyse", content.question.prompt)
            assertEquals(PracticePromptMode.WORD_TO_DEFINITION, content.question.mode)
            assertEquals(
                listOf(
                    PracticeChoice(ANALYSE_ID, "examine in detail"),
                    PracticeChoice(ASSESS_ID, "judge the quality"),
                ),
                content.question.choices,
            )
            assertTrue(content.answersEnabled)
        }

    @Test
    fun incorrectAnswerPersistsFeedbackAndNeedsTwoLaterQuestionsForRetry() =
        runTest {
            val eligibleRepository = FakePracticeSessionRepository(snapshot(questionCount = 3))
            val eligibleViewModel = viewModel(eligibleRepository)
            eligibleViewModel.uiState.first { it.content is PracticeContent.Question }

            eligibleViewModel.onAction(
                PracticeUiAction.SelectAnswer(questionId = "q1", choiceId = ASSESS_ID),
            )

            val feedback =
                eligibleViewModel.uiState.first { it.content is PracticeContent.Feedback }.content
                    as PracticeContent.Feedback
            val recorded = eligibleRepository.recordAttemptCalls.single()
            assertEquals(PracticeFeedbackKind.INCORRECT, feedback.feedback.kind)
            assertEquals(ASSESS_ID, feedback.selectedChoiceId)
            assertEquals(AttemptOutcome.INCORRECT, recorded.attempt.outcome)
            assertEquals(NOW, recorded.attempt.answeredAtEpochMillis)
            assertNotNull(recorded.retryQuestion)
            assertEquals("q1:retry", recorded.retryQuestion?.id)
            assertEquals("q1", recorded.retryQuestion?.retryOfQuestionId)
            assertEquals(4, recorded.retryQuestion?.sequence)

            val sparseRepository = FakePracticeSessionRepository(snapshot(questionCount = 2))
            val sparseViewModel = viewModel(sparseRepository)
            sparseViewModel.uiState.first { it.content is PracticeContent.Question }

            sparseViewModel.onAction(
                PracticeUiAction.SelectAnswer(questionId = "q1", choiceId = ASSESS_ID),
            )
            sparseViewModel.uiState.first { it.content is PracticeContent.Feedback }

            assertNull(sparseRepository.recordAttemptCalls.single().retryQuestion)
        }

    @Test
    fun continueAdvancesFromPersistedFeedbackToNextUnansweredQuestion() =
        runTest {
            val initial =
                snapshot(
                    questionCount = 2,
                    attempts = mapOf("q1" to attempt("q1", AttemptOutcome.CORRECT, ANALYSE_ID)),
                )
            val repository = FakePracticeSessionRepository(initial)
            val viewModel = viewModel(repository)
            viewModel.uiState.first { it.content is PracticeContent.Feedback }

            viewModel.onAction(PracticeUiAction.Continue("q1"))

            val content =
                viewModel.uiState
                    .first {
                        (it.content as? PracticeContent.Question)?.question?.id == "q2"
                    }.content as PracticeContent.Question
            assertEquals("q2", content.question.id)
            assertEquals("q2", repository.advanceCalls.single().nextQuestionId)
            assertEquals("q2", repository.currentSnapshot()?.session?.currentQuestionId)
        }

    @Test
    fun continueStorageFailureShowsRecoverableErrorAndRetryRestoresFeedback() =
        runTest {
            val initial =
                snapshot(
                    questionCount = 1,
                    attempts = mapOf("q1" to attempt("q1", AttemptOutcome.CORRECT, ANALYSE_ID)),
                )
            val repository = FakePracticeSessionRepository(initial)
            val viewModel = viewModel(repository)
            viewModel.uiState.first { it.content is PracticeContent.Feedback }
            repository.advanceFailure = RuntimeException("Storage unavailable")

            viewModel.onAction(PracticeUiAction.Continue("q1"))

            val error = viewModel.uiState.first { it.content is PracticeContent.Error }.content as PracticeContent.Error
            assertEquals("Storage unavailable", error.message)

            repository.advanceFailure = null
            viewModel.onAction(PracticeUiAction.RetryLoad)
            val restored = viewModel.uiState.first { it.content is PracticeContent.Feedback }
            assertTrue(restored.content is PracticeContent.Feedback)
        }

    @Test
    fun continuingFinalQuestionEmitsOpenSummaryExactlyOnce() =
        runTest {
            val initial =
                snapshot(
                    questionCount = 1,
                    attempts = mapOf("q1" to attempt("q1", AttemptOutcome.CORRECT, ANALYSE_ID)),
                )
            val repository = FakePracticeSessionRepository(initial)
            val viewModel = viewModel(repository)
            val effects = mutableListOf<PracticeEffect>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.effects.collect { effects += it }
            }
            viewModel.uiState.first { it.content is PracticeContent.Feedback }

            viewModel.onAction(PracticeUiAction.Continue("q1"))
            advanceUntilIdle()

            assertEquals(listOf(PracticeEffect.OpenSummary(SESSION_ID)), effects)
            assertEquals(SessionStatus.COMPLETED, repository.currentSnapshot()?.session?.status)
            assertNull(repository.advanceCalls.single().nextQuestionId)
        }

    @Test
    fun persistedGradedAttemptReconstructsFeedbackAfterRecreation() =
        runTest {
            val initial =
                snapshot(
                    questionCount = 1,
                    attempts = mapOf("q1" to attempt("q1", AttemptOutcome.CORRECT, ANALYSE_ID)),
                )

            val content =
                viewModel(FakePracticeSessionRepository(initial))
                    .uiState
                    .first { it.content is PracticeContent.Feedback }
                    .content
                    as PracticeContent.Feedback

            assertEquals(PracticeFeedbackKind.CORRECT, content.feedback.kind)
            assertEquals(ANALYSE_ID, content.selectedChoiceId)
            assertEquals(ANALYSE_ID, content.feedback.correctChoiceId)
            assertEquals("analyse — examine in detail", content.feedback.message)
        }

    @Test
    fun exitConfirmationCanBeDismissedAndConfirmedReturningHomeOnce() =
        runTest {
            val repository = FakePracticeSessionRepository(snapshot(questionCount = 1))
            val viewModel = viewModel(repository)
            val effects = mutableListOf<PracticeEffect>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.effects.collect { effects += it }
            }
            viewModel.uiState.first { it.content is PracticeContent.Question }

            viewModel.onAction(PracticeUiAction.RequestExit)
            assertTrue(viewModel.uiState.first { it.showExitConfirmation }.showExitConfirmation)

            viewModel.onAction(PracticeUiAction.DismissExit)
            assertFalse(viewModel.uiState.first { !it.showExitConfirmation }.showExitConfirmation)
            assertTrue(repository.updateStatusCalls.isEmpty())

            viewModel.onAction(PracticeUiAction.RequestExit)
            viewModel.uiState.first { it.showExitConfirmation }
            viewModel.onAction(PracticeUiAction.ConfirmExit)
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.showExitConfirmation)
            assertEquals(
                listOf(StatusUpdate(SessionStatus.ABANDONED, NOW)),
                repository.updateStatusCalls,
            )
            assertEquals(listOf(PracticeEffect.ReturnHome), effects)
        }

    private fun viewModel(repository: FakePracticeSessionRepository): PracticeViewModel =
        PracticeViewModel(
            savedStateHandle = SavedStateHandle(mapOf("sessionId" to SESSION_ID)),
            sessionRepository = repository,
            wordRepository = FakePracticeWordRepository(deck),
            timeProvider = TimeProvider { NOW },
        )

    private fun snapshot(
        questionCount: Int,
        attempts: Map<String, Attempt> = emptyMap(),
    ): SessionSnapshot {
        val questions =
            (1..questionCount).map { index ->
                val question = question(index)
                SessionQuestionState(question, attempts[question.id])
            }
        return SessionSnapshot(
            session =
                PracticeSession(
                    id = SESSION_ID,
                    difficulty = PracticeDifficulty.STANDARD,
                    randomSeed = 42,
                    plannedWordCount = questionCount,
                    status = SessionStatus.ACTIVE,
                    correctCount = attempts.values.count { it.outcome == AttemptOutcome.CORRECT },
                    currentQuestionId = "q1",
                    startedAtEpochMillis = 1_000,
                    endedAtEpochMillis = null,
                ),
            questions = questions,
        )
    }

    private fun question(index: Int): SessionQuestion {
        val wordId =
            if (index == 1) {
                ANALYSE_ID
            } else if (index == 2) {
                ASSESS_ID
            } else {
                DERIVE_ID
            }
        val otherId = if (wordId == ANALYSE_ID) ASSESS_ID else ANALYSE_ID
        return SessionQuestion(
            id = "q$index",
            sessionId = SESSION_ID,
            sequence = index,
            wordId = wordId,
            questionType = QuestionType.WORD_TO_DEFINITION,
            prompt = deck.first { it.id == wordId }.displaySpelling,
            optionIds = listOf(wordId, otherId),
            correctOptionId = wordId,
        )
    }

    private fun attempt(
        questionId: String,
        outcome: AttemptOutcome,
        selectedOptionId: String?,
    ): Attempt =
        Attempt(
            id = "attempt-$questionId",
            questionId = questionId,
            sessionId = SESSION_ID,
            wordId = ANALYSE_ID,
            selectedOptionId = selectedOptionId,
            outcome = outcome,
            isRetry = false,
            answeredAtEpochMillis = NOW - 1,
        )

    private companion object {
        const val SESSION_ID = "session-1"
        const val ANALYSE_ID = "en:analyse"
        const val ASSESS_ID = "en:assess"
        const val DERIVE_ID = "en:derive"
        const val NOW = 50_000L
    }
}

private data class RecordedAttemptCall(
    val attempt: Attempt,
    val retryQuestion: SessionQuestion?,
)

private data class AdvanceCall(
    val expectedCurrentQuestionId: String,
    val nextQuestionId: String?,
    val completedAtEpochMillis: Long,
)

private data class StatusUpdate(
    val status: SessionStatus,
    val endedAtEpochMillis: Long?,
)

private class FakePracticeSessionRepository(
    initialSnapshot: SessionSnapshot?,
) : PracticeSessionRepository {
    private val snapshots = MutableStateFlow(initialSnapshot)

    val recordAttemptCalls = mutableListOf<RecordedAttemptCall>()
    val advanceCalls = mutableListOf<AdvanceCall>()
    val updateStatusCalls = mutableListOf<StatusUpdate>()
    var advanceFailure: Throwable? = null

    fun currentSnapshot(): SessionSnapshot? = snapshots.value

    override suspend fun createSession(
        session: PracticeSession,
        questions: List<SessionQuestion>,
    ) = error("Not used")

    override suspend fun getSession(sessionId: String): SessionSnapshot? = snapshots.value

    override fun observeSession(sessionId: String): Flow<SessionSnapshot?> = snapshots

    override suspend fun recordAttempt(
        attempt: Attempt,
        retryQuestion: SessionQuestion?,
    ): RecordAttemptResult {
        recordAttemptCalls += RecordedAttemptCall(attempt, retryQuestion)
        val current = requireNotNull(snapshots.value)
        val existing = current.questions.first { it.question.id == attempt.questionId }.attempt
        if (existing != null) return RecordAttemptResult.AlreadyRecorded(existing)

        val updatedQuestions =
            current.questions
                .map { state ->
                    if (state.question.id == attempt.questionId) state.copy(attempt = attempt) else state
                }.toMutableList()
        retryQuestion?.let { updatedQuestions += SessionQuestionState(it, attempt = null) }
        snapshots.value =
            current.copy(
                session =
                    current.session.copy(
                        correctCount =
                            current.session.correctCount +
                                if (attempt.outcome == AttemptOutcome.CORRECT) 1 else 0,
                    ),
                questions = updatedQuestions,
            )
        return RecordAttemptResult.Recorded(attempt, progress = null)
    }

    override suspend fun advanceFrom(
        sessionId: String,
        expectedCurrentQuestionId: String,
        nextQuestionId: String?,
        completedAtEpochMillis: Long,
    ): Boolean {
        advanceFailure?.let { throw it }
        advanceCalls += AdvanceCall(expectedCurrentQuestionId, nextQuestionId, completedAtEpochMillis)
        val current = snapshots.value ?: return false
        if (
            current.session.status != SessionStatus.ACTIVE ||
            current.session.currentQuestionId != expectedCurrentQuestionId
        ) {
            return false
        }
        snapshots.value =
            current.copy(
                session =
                    current.session.copy(
                        status = if (nextQuestionId == null) SessionStatus.COMPLETED else SessionStatus.ACTIVE,
                        currentQuestionId = nextQuestionId,
                        endedAtEpochMillis = if (nextQuestionId == null) completedAtEpochMillis else null,
                    ),
            )
        return true
    }

    override suspend fun updateStatus(
        sessionId: String,
        status: SessionStatus,
        endedAtEpochMillis: Long?,
    ): Boolean {
        updateStatusCalls += StatusUpdate(status, endedAtEpochMillis)
        val current = snapshots.value ?: return false
        if (current.session.status != SessionStatus.ACTIVE) return false
        snapshots.value =
            current.copy(
                session =
                    current.session.copy(
                        status = status,
                        currentQuestionId = null,
                        endedAtEpochMillis = endedAtEpochMillis,
                    ),
            )
        return true
    }

    override fun observeRecentSessions(limit: Int): Flow<List<PracticeSession>> = emptyFlow()
}

private class FakePracticeWordRepository(
    words: List<Word>,
) : WordRepository {
    private val deck = MutableStateFlow(words)
    private val wordsById = words.associateBy(Word::id)

    override fun observeDeck(): Flow<List<Word>> = deck

    override fun observeDueWords(
        nowEpochMillis: Long,
        limit: Int,
    ): Flow<List<Word>> = emptyFlow()

    override suspend fun getDueWords(
        nowEpochMillis: Long,
        limit: Int,
    ): List<Word> = emptyList()

    override suspend fun getWordsByIds(ids: List<String>): List<Word> = ids.mapNotNull(wordsById::get)

    override suspend fun importStarterDeck(importedAtEpochMillis: Long): StarterDeckImportResult = error("Not used")
}

private fun testDeck(): List<Word> =
    listOf(
        testWord("en:analyse", "analyse", "examine in detail"),
        testWord("en:assess", "assess", "judge the quality"),
        testWord("en:derive", "derive", "obtain from a source"),
    )

private fun testWord(
    id: String,
    spelling: String,
    definition: String,
): Word =
    Word(
        id = id,
        normalizedSpelling = spelling,
        displaySpelling = spelling,
        deckId = "test",
        sourceName = "test",
        canonicalMeanings =
            listOf(
                CanonicalMeaning(
                    id = "$id:meaning",
                    wordId = id,
                    partOfSpeech = PartOfSpeech.VERB,
                    definition = definition,
                    provenance = "test",
                ),
            ),
    )

@OptIn(ExperimentalCoroutinesApi::class)
class PracticeVmMainDispatcherRule(
    private val dispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
