package com.cailiangzhe.lexidue.domain.repository

import com.cailiangzhe.lexidue.domain.model.LearningStatistics
import com.cailiangzhe.lexidue.domain.model.ReviewBoxCount
import kotlinx.coroutines.flow.Flow

interface StatisticsRepository {
    fun observeStatistics(nowEpochMillis: Long): Flow<LearningStatistics>

    fun observeReviewBoxDistribution(): Flow<List<ReviewBoxCount>>
}
