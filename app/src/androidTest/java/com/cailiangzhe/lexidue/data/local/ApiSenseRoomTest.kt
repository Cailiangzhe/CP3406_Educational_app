package com.cailiangzhe.lexidue.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cailiangzhe.lexidue.data.local.dao.ApiSenseDao
import com.cailiangzhe.lexidue.data.local.entity.ApiSenseEntity
import com.cailiangzhe.lexidue.data.local.entity.CanonicalMeaningEntity
import com.cailiangzhe.lexidue.data.local.entity.WordEntity
import com.cailiangzhe.lexidue.domain.model.PartOfSpeech
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ApiSenseRoomTest {
    private lateinit var database: LexiDueDatabase
    private lateinit var dao: ApiSenseDao

    @Before
    fun createDatabase() =
        runTest {
            val context = ApplicationProvider.getApplicationContext<Context>()
            database =
                Room
                    .inMemoryDatabaseBuilder(context, LexiDueDatabase::class.java)
                    .allowMainThreadQueries()
                    .build()
            dao = database.apiSenseDao()
            database.wordDao().insertWords(
                listOf(
                    word(WORD_ID, "analyse"),
                    word(OTHER_WORD_ID, "assess"),
                ),
            )
            database.wordDao().insertMeanings(
                listOf(
                    canonicalMeaning(WORD_ID, "canonical:analyse:1", "Examine something carefully."),
                    canonicalMeaning(OTHER_WORD_ID, "canonical:assess:1", "Evaluate something."),
                ),
            )
        }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun replaceAndUpsert_keepApiSensesSeparateAndRetainOldCacheOnRejectedInput() =
        runTest {
            val original = apiSense(id = "api:analyse:1", fetchedAt = 100L)
            dao.upsertAll(listOf(original))
            dao.upsertAll(
                listOf(
                    original.copy(definition = "Updated provider definition.", fetchedAtEpochMillis = 200L),
                    apiSense(id = "api:analyse:2", fetchedAt = 200L),
                ),
            )

            assertEquals(2, dao.getForWord(WORD_ID).size)
            assertTrue(dao.getForWord(WORD_ID).any { it.definition == "Updated provider definition." })

            val replacement =
                apiSense(
                    id = "api:analyse:3",
                    definition = "Latest validated provider definition.",
                    fetchedAt = 300L,
                )
            dao.replaceForWord(WORD_ID, listOf(replacement))

            assertEquals(listOf(replacement), dao.getForWord(WORD_ID))
            val related = dao.observeWordWithEnrichment(WORD_ID).first()
            assertNotNull(related)
            assertEquals("canonical:analyse:1", related?.canonicalMeanings?.single()?.id)
            assertEquals("api:analyse:3", related?.apiSenses?.single()?.id)
            assertEquals(300L, dao.getLatestFetchedAtEpochMillis(WORD_ID))
            assertEquals(300L, dao.observeLatestFetchedAtEpochMillis().first())

            var rejected = false
            try {
                dao.replaceForWord(
                    WORD_ID,
                    listOf(apiSense(id = "api:wrong-word", wordId = OTHER_WORD_ID, fetchedAt = 400L)),
                )
            } catch (_: IllegalArgumentException) {
                rejected = true
            }

            assertTrue(rejected)
            assertEquals(listOf(replacement), dao.getForWord(WORD_ID))
        }

    @Test
    fun offlineObservations_orderRecentCacheAndWordDeletionCascades() =
        runTest {
            dao.replaceForWord(WORD_ID, listOf(apiSense(id = "api:analyse:1", fetchedAt = 100L)))
            dao.replaceForWord(
                OTHER_WORD_ID,
                listOf(
                    apiSense(
                        id = "api:assess:1",
                        wordId = OTHER_WORD_ID,
                        definition = "Judge the quality of something.",
                        fetchedAt = 200L,
                    ),
                ),
            )

            assertEquals(2, dao.observeAllWordsWithEnrichment().first().size)
            assertEquals(
                OTHER_WORD_ID,
                dao
                    .observeRecentWordEnrichment(limit = 1)
                    .first()
                    .single()
                    .word.id,
            )

            database.openHelper.writableDatabase.execSQL(
                "DELETE FROM words WHERE id = ?",
                arrayOf<Any?>(OTHER_WORD_ID),
            )

            assertTrue(dao.getForWord(OTHER_WORD_ID).isEmpty())
        }

    private fun word(
        id: String,
        spelling: String,
    ): WordEntity =
        WordEntity(
            id = id,
            normalizedSpelling = spelling,
            displaySpelling = spelling,
            deckId = "starter-academic-en-v1",
            sourceName = "LexiDue original starter deck",
            importedAtEpochMillis = 10L,
        )

    private fun canonicalMeaning(
        wordId: String,
        id: String,
        definition: String,
    ): CanonicalMeaningEntity =
        CanonicalMeaningEntity(
            id = id,
            wordId = wordId,
            partOfSpeech = PartOfSpeech.VERB,
            definition = definition,
            provenance = "LexiDue original content",
        )

    private fun apiSense(
        id: String,
        wordId: String = WORD_ID,
        definition: String = "Understand the nature of something.",
        fetchedAt: Long,
    ): ApiSenseEntity =
        ApiSenseEntity(
            id = id,
            wordId = wordId,
            partOfSpeech = PartOfSpeech.VERB,
            definition = definition,
            example = "The learner analysed the result.",
            phonetic = "/ˈæn.əl.aɪz/",
            audioUrl = "https://example.test/analyse.mp3",
            provider = "Free Dictionary API",
            source = "https://dictionaryapi.dev/",
            fetchedAtEpochMillis = fetchedAt,
        )

    private companion object {
        const val WORD_ID = "en:analyse"
        const val OTHER_WORD_ID = "en:assess"
    }
}
