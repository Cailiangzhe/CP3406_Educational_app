package com.cailiangzhe.lexidue.domain.usecase

import com.cailiangzhe.lexidue.domain.model.AnswerOutcome
import com.cailiangzhe.lexidue.domain.model.ReviewScheduleState

/** Applies the transparent 1/3/7/14/30-day schedule to graded answers only. */
class UpdateReviewSchedule(
    private val timeProvider: TimeProvider,
) {
    operator fun invoke(
        progress: ReviewScheduleState,
        outcome: AnswerOutcome,
    ): ReviewScheduleState {
        if (!outcome.isGraded) return progress

        val nextBox =
            when (outcome) {
                AnswerOutcome.CORRECT -> {
                    (progress.reviewBox + 1).coerceAtMost(ReviewScheduleState.MAX_REVIEW_BOX)
                }

                AnswerOutcome.INCORRECT -> {
                    FIRST_REVIEW_BOX
                }

                AnswerOutcome.SKIPPED,
                AnswerOutcome.EXITED,
                -> {
                    error("Unscored outcomes returned before scheduling.")
                }
            }
        val intervalDays = REVIEW_INTERVAL_DAYS[nextBox - 1]

        return progress.copy(
            reviewBox = nextBox,
            correctCount = progress.correctCount + if (outcome == AnswerOutcome.CORRECT) 1 else 0,
            incorrectCount =
                progress.incorrectCount + if (outcome == AnswerOutcome.INCORRECT) 1 else 0,
            nextReviewAtEpochMillis = addDaysSaturated(timeProvider.nowEpochMillis(), intervalDays),
        )
    }

    private fun addDaysSaturated(
        epochMillis: Long,
        days: Int,
    ): Long {
        val durationMillis = days * MILLIS_PER_DAY
        return if (epochMillis > Long.MAX_VALUE - durationMillis) {
            Long.MAX_VALUE
        } else {
            epochMillis + durationMillis
        }
    }

    companion object {
        val REVIEW_INTERVAL_DAYS: List<Int> = listOf(1, 3, 7, 14, 30)
        private const val FIRST_REVIEW_BOX = 1
        private const val MILLIS_PER_DAY = 86_400_000L
    }
}
