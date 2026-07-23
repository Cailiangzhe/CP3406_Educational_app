package com.cailiangzhe.lexidue.feature.practice

import androidx.lifecycle.SavedStateHandle
import com.cailiangzhe.lexidue.domain.model.Attempt
import com.cailiangzhe.lexidue.domain.model.AttemptOutcome
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class PracticeSummaryViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun completedSession_derivesCountsAccuracyRetriesAndReviewSpellings() =
        runTest {
            val sessionRepository = FakeSessionRepository(completedSnapshot(SessionStatus.COMPLETED))
            val viewModel =
                PracticeSummaryViewModel(
                    savedStateHandle = SavedStateHandle(mapOf("sessionId" to SESSION_ID)),
                    sessionRepository = sessionRepository,
                    wordRepository = FakeWordRepository(listOf(testWord(ANALYSE_ID, "analyse"))),
                )

            val state = viewModel.uiState.first { !it.isLoading }

            assertNull(state.errorMessage)
            assertEquals(SESSION_ID, state.sessionId)
            assertEquals(3, state.plannedWordCount)
            assertEquals(2, state.correctCount)
            assertEquals(1, state.incorrectCount)
            assertEquals(1, state.skippedCount)
            assertEquals(1, state.retryCount)
            assertEquals(67, state.accuracyPercent)
            assertEquals(listOf("analyse"), state.reviewWords)
            assertNotNull(state.completedAtLabel)
        }

    @Test
    fun abandonedSession_isStillPresentedAsASummary() =
        runTest {
            val viewModel =
                PracticeSummaryViewModel(
                    savedStateHandle = SavedStateHandle(mapOf("sessionId" to SESSION_ID)),
                    sessionRepository = FakeSessionRepository(completedSnapshot(SessionStatus.ABANDONED)),
                    wordRepository = FakeWordRepository(listOf(testWord(ANALYSE_ID, "analyse"))),
                )

            val state = viewModel.uiState.first { !it.isLoading }

            assertNull(state.errorMessage)
            assertFalse(state.isLoading)
            assertEquals(2, state.correctCount)
            assertEquals(listOf("analyse"), state.reviewWords)
        }

    @Test
    fun missingSession_producesClearErrorState() =
        runTest {
            val viewModel =
                PracticeSummaryViewModel(
                    savedStateHandle = SavedStateHandle(mapOf("sessionId" to SESSION_ID)),
                    sessionRepository = FakeSessionRepository(null),
                    wordRepository = FakeWordRepository(emptyList()),
                )

            val state = viewModel.uiState.first { !it.isLoading }

            assertEquals(SESSION_ID, state.sessionId)
            assertEquals("This practice session could not be found.", state.errorMessage)
        }

    private fun completedSnapshot(status: SessionStatus): SessionSnapshot {
        val questions =
            listOf(
                questionState("q1", ANALYSE_ID, AttemptOutcome.INCORRECT),
                questionState("q2", ASSESS_ID, AttemptOutcome.SKIPPED),
                questionState("q3", ANALYSE_ID, AttemptOutcome.CORRECT, isRetry = true),
                questionState("q4", CONCEPT_ID, AttemptOutcome.CORRECT),
            )
        return SessionSnapshot(
            session =
                PracticeSession(
                    id = SESSION_ID,
                    difficulty = PracticeDifficulty.STANDARD,
                    randomSeed = 42,
                    plannedWordCount = 3,
                    status = status,
                    correctCount = 2,
                    currentQuestionId = null,
                    startedAtEpochMillis = 1_000,
                    endedAtEpochMillis = 2_000,
                ),
            questions = questions,
        )
    }

    private fun questionState(
        questionId: String,
        wordId: String,
        outcome: AttemptOutcome,
        isRetry: Boolean = false,
    ): SessionQuestionState {
        val correctOptionId = "correct-$questionId"
        val selectedOptionId =
            when (outcome) {
                AttemptOutcome.CORRECT -> correctOptionId
                AttemptOutcome.INCORRECT -> "wrong-$questionId"
                AttemptOutcome.SKIPPED -> null
            }
        return SessionQuestionState(
            question =
                SessionQuestion(
                    id = questionId,
                    sessionId = SESSION_ID,
                    sequence = questionId.removePrefix("q").toInt(),
                    wordId = wordId,
                    questionType = QuestionType.WORD_TO_DEFINITION,
                    prompt = wordId,
                    optionIds = listOf(correctOptionId, "wrong-$questionId"),
                    correctOptionId = correctOptionId,
                    retryOfQuestionId = if (isRetry) "q1" else null,
                ),
            attempt =
                Attempt(
                    id = "attempt-$questionId",
                    questionId = questionId,
                    sessionId = SESSION_ID,
                    wordId = wordId,
                    selectedOptionId = selectedOptionId,
                    outcome = outcome,
                    isRetry = isRetry,
                    answeredAtEpochMillis = 1_500,
                ),
        )
    }

    private fun testWord(
        id: String,
        spelling: String,
    ): Word =
        Word(
            id = id,
            normalizedSpelling = spelling,
            displaySpelling = spelling,
            deckId = "test",
            sourceName = "test",
            canonicalMeanings = emptyList(),
        )

    private companion object {
        const val SESSION_ID = "session-1"
        const val ANALYSE_ID = "en:analyse"
        const val ASSESS_ID = "en:assess"
        const val CONCEPT_ID = "en:concept"
    }
}

private class FakeSessionRepository(
    initialSnapshot: SessionSnapshot?,
) : PracticeSessionRepository {
    private val snapshots = MutableStateFlow(initialSnapshot)

    override suspend fun createSession(
        session: PracticeSession,
        questions: List<SessionQuestion>,
    ) = error("Not used")

    override suspend fun getSession(sessionId: String): SessionSnapshot? = snapshots.value

    override fun observeSession(sessionId: String): Flow<SessionSnapshot?> = snapshots

    override suspend fun recordAttempt(
        attempt: Attempt,
        retryQuestion: SessionQuestion?,
    ): RecordAttemptResult = error("Not used")

    override suspend fun advanceFrom(
        sessionId: String,
        expectedCurrentQuestionId: String,
        nextQuestionId: String?,
        completedAtEpochMillis: Long,
    ): Boolean = error("Not used")

    override suspend fun updateStatus(
        sessionId: String,
        status: SessionStatus,
        endedAtEpochMillis: Long?,
    ): Boolean = error("Not used")

    override fun observeRecentSessions(limit: Int): Flow<List<PracticeSession>> = emptyFlow()
}

private class FakeWordRepository(
    words: List<Word>,
) : WordRepository {
    private val wordsById = words.associateBy(Word::id)

    override fun observeDeck(): Flow<List<Word>> = emptyFlow()

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

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val dispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
