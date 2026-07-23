package com.cailiangzhe.lexidue.domain.usecase

import com.cailiangzhe.lexidue.domain.model.AnswerEvaluation
import com.cailiangzhe.lexidue.domain.model.AnswerOutcome
import com.cailiangzhe.lexidue.domain.model.QuestionMode
import com.cailiangzhe.lexidue.domain.model.QuizOption
import com.cailiangzhe.lexidue.domain.model.QuizQuestion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class DelayedRetryQueueTest {
    @Test
    fun retryIsReleasedOnlyAfterTwoInterveningQuestions() {
        val queue = DelayedRetryQueue()
        val first = retryQuestion("first")

        queue.recordQuestionPresented(first)
        assertTrue(queue.scheduleRetry(first, retryEvaluation(first, AnswerOutcome.INCORRECT)))
        assertNull(queue.pollEligibleRetry())

        queue.recordQuestionPresented(retryQuestion("second"))
        assertNull(queue.pollEligibleRetry())

        queue.recordQuestionPresented(retryQuestion("third"))
        assertEquals(first, queue.pollEligibleRetry())
        assertEquals(0, queue.pendingCount)
    }

    @Test
    fun duplicatePendingRetry_isNotAddedTwice() {
        val queue = DelayedRetryQueue()
        val question = retryQuestion("one")
        queue.recordQuestionPresented(question)

        val incorrect = retryEvaluation(question, AnswerOutcome.INCORRECT)
        assertTrue(queue.scheduleRetry(question, incorrect))
        assertFalse(queue.scheduleRetry(question, incorrect))
        assertEquals(1, queue.pendingCount)
    }

    @Test
    fun retriesRemainFifoWhenSeveralBecomeEligible() {
        val queue = DelayedRetryQueue()
        val first = retryQuestion("first")
        val second = retryQuestion("second")

        queue.recordQuestionPresented(first)
        queue.scheduleRetry(first, retryEvaluation(first, AnswerOutcome.INCORRECT))
        queue.recordQuestionPresented(second)
        queue.scheduleRetry(second, retryEvaluation(second, AnswerOutcome.INCORRECT))
        queue.recordQuestionPresented(retryQuestion("third"))
        queue.recordQuestionPresented(retryQuestion("fourth"))

        assertEquals(first, queue.pollEligibleRetry())
        assertEquals(second, queue.pollEligibleRetry())
    }

    @Test
    fun questionMustBePresentedBeforeScheduling() {
        val queue = DelayedRetryQueue()

        assertThrows(IllegalArgumentException::class.java) {
            val unseen = retryQuestion("unseen")
            queue.scheduleRetry(unseen, retryEvaluation(unseen, AnswerOutcome.INCORRECT))
        }
    }

    @Test
    fun correctSkipAndExitNeverScheduleARetry() {
        val queue = DelayedRetryQueue()
        val question = retryQuestion("one")
        queue.recordQuestionPresented(question)

        assertFalse(queue.scheduleRetry(question, retryEvaluation(question, AnswerOutcome.CORRECT)))
        assertFalse(queue.scheduleRetry(question, retryEvaluation(question, AnswerOutcome.SKIPPED)))
        assertFalse(queue.scheduleRetry(question, retryEvaluation(question, AnswerOutcome.EXITED)))
        assertEquals(0, queue.pendingCount)
    }

    @Test
    fun configuredDelayCannotUndercutTwoQuestionMinimum() {
        assertThrows(IllegalArgumentException::class.java) {
            DelayedRetryQueue(minimumInterveningQuestions = 1)
        }
    }
}

private fun retryQuestion(id: String): QuizQuestion =
    QuizQuestion(
        id = id,
        sourceCardId = "card-$id",
        mode = QuestionMode.WORD_TO_MEANING,
        prompt = "prompt $id",
        partOfSpeech = "noun",
        options = listOf(QuizOption("$id-correct", "correct $id"), QuizOption("$id-other", "other $id")),
        correctOptionId = "$id-correct",
    )

private fun retryEvaluation(
    question: QuizQuestion,
    outcome: AnswerOutcome,
): AnswerEvaluation =
    AnswerEvaluation(
        questionId = question.id,
        cardId = question.sourceCardId,
        outcome = outcome,
        selectedOptionId = if (outcome.isGraded) question.correctOptionId else null,
    )
