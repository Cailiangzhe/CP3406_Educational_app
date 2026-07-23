package com.cailiangzhe.lexidue.domain.usecase

import com.cailiangzhe.lexidue.domain.model.AnswerOutcome
import com.cailiangzhe.lexidue.domain.model.ReviewScheduleState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

class UpdateReviewScheduleTest {
    private val update = UpdateReviewSchedule(TimeProvider { NOW })

    @Test
    fun correctAnswersAdvanceThroughOneThreeSevenFourteenThirtyDayIntervals() {
        var progress = ReviewScheduleState(cardId = "card")

        UpdateReviewSchedule.REVIEW_INTERVAL_DAYS.forEachIndexed { index, intervalDays ->
            progress = update(progress, AnswerOutcome.CORRECT)
            assertEquals(index + 1, progress.reviewBox)
            assertEquals(NOW + intervalDays * MILLIS_PER_DAY, progress.nextReviewAtEpochMillis)
            assertEquals(index + 1, progress.correctCount)
            assertEquals(0, progress.incorrectCount)
        }

        progress = update(progress, AnswerOutcome.CORRECT)
        assertEquals(ReviewScheduleState.MAX_REVIEW_BOX, progress.reviewBox)
        assertEquals(NOW + 30 * MILLIS_PER_DAY, progress.nextReviewAtEpochMillis)
        assertEquals(6, progress.correctCount)
    }

    @Test
    fun incorrectAnswerResetsMasteredCardToFirstInterval() {
        val mastered =
            ReviewScheduleState(
                cardId = "card",
                reviewBox = 5,
                correctCount = 5,
                nextReviewAtEpochMillis = NOW + 30 * MILLIS_PER_DAY,
            )

        val updated = update(mastered, AnswerOutcome.INCORRECT)

        assertEquals(1, updated.reviewBox)
        assertEquals(5, updated.correctCount)
        assertEquals(1, updated.incorrectCount)
        assertEquals(NOW + MILLIS_PER_DAY, updated.nextReviewAtEpochMillis)
    }

    @Test
    fun skipAndExitDoNotChangeProgressOrReadTime() {
        var timeReads = 0
        val schedule =
            UpdateReviewSchedule(
                TimeProvider {
                    timeReads += 1
                    NOW
                },
            )
        val progress = ReviewScheduleState(cardId = "card", reviewBox = 3)

        assertSame(progress, schedule(progress, AnswerOutcome.SKIPPED))
        assertSame(progress, schedule(progress, AnswerOutcome.EXITED))
        assertEquals(0, timeReads)
    }

    @Test
    fun dueDateSaturatesAtMaximumLongInsteadOfOverflowing() {
        val schedule = UpdateReviewSchedule(TimeProvider { Long.MAX_VALUE - 1 })

        val updated = schedule(ReviewScheduleState("card"), AnswerOutcome.CORRECT)

        assertEquals(Long.MAX_VALUE, updated.nextReviewAtEpochMillis)
    }

    @Test
    fun invalidReviewBoxIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            ReviewScheduleState(cardId = "card", reviewBox = 6)
        }
    }

    companion object {
        private const val NOW = 2_000_000L
        private const val MILLIS_PER_DAY = 86_400_000L
    }
}
