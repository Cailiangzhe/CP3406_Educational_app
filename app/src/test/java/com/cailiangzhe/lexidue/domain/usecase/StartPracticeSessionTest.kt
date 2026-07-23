package com.cailiangzhe.lexidue.domain.usecase

import com.cailiangzhe.lexidue.domain.model.PracticeDifficulty
import com.cailiangzhe.lexidue.domain.model.QuestionType
import com.cailiangzhe.lexidue.testing.FakeWordRepository
import com.cailiangzhe.lexidue.testing.RecordingPracticeSessionRepository
import com.cailiangzhe.lexidue.testing.SequenceIdProvider
import com.cailiangzhe.lexidue.testing.testWord
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StartPracticeSessionTest {
    @Test
    fun standardSession_selectsDueThenDeck_andStoresAlternatingQuestions() =
        runTest {
            val words = (1..6).map(::testWord)
            val wordRepository =
                FakeWordRepository(
                    words = words,
                    dueWordIds = listOf("en:word3", "en:word1"),
                )
            val sessionRepository = RecordingPracticeSessionRepository()
            val useCase =
                StartPracticeSession(
                    wordRepository = wordRepository,
                    practiceSessionRepository = sessionRepository,
                    timeProvider = TimeProvider { 42_000L },
                    idProvider =
                        SequenceIdProvider(
                            listOf("session-1", "question-1", "question-2", "question-3", "question-4"),
                        ),
                )

            val sessionId =
                useCase(
                    plannedWordCount = 4,
                    difficulty = PracticeDifficulty.STANDARD,
                )

            assertEquals("session-1", sessionId)
            assertEquals(1, wordRepository.importCallCount)
            assertEquals(1, sessionRepository.createSessionCallCount)
            assertEquals(
                listOf("en:word3", "en:word1", "en:word2", "en:word4"),
                sessionRepository.createdQuestions.map { it.wordId },
            )
            assertEquals(
                listOf(
                    QuestionType.WORD_TO_DEFINITION,
                    QuestionType.DEFINITION_TO_WORD,
                    QuestionType.WORD_TO_DEFINITION,
                    QuestionType.DEFINITION_TO_WORD,
                ),
                sessionRepository.createdQuestions.map { it.questionType },
            )
            assertTrue(sessionRepository.createdQuestions.all { it.optionIds.size == 4 })
            assertTrue(
                sessionRepository.createdQuestions.all {
                    it.optionIds.distinct().size == it.optionIds.size &&
                        it.optionIds.count { optionId -> optionId == it.correctOptionId } == 1
                },
            )
            assertEquals("question-1", sessionRepository.createdSession?.currentQuestionId)
            assertEquals(4, sessionRepository.createdSession?.plannedWordCount)
            assertNotEquals(0L, sessionRepository.createdSession?.randomSeed)
        }

    @Test
    fun foundationSession_isDeterministic_andUsesThreeOptions() =
        runTest {
            val first = createFoundationSession()
            val second = createFoundationSession()

            assertEquals(first.createdSession, second.createdSession)
            assertEquals(first.createdQuestions, second.createdQuestions)
            assertTrue(first.createdQuestions.all { it.optionIds.size == 3 })
        }

    private suspend fun createFoundationSession(): RecordingPracticeSessionRepository {
        val words = (1..5).map(::testWord)
        val sessionRepository = RecordingPracticeSessionRepository()
        val useCase =
            StartPracticeSession(
                wordRepository = FakeWordRepository(words),
                practiceSessionRepository = sessionRepository,
                timeProvider = TimeProvider { 7_000L },
                idProvider =
                    SequenceIdProvider(
                        listOf("fixed-session", "fixed-q1", "fixed-q2", "fixed-q3", "fixed-q4"),
                    ),
            )

        useCase(
            plannedWordCount = 4,
            difficulty = PracticeDifficulty.FOUNDATION,
        )
        return sessionRepository
    }
}
