package com.cailiangzhe.lexidue.domain.repository

import com.cailiangzhe.lexidue.domain.model.DictionaryRefreshResult
import com.cailiangzhe.lexidue.domain.model.SavedDictionaryEnrichment
import kotlinx.coroutines.flow.Flow

interface DictionaryEnrichmentRepository {
    /** Observes the most recently refreshed local entries; this method never performs networking. */
    fun observeSavedEnrichment(limit: Int = MAX_REFRESH_WORDS): Flow<List<SavedDictionaryEnrichment>>

    /** Refreshes only stale due words and keeps existing rows untouched when any lookup fails. */
    suspend fun refreshDueWords(
        nowEpochMillis: Long,
        limit: Int = MAX_REFRESH_WORDS,
    ): DictionaryRefreshResult

    companion object {
        const val MAX_REFRESH_WORDS = 5
        const val STALE_AFTER_MILLIS = 30L * 24L * 60L * 60L * 1_000L
    }
}

fun isDictionaryEnrichmentFresh(
    fetchedAtEpochMillis: Long,
    nowEpochMillis: Long,
): Boolean =
    fetchedAtEpochMillis >= 0L &&
        nowEpochMillis >= fetchedAtEpochMillis &&
        nowEpochMillis - fetchedAtEpochMillis <= DictionaryEnrichmentRepository.STALE_AFTER_MILLIS
