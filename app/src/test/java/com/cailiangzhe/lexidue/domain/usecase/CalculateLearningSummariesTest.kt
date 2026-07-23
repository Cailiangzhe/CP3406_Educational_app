package com.cailiangzhe.lexidue.domain.usecase

import com.cailiangzhe.lexidue.domain.model.AnswerEvaluation
import com.cailiangzhe.lexidue.domain.model.AnswerOutcome
import com.cailiangzhe.lexidue.domain.model.ReviewScheduleState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculateLearningSummariesTest {
    @Test
    fun sessionSummaryExcludesSkipAndExitFromAccuracy() {
        val evaluations =
            listOf(
                evaluation("a", AnswerOutcome.CORRECT),
                evaluation("b", AnswerOutcome.INCORRECT),
                evaluation("a", AnswerOutcome.CORRECT, isRetry = true),
                evaluation("c", AnswerOutcome.SKIPPED),
                evaluation("d", AnswerOutcome.EXITED, isRetry = true),
            )

        val summary = CalculateSessionSummary()(evaluations)

        assertEquals(5, summary.actionCount)
        assertEquals(3, summary.gradedAnswerCount)
        assertEquals(2, summary.correctCount)
        assertEquals(1, summary.incorrectCount)
        assertEquals(1, summary.skippedCount)
        assertEquals(1, summary.retryAnswerCount)
        assertTrue(summary.exitedEarly)
        assertEquals(2.0 / 3.0, summary.accuracy, 0.000_001)
    }

    @Test
    fun sessionWithOnlyUnscoredActionsHasZeroAccuracy() {
        val summary =
            CalculateSessionSummary()(
                listOf(
                    evaluation("a", AnswerOutcome.SKIPPED),
                    evaluation("b", AnswerOutcome.EXITED),
                ),
            )

        assertEquals(0, summary.gradedAnswerCount)
        assertEquals(0.0, summary.accuracy, 0.0)
        assertTrue(summary.exitedEarly)
    }

    @Test
    fun masteryUsesReviewBoxFiveAndInjectedCurrentTime() {
        val cardIds = linkedSetOf("a", "b", "c", "d", "e", "f")
        val progress =
            listOf(
                ReviewScheduleState("a", reviewBox = 5, nextReviewAtEpochMillis = NOW),
                ReviewScheduleState("b", reviewBox = 4, nextReviewAtEpochMillis = NOW + 1),
                ReviewScheduleState("c", reviewBox = 5, nextReviewAtEpochMillis = null),
                ReviewScheduleState("outside", reviewBox = 5, nextReviewAtEpochMillis = NOW - 1),
            )

        val snapshot =
            CalculateMasterySnapshot(TimeProvider { NOW })(
                cardIds = cardIds,
                progress = progress,
                evaluations = reviewEvaluations(),
            )

        assertEquals(setOf("a", "c"), snapshot.masteredCardIds)
        assertEquals(setOf("a"), snapshot.dueCardIds)
        assertEquals(setOf("b", "d"), snapshot.needsReviewCardIds)
        assertEquals(2, snapshot.masteredCardCount)
        assertEquals(1.0 / 3.0, snapshot.masteryRatio, 0.000_001)
    }

    @Test
    fun skippedActionsDoNotHideLatestIncorrectGradedAnswer() {
        val snapshot =
            CalculateMasterySnapshot(TimeProvider { NOW })(
                cardIds = setOf("card"),
                progress = emptyList(),
                evaluations =
                    listOf(
                        evaluation("card", AnswerOutcome.INCORRECT),
                        evaluation("card", AnswerOutcome.SKIPPED),
                    ),
            )

        assertEquals(setOf("card"), snapshot.needsReviewCardIds)
    }

    @Test
    fun emptyDeckHasAStableZeroMasteryRatio() {
        val snapshot =
            CalculateMasterySnapshot(TimeProvider { NOW })(
                cardIds = emptySet(),
                progress = emptyList(),
                evaluations = emptyList(),
            )

        assertEquals(0, snapshot.totalCardCount)
        assertEquals(0.0, snapshot.masteryRatio, 0.0)
        assertTrue(snapshot.masteredCardIds.isEmpty())
        assertFalse(snapshot.dueCardIds.isNotEmpty())
    }

    private fun reviewEvaluations(): List<AnswerEvaluation> =
        buildList {
            add(evaluation("b", AnswerOutcome.INCORRECT))

            add(evaluation("c", AnswerOutcome.CORRECT))
            add(evaluation("c", AnswerOutcome.INCORRECT))
            add(evaluation("c", AnswerOutcome.CORRECT))

            add(evaluation("d", AnswerOutcome.CORRECT))
            add(evaluation("d", AnswerOutcome.INCORRECT))
            add(evaluation("d", AnswerOutcome.INCORRECT))
            add(evaluation("d", AnswerOutcome.INCORRECT))
            add(evaluation("d", AnswerOutcome.CORRECT))

            add(evaluation("e", AnswerOutcome.CORRECT))
            add(evaluation("e", AnswerOutcome.CORRECT))
            add(evaluation("e", AnswerOutcome.CORRECT))
            add(evaluation("e", AnswerOutcome.INCORRECT))
            add(evaluation("e", AnswerOutcome.CORRECT))

            add(evaluation("f", AnswerOutcome.CORRECT))
            add(evaluation("f", AnswerOutcome.CORRECT))
            add(evaluation("f", AnswerOutcome.INCORRECT))
            add(evaluation("f", AnswerOutcome.INCORRECT))
            add(evaluation("f", AnswerOutcome.CORRECT))
        }

    private fun evaluation(
        cardId: String,
        outcome: AnswerOutcome,
        isRetry: Boolean = false,
    ): AnswerEvaluation =
        AnswerEvaluation(
            questionId = "$cardId-${nextQuestionNumber++}",
            cardId = cardId,
            outcome = outcome,
            selectedOptionId = if (outcome.isGraded) "selected" else null,
            isRetry = isRetry,
        )

    private var nextQuestionNumber = 0

    companion object {
        private const val NOW = 10_000L
    }
}
