package com.cailiangzhe.lexidue.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.cailiangzhe.lexidue.data.local.entity.CanonicalMeaningEntity
import com.cailiangzhe.lexidue.data.local.entity.WordEntity
import com.cailiangzhe.lexidue.data.local.relation.WordWithMeaningsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertWords(words: List<WordEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMeanings(meanings: List<CanonicalMeaningEntity>): List<Long>

    @Transaction
    @Query("SELECT * FROM words ORDER BY normalized_spelling")
    fun observeAllWords(): Flow<List<WordWithMeaningsEntity>>

    @Transaction
    @Query("SELECT * FROM words WHERE id IN (:ids) ORDER BY normalized_spelling")
    suspend fun getWordsByIds(ids: List<String>): List<WordWithMeaningsEntity>

    @Transaction
    @Query(
        """
        SELECT words.* FROM words
        LEFT JOIN review_progress ON review_progress.word_id = words.id
        WHERE review_progress.word_id IS NULL
            OR review_progress.next_review_at_epoch_millis <= :nowEpochMillis
        ORDER BY COALESCE(review_progress.next_review_at_epoch_millis, 0), words.normalized_spelling
        LIMIT :limit
        """,
    )
    fun observeDueWords(
        nowEpochMillis: Long,
        limit: Int,
    ): Flow<List<WordWithMeaningsEntity>>

    @Transaction
    @Query(
        """
        SELECT words.* FROM words
        LEFT JOIN review_progress ON review_progress.word_id = words.id
        WHERE review_progress.word_id IS NULL
            OR review_progress.next_review_at_epoch_millis <= :nowEpochMillis
        ORDER BY COALESCE(review_progress.next_review_at_epoch_millis, 0), words.normalized_spelling
        LIMIT :limit
        """,
    )
    suspend fun getDueWords(
        nowEpochMillis: Long,
        limit: Int,
    ): List<WordWithMeaningsEntity>

    @Query("SELECT COUNT(*) FROM words")
    suspend fun countWords(): Int
}
