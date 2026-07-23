package com.cailiangzhe.lexidue.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.cailiangzhe.lexidue.data.local.entity.ReviewProgressEntity
import com.cailiangzhe.lexidue.data.local.query.ReviewBoxCountRow
import kotlinx.coroutines.flow.Flow

@Dao
interface ReviewProgressDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(progress: List<ReviewProgressEntity>): List<Long>

    @Upsert
    suspend fun upsert(progress: ReviewProgressEntity)

    @Query("SELECT * FROM review_progress WHERE word_id = :wordId")
    suspend fun get(wordId: String): ReviewProgressEntity?

    @Query("SELECT * FROM review_progress WHERE word_id = :wordId")
    fun observe(wordId: String): Flow<ReviewProgressEntity?>

    @Query(
        """
        SELECT review_box, COUNT(*) AS word_count
        FROM review_progress
        GROUP BY review_box
        ORDER BY review_box
        """,
    )
    fun observeReviewBoxDistribution(): Flow<List<ReviewBoxCountRow>>

    @Query("SELECT COUNT(*) FROM review_progress WHERE next_review_at_epoch_millis <= :nowEpochMillis")
    fun observeDueCount(nowEpochMillis: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM review_progress WHERE review_box >= :masteredReviewBox")
    fun observeMasteredCount(masteredReviewBox: Int): Flow<Int>
}
