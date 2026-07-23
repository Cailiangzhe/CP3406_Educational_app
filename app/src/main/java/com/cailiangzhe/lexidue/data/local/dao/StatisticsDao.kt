package com.cailiangzhe.lexidue.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.cailiangzhe.lexidue.data.local.query.LearningStatisticsRow
import kotlinx.coroutines.flow.Flow

@Dao
interface StatisticsDao {
    @Query(
        """
        SELECT
            (SELECT COUNT(*) FROM practice_sessions) AS total_sessions,
            (SELECT COUNT(*) FROM practice_sessions WHERE status = 'COMPLETED') AS completed_sessions,
            (SELECT COUNT(*) FROM attempts) AS total_attempts,
            (SELECT COUNT(*) FROM attempts WHERE outcome = 'CORRECT') AS correct_attempts,
            (SELECT COUNT(*) FROM attempts WHERE outcome = 'INCORRECT') AS incorrect_attempts,
            (SELECT COUNT(*) FROM attempts WHERE outcome = 'SKIPPED') AS skipped_attempts,
            (SELECT COUNT(*) FROM review_progress WHERE next_review_at_epoch_millis <= :nowEpochMillis)
                AS due_words,
            (SELECT COUNT(*) FROM review_progress WHERE review_box >= :masteredReviewBox)
                AS mastered_words
        """,
    )
    fun observeLearningStatistics(
        nowEpochMillis: Long,
        masteredReviewBox: Int,
    ): Flow<LearningStatisticsRow>
}
