package com.cailiangzhe.lexidue.domain.repository

import com.cailiangzhe.lexidue.domain.model.ReviewProgress
import kotlinx.coroutines.flow.Flow

interface ReviewProgressRepository {
    fun observeProgress(wordId: String): Flow<ReviewProgress?>

    suspend fun getProgress(wordId: String): ReviewProgress?
}
