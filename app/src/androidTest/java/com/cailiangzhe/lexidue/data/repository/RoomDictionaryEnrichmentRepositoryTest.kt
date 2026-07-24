package com.cailiangzhe.lexidue.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cailiangzhe.lexidue.data.local.LexiDueDatabase
import com.cailiangzhe.lexidue.data.local.entity.ApiSenseEntity
import com.cailiangzhe.lexidue.data.local.entity.WordEntity
import com.cailiangzhe.lexidue.data.remote.DictionaryLookupResult
import com.cailiangzhe.lexidue.data.remote.DictionaryRemoteDataSource
import com.cailiangzhe.lexidue.domain.model.ApiSense
import com.cailiangzhe.lexidue.domain.model.DictionaryEnrichment
import com.cailiangzhe.lexidue.domain.model.DictionaryRefreshFailureReason
import com.cailiangzhe.lexidue.domain.model.FREE_DICTIONARY_API_PROVIDER
import com.cailiangzhe.lexidue.domain.model.FREE_DICTIONARY_API_SOURCE
import com.cailiangzhe.lexidue.domain.model.PartOfSpeech
import com.cailiangzhe.lexidue.domain.model.StarterDeckImportResult
import com.cailiangzhe.lexidue.domain.model.Word
import com.cailiangzhe.lexidue.domain.repository.DictionaryEnrichmentRepository
import com.cailiangzhe.lexidue.domain.repository.WordRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomDictionaryEnrichmentRepositoryTest {
    private lateinit var context: Context
    private lateinit var database: LexiDueDatabase

    @Before
    fun createDatabase() {
        context = ApplicationProvider.getApplicationContext()
        database =
            Room
                .inMemoryDatabaseBuilder(context, LexiDueDatabase::class.java)
                .allowMainThreadQueries()
                .build()
    }

    @After
    fun closeDatabase() {
        database.close()
        context.deleteDatabase(RESTART_DATABASE)
    }

    @Test
    fun refreshDueWords_capsAnOverReturningWordSourceAtFive() =
        runTest {
            val words = seedWords(count = 7)
            val wordRepository = FakeWordRepository(words)
            val remote =
                FakeDictionaryRemoteDataSource(
                    responses = words.associate { it.normalizedSpelling to successFor(it, "initial") },
                )
            val repository = repository(wordRepository, remote)

            val result = repository.refreshDueWords(nowEpochMillis = NOW)

            assertEquals(DictionaryEnrichmentRepository.MAX_REFRESH_WORDS, result.selectedWordCount)
            assertEquals(DictionaryEnrichmentRepository.MAX_REFRESH_WORDS, result.refreshedWordCount)
            assertEquals(0, result.freshWordCount)
            assertTrue(result.failures.isEmpty())
            assertTrue(
                wordRepository.requestedLimits.single() >
                    DictionaryEnrichmentRepository.MAX_REFRESH_WORDS,
            )
            assertEquals(words.take(5).map(Word::normalizedSpelling), remote.requests)
            words.take(5).forEach { word ->
                assertEquals(1, database.apiSenseDao().getForWord(word.id).size)
            }
            words.drop(5).forEach { word ->
                assertTrue(database.apiSenseDao().getForWord(word.id).isEmpty())
            }
        }

    @Test
    fun freshOrFailedLeadingWords_doNotStarveLaterDueWords() =
        runTest {
            val words = seedWords(count = 7)
            words.take(5).forEach { word ->
                cache(word, fetchedAtEpochMillis = NOW, version = "fresh")
            }
            val remote =
                FakeDictionaryRemoteDataSource(
                    responses =
                        words.drop(5).associate { word ->
                            word.normalizedSpelling to successFor(word, "later")
                        },
                )
            val repository = repository(FakeWordRepository(words), remote)

            val freshResult = repository.refreshDueWords(nowEpochMillis = NOW)

            assertEquals(words.drop(5).map(Word::normalizedSpelling), remote.requests)
            assertEquals(5, freshResult.freshWordCount)
            assertEquals(2, freshResult.refreshedWordCount)

            database.apiSenseDao().deleteForWord(words[0].id)
            words.drop(1).forEach { word -> database.apiSenseDao().deleteForWord(word.id) }
            remote.responses.clear()
            remote.requests.clear()

            val firstFailedPage = repository.refreshDueWords(nowEpochMillis = NOW)
            assertEquals(5, firstFailedPage.failures.size)
            remote.requests.clear()
            remote.responses +=
                words.drop(5).associate { word ->
                    word.normalizedSpelling to successFor(word, "rotated")
                }

            repository.refreshDueWords(nowEpochMillis = NOW)

            assertTrue(remote.requests.containsAll(words.drop(5).map(Word::normalizedSpelling)))
            words.drop(5).forEach { word ->
                assertTrue(database.apiSenseDao().getForWord(word.id).isNotEmpty())
            }
        }

    @Test
    fun freshness_includesExactlyThirtyDays_andExpiresOneMillisecondLater() =
        runTest {
            val words = seedWords(count = 2)
            val exactlyThirtyDaysOld = words[0]
            val olderByOneMillisecond = words[1]
            val freshnessBoundary = NOW - DictionaryEnrichmentRepository.STALE_AFTER_MILLIS
            cache(exactlyThirtyDaysOld, fetchedAtEpochMillis = freshnessBoundary, version = "boundary")
            cache(
                olderByOneMillisecond,
                fetchedAtEpochMillis = freshnessBoundary - 1L,
                version = "expired",
            )
            val boundaryCache = database.apiSenseDao().getForWord(exactlyThirtyDaysOld.id)
            val remote =
                FakeDictionaryRemoteDataSource(
                    responses =
                        mapOf(
                            olderByOneMillisecond.normalizedSpelling to
                                successFor(olderByOneMillisecond, "refreshed"),
                        ),
                )
            val repository = repository(FakeWordRepository(words), remote)

            val result = repository.refreshDueWords(nowEpochMillis = NOW)

            assertEquals(2, result.selectedWordCount)
            assertEquals(1, result.freshWordCount)
            assertEquals(1, result.refreshedWordCount)
            assertTrue(result.failures.isEmpty())
            assertEquals(listOf(olderByOneMillisecond.normalizedSpelling), remote.requests)
            assertEquals(boundaryCache, database.apiSenseDao().getForWord(exactlyThirtyDaysOld.id))
            assertEquals(
                NOW,
                database
                    .apiSenseDao()
                    .getForWord(olderByOneMillisecond.id)
                    .single()
                    .fetchedAtEpochMillis,
            )
        }

    @Test
    fun partialAndTotalFailures_leaveTheLastSavedSnapshotsUntouched() =
        runTest {
            val words = seedWords(count = 2)
            val refreshedWord = words[0]
            val failedWord = words[1]
            words.forEach { word -> cache(word, fetchedAtEpochMillis = 0L, version = "old") }
            val failedWordCache = database.apiSenseDao().getForWord(failedWord.id)
            val remote =
                FakeDictionaryRemoteDataSource(
                    responses =
                        mapOf(
                            refreshedWord.normalizedSpelling to successFor(refreshedWord, "partial"),
                            failedWord.normalizedSpelling to DictionaryLookupResult.Timeout,
                        ),
                )
            val repository = repository(FakeWordRepository(words), remote)

            val partialResult = repository.refreshDueWords(nowEpochMillis = NOW)

            assertEquals(1, partialResult.refreshedWordCount)
            assertEquals(1, partialResult.failures.size)
            assertEquals(DictionaryRefreshFailureReason.TIMEOUT, partialResult.failures.single().reason)
            assertEquals(failedWordCache, database.apiSenseDao().getForWord(failedWord.id))

            val snapshotsBeforeTotalFailure =
                words.associate { word -> word.id to database.apiSenseDao().getForWord(word.id) }
            remote.responses.clear()
            remote.requests.clear()

            val totalFailureResult =
                repository.refreshDueWords(
                    nowEpochMillis = NOW + DictionaryEnrichmentRepository.STALE_AFTER_MILLIS + 1L,
                )

            assertEquals(0, totalFailureResult.refreshedWordCount)
            assertEquals(0, totalFailureResult.freshWordCount)
            assertEquals(2, totalFailureResult.failures.size)
            assertTrue(
                totalFailureResult.failures.all {
                    it.reason == DictionaryRefreshFailureReason.NETWORK
                },
            )
            assertEquals(words.map(Word::normalizedSpelling), remote.requests)
            words.forEach { word ->
                assertEquals(
                    snapshotsBeforeTotalFailure.getValue(word.id),
                    database.apiSenseDao().getForWord(word.id),
                )
            }
        }

    @Test
    fun recreatedRepository_observesSavedContentWhileNetworkIsUnavailable() =
        runTest {
            database.close()
            context.deleteDatabase(RESTART_DATABASE)
            database =
                Room
                    .databaseBuilder(context, LexiDueDatabase::class.java, RESTART_DATABASE)
                    .allowMainThreadQueries()
                    .build()
            val words = seedWords(count = 2)
            val onlineRemote =
                FakeDictionaryRemoteDataSource(
                    responses = words.associate { it.normalizedSpelling to successFor(it, "online") },
                )
            val onlineRepository = repository(FakeWordRepository(words), onlineRemote)
            onlineRepository.refreshDueWords(nowEpochMillis = NOW)
            val savedBeforeRecreation = onlineRepository.observeSavedEnrichment().first()

            database.close()
            database =
                Room
                    .databaseBuilder(context, LexiDueDatabase::class.java, RESTART_DATABASE)
                    .allowMainThreadQueries()
                    .build()

            val offlineRemote = FakeDictionaryRemoteDataSource()
            val recreatedRepository = repository(FakeWordRepository(words), offlineRemote)

            val savedWhileOffline = recreatedRepository.observeSavedEnrichment().first()

            assertEquals(savedBeforeRecreation, savedWhileOffline)
            assertEquals(2, savedWhileOffline.size)
            assertTrue(savedWhileOffline.all { it.senses.isNotEmpty() })
            assertTrue(offlineRemote.requests.isEmpty())
        }

    private fun repository(
        wordRepository: WordRepository,
        remoteDataSource: DictionaryRemoteDataSource,
    ): RoomDictionaryEnrichmentRepository =
        RoomDictionaryEnrichmentRepository(
            wordRepository = wordRepository,
            apiSenseDao = database.apiSenseDao(),
            remoteDataSource = remoteDataSource,
        )

    private suspend fun seedWords(count: Int): List<Word> {
        val words =
            (1..count).map { index ->
                Word(
                    id = "word-$index",
                    normalizedSpelling = "term$index",
                    displaySpelling = "Term $index",
                    deckId = "synthetic-test-deck",
                    sourceName = "Synthetic test data",
                    canonicalMeanings = emptyList(),
                )
            }
        database.wordDao().insertWords(
            words.map { word ->
                WordEntity(
                    id = word.id,
                    normalizedSpelling = word.normalizedSpelling,
                    displaySpelling = word.displaySpelling,
                    deckId = word.deckId,
                    sourceName = word.sourceName,
                    importedAtEpochMillis = 1L,
                )
            },
        )
        return words
    }

    private suspend fun cache(
        word: Word,
        fetchedAtEpochMillis: Long,
        version: String,
    ) {
        database.apiSenseDao().replaceForWord(
            wordId = word.id,
            senses =
                listOf(
                    ApiSenseEntity(
                        id = "api:${word.normalizedSpelling}:$version",
                        wordId = word.id,
                        partOfSpeech = PartOfSpeech.NOUN,
                        definition = "$version cached definition for ${word.displaySpelling}.",
                        example = "A synthetic example for ${word.displaySpelling}.",
                        phonetic = null,
                        audioUrl = null,
                        provider = FREE_DICTIONARY_API_PROVIDER,
                        source = FREE_DICTIONARY_API_SOURCE,
                        fetchedAtEpochMillis = fetchedAtEpochMillis,
                    ),
                ),
        )
    }

    private fun successFor(
        word: Word,
        version: String,
    ): DictionaryLookupResult =
        DictionaryLookupResult.Success(
            DictionaryEnrichment(
                normalizedWord = word.normalizedSpelling,
                senses =
                    listOf(
                        ApiSense(
                            stableId = "api:${word.normalizedSpelling}:$version",
                            normalizedWord = word.normalizedSpelling,
                            partOfSpeech = PartOfSpeech.NOUN,
                            definition = "$version remote definition for ${word.displaySpelling}.",
                            example = "A synthetic example for ${word.displaySpelling}.",
                            phonetic = null,
                            audioUrl = null,
                        ),
                    ),
            ),
        )

    private class FakeWordRepository(
        private val dueWords: List<Word>,
    ) : WordRepository {
        val requestedLimits = mutableListOf<Int>()

        override fun observeDeck(): Flow<List<Word>> = flowOf(dueWords)

        override fun observeDueWords(
            nowEpochMillis: Long,
            limit: Int,
        ): Flow<List<Word>> = flowOf(dueWords.take(limit))

        override suspend fun getDueWords(
            nowEpochMillis: Long,
            limit: Int,
        ): List<Word> {
            requestedLimits += limit
            return dueWords
        }

        override suspend fun getWordsByIds(ids: List<String>): List<Word> = dueWords.filter { it.id in ids }

        override suspend fun importStarterDeck(importedAtEpochMillis: Long): StarterDeckImportResult =
            StarterDeckImportResult(
                insertedWords = 0,
                insertedMeanings = 0,
                insertedProgressRows = 0,
            )
    }

    private class FakeDictionaryRemoteDataSource(
        responses: Map<String, DictionaryLookupResult> = emptyMap(),
    ) : DictionaryRemoteDataSource {
        val responses = responses.toMutableMap()
        val requests = mutableListOf<String>()

        override suspend fun lookup(requestedWord: String): DictionaryLookupResult {
            requests += requestedWord
            return responses[requestedWord] ?: DictionaryLookupResult.NetworkFailure
        }
    }

    private companion object {
        const val NOW = 5_000_000_000L
        const val RESTART_DATABASE = "dictionary-restart-test.db"
    }
}
