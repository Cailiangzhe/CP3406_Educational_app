package com.cailiangzhe.lexidue.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.cailiangzhe.lexidue.data.local.entity.AttemptEntity
import com.cailiangzhe.lexidue.data.local.entity.PracticeSessionEntity
import com.cailiangzhe.lexidue.data.local.entity.ReviewProgressEntity
import com.cailiangzhe.lexidue.data.local.entity.SessionQuestionEntity
import com.cailiangzhe.lexidue.domain.model.AttemptOutcome
import com.cailiangzhe.lexidue.domain.model.ReviewProgress
import com.cailiangzhe.lexidue.domain.model.SessionStatus
import kotlin.math.max
import kotlin.math.min

sealed interface RecordAttemptTransactionResult {
    data class Recorded(
        val attempt: AttemptEntity,
        val progress: ReviewProgressEntity?,
    ) : RecordAttemptTransactionResult

    data class AlreadyRecorded(
        val attempt: AttemptEntity,
    ) : RecordAttemptTransactionResult
}

/**
 * Owns the answer write boundary. The unique question index plus this transaction makes answer
 * recording safe against double taps and recomposition-driven retries.
 */
@Dao
abstract class LearningTransactionDao {
    @Query("SELECT * FROM attempts WHERE question_id = :questionId LIMIT 1")
    protected abstract suspend fun findAttemptForQuestion(questionId: String): AttemptEntity?

    @Query("SELECT * FROM session_questions WHERE id = :questionId")
    protected abstract suspend fun findQuestion(questionId: String): SessionQuestionEntity?

    @Query("SELECT * FROM practice_sessions WHERE id = :sessionId")
    protected abstract suspend fun findSession(sessionId: String): PracticeSessionEntity?

    @Query("SELECT * FROM review_progress WHERE word_id = :wordId")
    protected abstract suspend fun findProgress(wordId: String): ReviewProgressEntity?

    @Query(
        """
        SELECT COUNT(*) FROM session_questions
        WHERE session_id = :sessionId
            AND sequence > :afterSequence
            AND sequence < :beforeSequence
        """,
    )
    protected abstract suspend fun countQuestionsBetween(
        sessionId: String,
        afterSequence: Int,
        beforeSequence: Int,
    ): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertAttempt(attempt: AttemptEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun insertRetryQuestion(question: SessionQuestionEntity)

    @Upsert
    protected abstract suspend fun upsertProgress(progress: ReviewProgressEntity)

    @Query("UPDATE practice_sessions SET correct_count = correct_count + 1 WHERE id = :sessionId")
    protected abstract suspend fun incrementCorrectCount(sessionId: String): Int

    @Transaction
    open suspend fun recordAttempt(
        attempt: AttemptEntity,
        retryQuestion: SessionQuestionEntity? = null,
    ): RecordAttemptTransactionResult {
        findAttemptForQuestion(attempt.questionId)?.let {
            return RecordAttemptTransactionResult.AlreadyRecorded(it)
        }

        val question =
            requireNotNull(findQuestion(attempt.questionId)) {
                "Question ${attempt.questionId} does not exist"
            }
        require(question.sessionId == attempt.sessionId) { "Attempt session does not match its question" }
        require(question.wordId == attempt.wordId) { "Attempt word does not match its question" }
        require(attempt.isRetry == (question.retryOfQuestionId != null)) {
            "Attempt retry flag does not match its question"
        }
        validateOutcome(attempt, question)

        val session =
            requireNotNull(findSession(attempt.sessionId)) {
                "Session ${attempt.sessionId} does not exist"
            }
        check(session.status == SessionStatus.ACTIVE) { "Only active sessions accept answers" }
        check(session.currentQuestionId == attempt.questionId) {
            "Only the session's current question can be answered"
        }

        if (retryQuestion != null) {
            validateRetryQuestion(retryQuestion, question, attempt)
            insertRetryQuestion(retryQuestion)
        }

        val rowId = insertAttempt(attempt)
        check(rowId != INSERT_CONFLICT) { "Attempt id ${attempt.id} is already in use" }

        val updatedProgress = updateProgressFor(attempt)
        if (updatedProgress != null) {
            upsertProgress(updatedProgress)
        }
        if (attempt.outcome == AttemptOutcome.CORRECT) {
            check(incrementCorrectCount(attempt.sessionId) == 1) { "Session disappeared while recording answer" }
        }

        return RecordAttemptTransactionResult.Recorded(attempt, updatedProgress)
    }

    private suspend fun validateRetryQuestion(
        retryQuestion: SessionQuestionEntity,
        originalQuestion: SessionQuestionEntity,
        attempt: AttemptEntity,
    ) {
        require(attempt.outcome == AttemptOutcome.INCORRECT) {
            "Only an incorrect answer can schedule a retry"
        }
        require(retryQuestion.sessionId == originalQuestion.sessionId) {
            "Retry question must belong to the same session"
        }
        require(retryQuestion.wordId == originalQuestion.wordId) {
            "Retry question must practise the same word"
        }
        require(retryQuestion.retryOfQuestionId == originalQuestion.id) {
            "Retry question must reference the original question"
        }
        require(retryQuestion.sequence >= originalQuestion.sequence + MINIMUM_RETRY_SEQUENCE_GAP) {
            "A retry must be separated by at least two other questions"
        }
        require(
            countQuestionsBetween(
                sessionId = originalQuestion.sessionId,
                afterSequence = originalQuestion.sequence,
                beforeSequence = retryQuestion.sequence,
            ) >= MINIMUM_QUESTIONS_BEFORE_RETRY,
        ) { "A retry must follow at least two stored question instances" }
        validateQuestionOptions(retryQuestion)
    }

    private fun validateQuestionOptions(question: SessionQuestionEntity) {
        require(question.optionIds.size >= MINIMUM_OPTION_COUNT) { "A question needs at least two options" }
        require(question.optionIds.distinct().size == question.optionIds.size) {
            "Question options must be unique"
        }
        require(question.correctOptionId in question.optionIds) {
            "The correct option must be present"
        }
    }

    private suspend fun updateProgressFor(attempt: AttemptEntity): ReviewProgressEntity? {
        if (attempt.outcome == AttemptOutcome.SKIPPED) return null

        val previous =
            findProgress(attempt.wordId)
                ?: ReviewProgressEntity(
                    wordId = attempt.wordId,
                    reviewBox = ReviewProgress.NEW_REVIEW_BOX,
                    correctCount = 0,
                    incorrectCount = 0,
                    nextReviewAtEpochMillis = 0,
                    updatedAtEpochMillis = 0,
                )
        val nextBox =
            when (attempt.outcome) {
                AttemptOutcome.CORRECT -> {
                    min(
                        max(previous.reviewBox, ReviewProgress.NEW_REVIEW_BOX) + 1,
                        ReviewProgress.MASTERED_REVIEW_BOX,
                    )
                }

                AttemptOutcome.INCORRECT -> {
                    FIRST_REVIEW_BOX
                }

                AttemptOutcome.SKIPPED -> {
                    error("Skipped attempts do not update review progress")
                }
            }
        val nextReviewAt =
            addDaysSaturated(
                epochMillis = attempt.answeredAtEpochMillis,
                days = REVIEW_INTERVAL_DAYS[nextBox].orEmptyDays(),
            )

        return previous.copy(
            reviewBox = nextBox,
            correctCount = previous.correctCount + if (attempt.outcome == AttemptOutcome.CORRECT) 1 else 0,
            incorrectCount = previous.incorrectCount + if (attempt.outcome == AttemptOutcome.INCORRECT) 1 else 0,
            nextReviewAtEpochMillis = nextReviewAt,
            updatedAtEpochMillis = attempt.answeredAtEpochMillis,
        )
    }

    private fun validateOutcome(
        attempt: AttemptEntity,
        question: SessionQuestionEntity,
    ) {
        require(attempt.selectedOptionId == null || attempt.selectedOptionId in question.optionIds) {
            "Selected option is not part of the stored question"
        }
        when (attempt.outcome) {
            AttemptOutcome.CORRECT -> {
                require(attempt.selectedOptionId == question.correctOptionId) {
                    "A correct attempt must select the correct option"
                }
            }

            AttemptOutcome.INCORRECT -> {
                require(
                    attempt.selectedOptionId != null &&
                        attempt.selectedOptionId != question.correctOptionId,
                ) { "An incorrect attempt must select a wrong option" }
            }

            AttemptOutcome.SKIPPED -> {
                require(attempt.selectedOptionId == null) { "A skipped attempt cannot select an option" }
            }
        }
    }

    private fun Long?.orEmptyDays(): Long = this ?: error("Missing interval for review box")

    private fun addDaysSaturated(
        epochMillis: Long,
        days: Long,
    ): Long {
        val durationMillis = days * MILLIS_PER_DAY
        return if (epochMillis > Long.MAX_VALUE - durationMillis) {
            Long.MAX_VALUE
        } else {
            epochMillis + durationMillis
        }
    }

    private companion object {
        const val INSERT_CONFLICT = -1L
        const val FIRST_REVIEW_BOX = 1
        const val MINIMUM_OPTION_COUNT = 2
        const val MINIMUM_QUESTIONS_BEFORE_RETRY = 2
        const val MINIMUM_RETRY_SEQUENCE_GAP = 3
        const val MILLIS_PER_DAY = 86_400_000L
        val REVIEW_INTERVAL_DAYS = mapOf(1 to 1L, 2 to 3L, 3 to 7L, 4 to 14L, 5 to 30L)
    }
}
