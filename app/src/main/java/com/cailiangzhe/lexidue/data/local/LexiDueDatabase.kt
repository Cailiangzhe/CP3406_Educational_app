package com.cailiangzhe.lexidue.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.cailiangzhe.lexidue.data.local.dao.ApiSenseDao
import com.cailiangzhe.lexidue.data.local.dao.LearningTransactionDao
import com.cailiangzhe.lexidue.data.local.dao.ReviewProgressDao
import com.cailiangzhe.lexidue.data.local.dao.SessionDao
import com.cailiangzhe.lexidue.data.local.dao.StatisticsDao
import com.cailiangzhe.lexidue.data.local.dao.WordDao
import com.cailiangzhe.lexidue.data.local.entity.ApiSenseEntity
import com.cailiangzhe.lexidue.data.local.entity.AttemptEntity
import com.cailiangzhe.lexidue.data.local.entity.CanonicalMeaningEntity
import com.cailiangzhe.lexidue.data.local.entity.PracticeSessionEntity
import com.cailiangzhe.lexidue.data.local.entity.ReviewProgressEntity
import com.cailiangzhe.lexidue.data.local.entity.SessionQuestionEntity
import com.cailiangzhe.lexidue.data.local.entity.WordEntity

@Database(
    entities = [
        WordEntity::class,
        CanonicalMeaningEntity::class,
        ReviewProgressEntity::class,
        PracticeSessionEntity::class,
        SessionQuestionEntity::class,
        AttemptEntity::class,
        ApiSenseEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(AppTypeConverters::class)
abstract class LexiDueDatabase : RoomDatabase() {
    abstract fun apiSenseDao(): ApiSenseDao

    abstract fun wordDao(): WordDao

    abstract fun reviewProgressDao(): ReviewProgressDao

    abstract fun sessionDao(): SessionDao

    abstract fun learningTransactionDao(): LearningTransactionDao

    abstract fun statisticsDao(): StatisticsDao

    companion object {
        const val DATABASE_NAME = "lexidue.db"
    }
}

/** Adds the quarantined dictionary cache without rewriting any M2 table. */
val MIGRATION_1_2: Migration =
    object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS api_senses (
                    id TEXT NOT NULL,
                    word_id TEXT NOT NULL,
                    part_of_speech TEXT NOT NULL,
                    definition TEXT NOT NULL,
                    example TEXT,
                    phonetic TEXT,
                    audio_url TEXT,
                    provider TEXT NOT NULL,
                    source TEXT NOT NULL,
                    fetched_at_epoch_millis INTEGER NOT NULL,
                    PRIMARY KEY(id),
                    FOREIGN KEY(word_id) REFERENCES words(id)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_api_senses_word_id
                ON api_senses (word_id)
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_api_senses_fetched_at_epoch_millis
                ON api_senses (fetched_at_epoch_millis)
                """.trimIndent(),
            )
        }
    }
