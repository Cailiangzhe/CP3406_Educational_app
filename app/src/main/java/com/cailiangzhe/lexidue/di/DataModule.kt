package com.cailiangzhe.lexidue.di

import android.content.Context
import androidx.room.Room
import com.cailiangzhe.lexidue.data.local.LexiDueDatabase
import com.cailiangzhe.lexidue.data.local.dao.LearningTransactionDao
import com.cailiangzhe.lexidue.data.local.dao.ReviewProgressDao
import com.cailiangzhe.lexidue.data.local.dao.SessionDao
import com.cailiangzhe.lexidue.data.local.dao.StatisticsDao
import com.cailiangzhe.lexidue.data.local.dao.WordDao
import com.cailiangzhe.lexidue.data.repository.RoomPracticeSessionRepository
import com.cailiangzhe.lexidue.data.repository.RoomReviewProgressRepository
import com.cailiangzhe.lexidue.data.repository.RoomStatisticsRepository
import com.cailiangzhe.lexidue.data.repository.RoomWordRepository
import com.cailiangzhe.lexidue.domain.repository.PracticeSessionRepository
import com.cailiangzhe.lexidue.domain.repository.ReviewProgressRepository
import com.cailiangzhe.lexidue.domain.repository.StatisticsRepository
import com.cailiangzhe.lexidue.domain.repository.WordRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds
    @Singleton
    abstract fun bindWordRepository(repository: RoomWordRepository): WordRepository

    @Binds
    @Singleton
    abstract fun bindReviewProgressRepository(repository: RoomReviewProgressRepository): ReviewProgressRepository

    @Binds
    @Singleton
    abstract fun bindPracticeSessionRepository(repository: RoomPracticeSessionRepository): PracticeSessionRepository

    @Binds
    @Singleton
    abstract fun bindStatisticsRepository(repository: RoomStatisticsRepository): StatisticsRepository

    companion object {
        @Provides
        @Singleton
        fun provideDatabase(
            @ApplicationContext context: Context,
        ): LexiDueDatabase = Room.databaseBuilder(context, LexiDueDatabase::class.java, LexiDueDatabase.DATABASE_NAME).build()

        @Provides
        fun provideWordDao(database: LexiDueDatabase): WordDao = database.wordDao()

        @Provides
        fun provideReviewProgressDao(database: LexiDueDatabase): ReviewProgressDao = database.reviewProgressDao()

        @Provides
        fun provideSessionDao(database: LexiDueDatabase): SessionDao = database.sessionDao()

        @Provides
        fun provideLearningTransactionDao(database: LexiDueDatabase): LearningTransactionDao = database.learningTransactionDao()

        @Provides
        fun provideStatisticsDao(database: LexiDueDatabase): StatisticsDao = database.statisticsDao()
    }
}
