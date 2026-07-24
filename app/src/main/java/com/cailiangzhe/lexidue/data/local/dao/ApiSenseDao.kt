package com.cailiangzhe.lexidue.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.cailiangzhe.lexidue.data.local.entity.ApiSenseEntity
import com.cailiangzhe.lexidue.data.local.relation.WordWithEnrichmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ApiSenseDao {
    @Upsert
    suspend fun upsertAll(senses: List<ApiSenseEntity>)

    @Query("DELETE FROM api_senses WHERE word_id = :wordId")
    suspend fun deleteForWord(wordId: String)

    /** Replaces one word's complete provider snapshot in a single Room transaction. */
    @Transaction
    suspend fun replaceForWord(
        wordId: String,
        senses: List<ApiSenseEntity>,
    ) {
        require(senses.isNotEmpty()) { "A validated replacement cannot be empty" }
        require(senses.all { it.wordId == wordId }) {
            "Every API sense must belong to the word being replaced"
        }
        deleteForWord(wordId)
        upsertAll(senses)
    }

    @Query(
        """
        SELECT * FROM api_senses
        WHERE word_id = :wordId
        ORDER BY part_of_speech, definition, id
        """,
    )
    fun observeForWord(wordId: String): Flow<List<ApiSenseEntity>>

    @Query(
        """
        SELECT * FROM api_senses
        WHERE word_id = :wordId
        ORDER BY part_of_speech, definition, id
        """,
    )
    suspend fun getForWord(wordId: String): List<ApiSenseEntity>

    @Transaction
    @Query("SELECT * FROM words WHERE id = :wordId")
    fun observeWordWithEnrichment(wordId: String): Flow<WordWithEnrichmentEntity?>

    @Transaction
    @Query("SELECT * FROM words ORDER BY normalized_spelling")
    fun observeAllWordsWithEnrichment(): Flow<List<WordWithEnrichmentEntity>>

    @Transaction
    @Query(
        """
        SELECT words.* FROM words
        INNER JOIN api_senses ON api_senses.word_id = words.id
        GROUP BY words.id
        ORDER BY MAX(api_senses.fetched_at_epoch_millis) DESC, words.normalized_spelling
        LIMIT :limit
        """,
    )
    fun observeRecentWordEnrichment(limit: Int): Flow<List<WordWithEnrichmentEntity>>

    @Query(
        """
        SELECT MAX(fetched_at_epoch_millis) FROM api_senses
        WHERE word_id = :wordId
        """,
    )
    suspend fun getLatestFetchedAtEpochMillis(wordId: String): Long?

    @Query("SELECT MAX(fetched_at_epoch_millis) FROM api_senses")
    fun observeLatestFetchedAtEpochMillis(): Flow<Long?>
}
