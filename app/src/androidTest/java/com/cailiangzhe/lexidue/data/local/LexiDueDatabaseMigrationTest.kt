package com.cailiangzhe.lexidue.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LexiDueDatabaseMigrationTest {
    @get:Rule
    val helper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            LexiDueDatabase::class.java,
        )

    @Test
    fun migrate1To2_preservesEveryM2TableAndAddsEmptyApiCache() {
        helper.createDatabase(TEST_DATABASE, 1).apply {
            insertM2Rows()
            close()
        }

        val migrated =
            helper.runMigrationsAndValidate(
                TEST_DATABASE,
                2,
                true,
                MIGRATION_1_2,
            )

        migrated.assertSingleValue(
            "SELECT display_spelling FROM words WHERE id = 'en:analyse'",
            "analyse",
        )
        migrated.assertSingleValue(
            "SELECT definition FROM canonical_meanings WHERE id = 'canonical:analyse:1'",
            "Examine something carefully.",
        )
        migrated.assertSingleValue(
            "SELECT review_box FROM review_progress WHERE word_id = 'en:analyse'",
            "2",
        )
        migrated.assertSingleValue(
            "SELECT correct_count FROM practice_sessions WHERE id = 'session-1'",
            "1",
        )
        migrated.assertSingleValue(
            "SELECT prompt FROM session_questions WHERE id = 'question-1'",
            "analyse",
        )
        migrated.assertSingleValue(
            "SELECT outcome FROM attempts WHERE id = 'attempt-1'",
            "CORRECT",
        )
        migrated.assertSingleValue("SELECT COUNT(*) FROM api_senses", "0")

        migrated.execSQL(
            """
            INSERT INTO api_senses (
                id,
                word_id,
                part_of_speech,
                definition,
                example,
                phonetic,
                audio_url,
                provider,
                source,
                fetched_at_epoch_millis
            ) VALUES (
                'api:analyse:1',
                'en:analyse',
                'VERB',
                'Understand the nature of something.',
                NULL,
                NULL,
                NULL,
                'Free Dictionary API',
                'https://dictionaryapi.dev/',
                2000
            )
            """.trimIndent(),
        )
        migrated.assertSingleValue(
            "SELECT fetched_at_epoch_millis FROM api_senses WHERE id = 'api:analyse:1'",
            "2000",
        )
    }

    private fun SupportSQLiteDatabase.insertM2Rows() {
        execSQL(
            """
            INSERT INTO words (
                id,
                normalized_spelling,
                display_spelling,
                deck_id,
                source_name,
                imported_at_epoch_millis
            ) VALUES (
                'en:analyse',
                'analyse',
                'analyse',
                'starter-academic-en-v1',
                'LexiDue original starter deck',
                1000
            )
            """.trimIndent(),
        )
        execSQL(
            """
            INSERT INTO canonical_meanings (
                id,
                word_id,
                part_of_speech,
                definition,
                provenance
            ) VALUES (
                'canonical:analyse:1',
                'en:analyse',
                'VERB',
                'Examine something carefully.',
                'LexiDue original content'
            )
            """.trimIndent(),
        )
        execSQL(
            """
            INSERT INTO review_progress (
                word_id,
                review_box,
                correct_count,
                incorrect_count,
                next_review_at_epoch_millis,
                updated_at_epoch_millis
            ) VALUES (
                'en:analyse',
                2,
                3,
                1,
                3000,
                2000
            )
            """.trimIndent(),
        )
        execSQL(
            """
            INSERT INTO practice_sessions (
                id,
                difficulty,
                random_seed,
                planned_word_count,
                status,
                correct_count,
                current_question_id,
                started_at_epoch_millis,
                ended_at_epoch_millis
            ) VALUES (
                'session-1',
                'STANDARD',
                42,
                1,
                'ACTIVE',
                1,
                'question-1',
                1000,
                NULL
            )
            """.trimIndent(),
        )
        execSQL(
            """
            INSERT INTO session_questions (
                id,
                session_id,
                sequence,
                word_id,
                question_type,
                prompt,
                option_ids,
                correct_option_id,
                retry_of_question_id
            ) VALUES (
                'question-1',
                'session-1',
                0,
                'en:analyse',
                'WORD_TO_DEFINITION',
                'analyse',
                '["canonical:analyse:1"]',
                'canonical:analyse:1',
                NULL
            )
            """.trimIndent(),
        )
        execSQL(
            """
            INSERT INTO attempts (
                id,
                question_id,
                session_id,
                word_id,
                selected_option_id,
                outcome,
                is_retry,
                answered_at_epoch_millis
            ) VALUES (
                'attempt-1',
                'question-1',
                'session-1',
                'en:analyse',
                'canonical:analyse:1',
                'CORRECT',
                0,
                1500
            )
            """.trimIndent(),
        )
    }

    private fun SupportSQLiteDatabase.assertSingleValue(
        sql: String,
        expected: String,
    ) {
        query(sql).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(expected, cursor.getString(0))
            assertTrue(!cursor.moveToNext())
        }
    }

    private companion object {
        const val TEST_DATABASE = "lexidue-migration-test"
    }
}
