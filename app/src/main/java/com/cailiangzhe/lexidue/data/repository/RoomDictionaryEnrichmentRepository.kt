package com.cailiangzhe.lexidue.data.repository

import com.cailiangzhe.lexidue.data.local.dao.ApiSenseDao
import com.cailiangzhe.lexidue.data.local.entity.ApiSenseEntity
import com.cailiangzhe.lexidue.data.local.relation.WordWithEnrichmentEntity
import com.cailiangzhe.lexidue.data.remote.DictionaryLookupResult
import com.cailiangzhe.lexidue.data.remote.DictionaryRemoteDataSource
import com.cailiangzhe.lexidue.domain.model.ApiSense
import com.cailiangzhe.lexidue.domain.model.DictionaryRefreshFailure
import com.cailiangzhe.lexidue.domain.model.DictionaryRefreshFailureReason
import com.cailiangzhe.lexidue.domain.model.DictionaryRefreshResult
import com.cailiangzhe.lexidue.domain.model.SavedDictionaryEnrichment
import com.cailiangzhe.lexidue.domain.model.Word
import com.cailiangzhe.lexidue.domain.repository.DictionaryEnrichmentRepository
import com.cailiangzhe.lexidue.domain.repository.WordRepository
import com.cailiangzhe.lexidue.domain.repository.isDictionaryEnrichmentFresh
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomDictionaryEnrichmentRepository
    @Inject
    constructor(
        private val wordRepository: WordRepository,
        private val apiSenseDao: ApiSenseDao,
        private val remoteDataSource: DictionaryRemoteDataSource,
    ) : DictionaryEnrichmentRepository {
        private val refreshMutex = Mutex()
        private var nextCandidateOffset = 0

        override fun observeSavedEnrichment(limit: Int): Flow<List<SavedDictionaryEnrichment>> {
            requireRefreshLimit(limit)
            return apiSenseDao.observeRecentWordEnrichment(limit).map { rows ->
                rows.map(WordWithEnrichmentEntity::toSavedEnrichment)
            }
        }

        override suspend fun refreshDueWords(
            nowEpochMillis: Long,
            limit: Int,
        ): DictionaryRefreshResult {
            require(nowEpochMillis >= 0L) { "Refresh time cannot be negative." }
            requireRefreshLimit(limit)
            return refreshMutex.withLock {
                refreshDueWordsLocked(nowEpochMillis, limit)
            }
        }

        private suspend fun refreshDueWordsLocked(
            nowEpochMillis: Long,
            limit: Int,
        ): DictionaryRefreshResult {
            val dueWords =
                wordRepository
                    .getDueWords(nowEpochMillis, MAX_LOCAL_CANDIDATES)
                    .distinctBy(Word::id)
                    .take(MAX_LOCAL_CANDIDATES)
            val startOffset =
                if (dueWords.isEmpty()) 0 else nextCandidateOffset % dueWords.size
            val orderedCandidates = dueWords.drop(startOffset) + dueWords.take(startOffset)
            var refreshedWordCount = 0
            var freshWordCount = 0
            var networkRequestCount = 0
            var inspectedWordCount = 0
            val failures = mutableListOf<DictionaryRefreshFailure>()

            for (word in orderedCandidates) {
                if (networkRequestCount >= limit) break
                inspectedWordCount += 1
                val latestFetchedAt = apiSenseDao.getLatestFetchedAtEpochMillis(word.id)
                if (
                    latestFetchedAt != null &&
                    isDictionaryEnrichmentFresh(
                        fetchedAtEpochMillis = latestFetchedAt,
                        nowEpochMillis = nowEpochMillis,
                    )
                ) {
                    freshWordCount += 1
                    continue
                }

                networkRequestCount += 1
                val lookup =
                    try {
                        remoteDataSource.lookup(word.normalizedSpelling)
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (_: Exception) {
                        DictionaryLookupResult.NetworkFailure
                    }
                when (lookup) {
                    is DictionaryLookupResult.Success -> {
                        val enrichment = lookup.enrichment
                        if (enrichment.normalizedWord != word.normalizedSpelling) {
                            failures += word.failure(DictionaryRefreshFailureReason.UNUSABLE_CONTENT)
                        } else {
                            apiSenseDao.replaceForWord(
                                wordId = word.id,
                                senses =
                                    enrichment.senses.map { sense ->
                                        sense.toEntity(
                                            wordId = word.id,
                                            fetchedAtEpochMillis = nowEpochMillis,
                                        )
                                    },
                            )
                            refreshedWordCount += 1
                        }
                    }

                    DictionaryLookupResult.InvalidRequest -> {
                        failures += word.failure(DictionaryRefreshFailureReason.INVALID_REQUEST)
                    }

                    DictionaryLookupResult.NotFound -> {
                        failures += word.failure(DictionaryRefreshFailureReason.NOT_FOUND)
                    }

                    DictionaryLookupResult.MalformedPayload -> {
                        failures += word.failure(DictionaryRefreshFailureReason.MALFORMED_PAYLOAD)
                    }

                    DictionaryLookupResult.Timeout -> {
                        failures += word.failure(DictionaryRefreshFailureReason.TIMEOUT)
                    }

                    DictionaryLookupResult.NetworkFailure -> {
                        failures += word.failure(DictionaryRefreshFailureReason.NETWORK)
                    }

                    is DictionaryLookupResult.HttpFailure -> {
                        failures += word.failure(DictionaryRefreshFailureReason.HTTP)
                    }

                    DictionaryLookupResult.UnusableContent -> {
                        failures += word.failure(DictionaryRefreshFailureReason.UNUSABLE_CONTENT)
                    }
                }
            }
            if (dueWords.isNotEmpty()) {
                nextCandidateOffset = (startOffset + inspectedWordCount) % dueWords.size
            }

            return DictionaryRefreshResult(
                selectedWordCount = refreshedWordCount + freshWordCount + failures.size,
                refreshedWordCount = refreshedWordCount,
                freshWordCount = freshWordCount,
                failures = failures,
            )
        }

        private fun requireRefreshLimit(limit: Int) {
            require(limit in 1..DictionaryEnrichmentRepository.MAX_REFRESH_WORDS) {
                "Dictionary refresh limit must be between 1 and ${DictionaryEnrichmentRepository.MAX_REFRESH_WORDS}."
            }
        }

        private companion object {
            const val MAX_LOCAL_CANDIDATES = 500
        }
    }

private fun WordWithEnrichmentEntity.toSavedEnrichment(): SavedDictionaryEnrichment {
    check(apiSenses.isNotEmpty()) { "A saved enrichment relation must contain an API sense." }
    return SavedDictionaryEnrichment(
        wordId = word.id,
        displayWord = word.displaySpelling,
        senses =
            apiSenses
                .sortedWith(compareBy(ApiSenseEntity::partOfSpeech, ApiSenseEntity::definition, ApiSenseEntity::id))
                .map { entity -> entity.toDomain(word.normalizedSpelling) },
        fetchedAtEpochMillis = apiSenses.maxOf(ApiSenseEntity::fetchedAtEpochMillis),
    )
}

private fun ApiSenseEntity.toDomain(normalizedWord: String): ApiSense =
    ApiSense(
        stableId = id,
        normalizedWord = normalizedWord,
        partOfSpeech = partOfSpeech,
        definition = definition,
        example = example,
        phonetic = phonetic,
        audioUrl = audioUrl,
        provider = provider,
        source = source,
    )

private fun ApiSense.toEntity(
    wordId: String,
    fetchedAtEpochMillis: Long,
): ApiSenseEntity =
    ApiSenseEntity(
        id = stableId,
        wordId = wordId,
        partOfSpeech = partOfSpeech,
        definition = definition,
        example = example,
        phonetic = phonetic,
        audioUrl = audioUrl,
        provider = provider,
        source = source,
        fetchedAtEpochMillis = fetchedAtEpochMillis,
    )

private fun Word.failure(reason: DictionaryRefreshFailureReason): DictionaryRefreshFailure =
    DictionaryRefreshFailure(
        displayWord = displaySpelling,
        reason = reason,
    )
