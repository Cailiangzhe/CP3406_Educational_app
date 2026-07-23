package com.cailiangzhe.lexidue.domain.usecase

import com.cailiangzhe.lexidue.domain.model.AnswerEvaluation
import com.cailiangzhe.lexidue.domain.model.AnswerOutcome
import com.cailiangzhe.lexidue.domain.model.LearningMasterySnapshot
import com.cailiangzhe.lexidue.domain.model.LearningSessionSummary
import com.cailiangzhe.lexidue.domain.model.ReviewScheduleState

class CalculateSessionSummary {
    operator fun invoke(evaluations: List<AnswerEvaluation>): LearningSessionSummary {
        val correctCount = evaluations.count { it.outcome == AnswerOutcome.CORRECT }
        val incorrectCount = evaluations.count { it.outcome == AnswerOutcome.INCORRECT }
        val gradedCount = correctCount + incorrectCount

        return LearningSessionSummary(
            actionCount = evaluations.size,
            gradedAnswerCount = gradedCount,
            correctCount = correctCount,
            incorrectCount = incorrectCount,
            skippedCount = evaluations.count { it.outcome == AnswerOutcome.SKIPPED },
            retryAnswerCount =
                evaluations.count { it.isRetry && it.outcome != AnswerOutcome.EXITED },
            exitedEarly = evaluations.any { it.outcome == AnswerOutcome.EXITED },
            accuracy = if (gradedCount == 0) 0.0 else correctCount.toDouble() / gradedCount,
        )
    }
}

/** Calculates the objective mastery definitions used by Home and Statistics. */
class CalculateMasterySnapshot(
    private val timeProvider: TimeProvider,
) {
    operator fun invoke(
        cardIds: Set<String>,
        progress: Collection<ReviewScheduleState>,
        evaluations: List<AnswerEvaluation>,
    ): LearningMasterySnapshot {
        require(cardIds.none(String::isBlank)) { "Learning card identifiers cannot be blank." }

        val progressByCard = progress.associateBy(ReviewScheduleState::cardId)
        val now = timeProvider.nowEpochMillis()
        val masteredCardIds =
            cardIds.filterTo(linkedSetOf()) { progressByCard[it]?.isMastered == true }
        val dueCardIds =
            cardIds.filterTo(linkedSetOf()) { cardId ->
                progressByCard[cardId]
                    ?.nextReviewAtEpochMillis
                    ?.let { it <= now } == true
            }
        val needsReviewCardIds =
            cardIds.filterTo(linkedSetOf()) { cardId ->
                val graded =
                    evaluations.filter {
                        it.cardId == cardId && it.outcome.isGraded
                    }
                val latestWasIncorrect = graded.lastOrNull()?.outcome == AnswerOutcome.INCORRECT
                val belowAccuracyThreshold =
                    graded.size >= MINIMUM_GRADED_ATTEMPTS_FOR_ACCURACY &&
                        graded.count { it.outcome == AnswerOutcome.CORRECT }.toDouble() /
                        graded.size < NEEDS_REVIEW_ACCURACY_THRESHOLD
                latestWasIncorrect || belowAccuracyThreshold
            }

        return LearningMasterySnapshot(
            totalCardCount = cardIds.size,
            masteredCardIds = masteredCardIds,
            dueCardIds = dueCardIds,
            needsReviewCardIds = needsReviewCardIds,
        )
    }

    companion object {
        private const val MINIMUM_GRADED_ATTEMPTS_FOR_ACCURACY = 2
        private const val NEEDS_REVIEW_ACCURACY_THRESHOLD = 0.60
    }
}
