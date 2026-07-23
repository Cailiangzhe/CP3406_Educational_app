package com.cailiangzhe.lexidue.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cailiangzhe.lexidue.data.repository.RoomPracticeSessionRepository
import com.cailiangzhe.lexidue.data.repository.RoomReviewProgressRepository
import com.cailiangzhe.lexidue.data.repository.RoomWordRepository
import com.cailiangzhe.lexidue.domain.model.Attempt
import com.cailiangzhe.lexidue.domain.model.AttemptOutcome
import com.cailiangzhe.lexidue.domain.model.PracticeDifficulty
import com.cailiangzhe.lexidue.domain.model.PracticeSession
import com.cailiangzhe.lexidue.domain.model.QuestionType
import com.cailiangzhe.lexidue.domain.model.RecordAttemptResult
import com.cailiangzhe.lexidue.domain.model.SessionQuestion
import com.cailiangzhe.lexidue.domain.model.SessionStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomDataLayerTest {
    private lateinit var database: LexiDueDatabase
    private lateinit var wordRepository: RoomWordRepository
    private lateinit var progressRepository: RoomReviewProgressRepository
    private lateinit var sessionRepository: RoomPracticeSessionRepository

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room
                .inMemoryDatabaseBuilder(context, LexiDueDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        wordRepository = RoomWordRepository(database.wordDao(), StarterDeckImporter(database))
        progressRepository = RoomReviewProgressRepository(database.reviewProgressDao())
        sessionRepository =
            RoomPracticeSessionRepository(
                database = database,
                sessionDao = database.sessionDao(),
                learningTransactionDao = database.learningTransactionDao(),
            )
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun starterDeckImport_isAtomicAndIdempotent() =
        runTest {
            val firstImport = wordRepository.importStarterDeck(IMPORTED_AT)
            val secondImport = wordRepository.importStarterDeck(IMPORTED_AT + 1)

            assertEquals(24, firstImport.insertedWords)
            assertEquals(24, firstImport.insertedMeanings)
            assertEquals(24, firstImport.insertedProgressRows)
            assertEquals(0, secondImport.insertedWords)
            assertEquals(0, secondImport.insertedMeanings)
            assertEquals(0, secondImport.insertedProgressRows)
            assertEquals(24, wordRepository.observeDeck().first().size)
            assertEquals(5, wordRepository.getDueWords(IMPORTED_AT, limit = 5).size)
        }

    @Test
    fun recordAttempt_isIdempotentAndSessionReconstructs() =
        runTest {
            wordRepository.importStarterDeck(IMPORTED_AT)
            val session = testSession()
            val question = testQuestion()
            sessionRepository.createSession(session, listOf(question))
            val attempt = testCorrectAttempt()

            val firstResult = sessionRepository.recordAttempt(attempt)
            val repeatedResult = sessionRepository.recordAttempt(attempt)

            assertTrue(firstResult is RecordAttemptResult.Recorded)
            assertTrue(repeatedResult is RecordAttemptResult.AlreadyRecorded)
            val progress = progressRepository.getProgress(WORD_ID)
            assertEquals(1, progress?.reviewBox)
            assertEquals(1, progress?.correctCount)
            assertEquals(ANSWERED_AT + MILLIS_PER_DAY, progress?.nextReviewAtEpochMillis)

            val activeSnapshot = sessionRepository.getSession(SESSION_ID)
            assertEquals(QUESTION_ID, activeSnapshot?.session?.currentQuestionId)
            assertEquals(1, activeSnapshot?.session?.correctCount)
            assertEquals(attempt, activeSnapshot?.questions?.single()?.attempt)

            assertTrue(
                sessionRepository.advanceFrom(
                    sessionId = SESSION_ID,
                    expectedCurrentQuestionId = QUESTION_ID,
                    nextQuestionId = null,
                    completedAtEpochMillis = COMPLETED_AT,
                ),
            )
            assertTrue(
                !sessionRepository.advanceFrom(
                    sessionId = SESSION_ID,
                    expectedCurrentQuestionId = QUESTION_ID,
                    nextQuestionId = null,
                    completedAtEpochMillis = COMPLETED_AT,
                ),
            )
            val completedSnapshot = sessionRepository.getSession(SESSION_ID)
            assertEquals(SessionStatus.COMPLETED, completedSnapshot?.session?.status)
            assertNull(completedSnapshot?.session?.currentQuestionId)
            assertEquals(COMPLETED_AT, completedSnapshot?.session?.endedAtEpochMillis)
        }

    private fun testSession(): PracticeSession =
        PracticeSession(
            id = SESSION_ID,
            difficulty = PracticeDifficulty.STANDARD,
            randomSeed = 42,
            plannedWordCount = 1,
            status = SessionStatus.ACTIVE,
            correctCount = 0,
            currentQuestionId = QUESTION_ID,
            startedAtEpochMillis = IMPORTED_AT,
            endedAtEpochMillis = null,
        )

    private fun testQuestion(): SessionQuestion =
        SessionQuestion(
            id = QUESTION_ID,
            sessionId = SESSION_ID,
            sequence = 0,
            wordId = WORD_ID,
            questionType = QuestionType.WORD_TO_DEFINITION,
            prompt = "analyse",
            optionIds = listOf(CORRECT_OPTION_ID, "canonical:assess:1"),
            correctOptionId = CORRECT_OPTION_ID,
        )

    private fun testCorrectAttempt(): Attempt =
        Attempt(
            id = ATTEMPT_ID,
            questionId = QUESTION_ID,
            sessionId = SESSION_ID,
            wordId = WORD_ID,
            selectedOptionId = CORRECT_OPTION_ID,
            outcome = AttemptOutcome.CORRECT,
            isRetry = false,
            answeredAtEpochMillis = ANSWERED_AT,
        )

    private companion object {
        const val IMPORTED_AT = 1_000L
        const val ANSWERED_AT = 10_000L
        const val COMPLETED_AT = 20_000L
        const val MILLIS_PER_DAY = 86_400_000L
        const val SESSION_ID = "session-1"
        const val QUESTION_ID = "question-1"
        const val ATTEMPT_ID = "attempt-1"
        const val WORD_ID = "en:analyse"
        const val CORRECT_OPTION_ID = "canonical:analyse:1"
    }
}
