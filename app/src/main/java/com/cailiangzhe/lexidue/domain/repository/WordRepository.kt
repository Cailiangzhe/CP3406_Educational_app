package com.cailiangzhe.lexidue.domain.repository

import com.cailiangzhe.lexidue.domain.model.StarterDeckImportResult
import com.cailiangzhe.lexidue.domain.model.Word
import kotlinx.coroutines.flow.Flow

interface WordRepository {
    fun observeDeck(): Flow<List<Word>>

    fun observeDueWords(
        nowEpochMillis: Long,
        limit: Int,
    ): Flow<List<Word>>

    suspend fun getDueWords(
        nowEpochMillis: Long,
        limit: Int,
    ): List<Word>

    suspend fun getWordsByIds(ids: List<String>): List<Word>

    suspend fun importStarterDeck(importedAtEpochMillis: Long): StarterDeckImportResult
}
