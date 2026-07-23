package com.cailiangzhe.lexidue.data.repository

import com.cailiangzhe.lexidue.data.local.dao.ReviewProgressDao
import com.cailiangzhe.lexidue.data.local.dao.StatisticsDao
import com.cailiangzhe.lexidue.domain.model.LearningStatistics
import com.cailiangzhe.lexidue.domain.model.ReviewBoxCount
import com.cailiangzhe.lexidue.domain.model.ReviewProgress
import com.cailiangzhe.lexidue.domain.repository.StatisticsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomStatisticsRepository
    @Inject
    constructor(
        private val statisticsDao: StatisticsDao,
        private val reviewProgressDao: ReviewProgressDao,
    ) : StatisticsRepository {
        override fun observeStatistics(nowEpochMillis: Long): Flow<LearningStatistics> =
            statisticsDao
                .observeLearningStatistics(nowEpochMillis, ReviewProgress.MASTERED_REVIEW_BOX)
                .map {
                    LearningStatistics(
                        totalSessions = it.totalSessions,
                        completedSessions = it.completedSessions,
                        totalAttempts = it.totalAttempts,
                        correctAttempts = it.correctAttempts,
                        incorrectAttempts = it.incorrectAttempts,
                        skippedAttempts = it.skippedAttempts,
                        dueWords = it.dueWords,
                        masteredWords = it.masteredWords,
                    )
                }

        override fun observeReviewBoxDistribution(): Flow<List<ReviewBoxCount>> =
            reviewProgressDao.observeReviewBoxDistribution().map { rows ->
                rows.map { ReviewBoxCount(reviewBox = it.reviewBox, wordCount = it.wordCount) }
            }
    }
