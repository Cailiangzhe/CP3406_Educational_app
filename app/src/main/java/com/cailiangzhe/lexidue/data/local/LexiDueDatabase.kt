package com.cailiangzhe.lexidue.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.cailiangzhe.lexidue.data.local.dao.LearningTransactionDao
import com.cailiangzhe.lexidue.data.local.dao.ReviewProgressDao
import com.cailiangzhe.lexidue.data.local.dao.SessionDao
import com.cailiangzhe.lexidue.data.local.dao.StatisticsDao
import com.cailiangzhe.lexidue.data.local.dao.WordDao
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
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(AppTypeConverters::class)
abstract class LexiDueDatabase : RoomDatabase() {
    abstract fun wordDao(): WordDao

    abstract fun reviewProgressDao(): ReviewProgressDao

    abstract fun sessionDao(): SessionDao

    abstract fun learningTransactionDao(): LearningTransactionDao

    abstract fun statisticsDao(): StatisticsDao

    companion object {
        const val DATABASE_NAME = "lexidue.db"
    }
}
