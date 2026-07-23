package com.cailiangzhe.lexidue.data.repository

import com.cailiangzhe.lexidue.data.local.StarterDeckImporter
import com.cailiangzhe.lexidue.data.local.dao.WordDao
import com.cailiangzhe.lexidue.domain.model.StarterDeckImportResult
import com.cailiangzhe.lexidue.domain.model.Word
import com.cailiangzhe.lexidue.domain.repository.WordRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomWordRepository
    @Inject
    constructor(
        private val wordDao: WordDao,
        private val starterDeckImporter: StarterDeckImporter,
    ) : WordRepository {
        override fun observeDeck(): Flow<List<Word>> = wordDao.observeAllWords().map { rows -> rows.map { it.toDomain() } }

        override fun observeDueWords(
            nowEpochMillis: Long,
            limit: Int,
        ): Flow<List<Word>> {
            require(limit > 0) { "Due-word limit must be positive" }
            return wordDao.observeDueWords(nowEpochMillis, limit).map { rows -> rows.map { it.toDomain() } }
        }

        override suspend fun getDueWords(
            nowEpochMillis: Long,
            limit: Int,
        ): List<Word> {
            require(limit > 0) { "Due-word limit must be positive" }
            return wordDao.getDueWords(nowEpochMillis, limit).map { it.toDomain() }
        }

        override suspend fun getWordsByIds(ids: List<String>): List<Word> =
            if (ids.isEmpty()) emptyList() else wordDao.getWordsByIds(ids.distinct()).map { it.toDomain() }

        override suspend fun importStarterDeck(importedAtEpochMillis: Long): StarterDeckImportResult =
            starterDeckImporter.importIfNeeded(importedAtEpochMillis)
    }
