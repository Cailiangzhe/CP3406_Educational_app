package com.cailiangzhe.lexidue.data.repository

import com.cailiangzhe.lexidue.data.local.dao.ReviewProgressDao
import com.cailiangzhe.lexidue.domain.model.ReviewProgress
import com.cailiangzhe.lexidue.domain.repository.ReviewProgressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomReviewProgressRepository
    @Inject
    constructor(
        private val reviewProgressDao: ReviewProgressDao,
    ) : ReviewProgressRepository {
        override fun observeProgress(wordId: String): Flow<ReviewProgress?> = reviewProgressDao.observe(wordId).map { it?.toDomain() }

        override suspend fun getProgress(wordId: String): ReviewProgress? = reviewProgressDao.get(wordId)?.toDomain()
    }
