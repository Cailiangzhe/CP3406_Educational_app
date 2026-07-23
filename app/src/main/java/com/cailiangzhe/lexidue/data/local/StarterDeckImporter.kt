package com.cailiangzhe.lexidue.data.local

import androidx.room.withTransaction
import com.cailiangzhe.lexidue.data.local.entity.CanonicalMeaningEntity
import com.cailiangzhe.lexidue.data.local.entity.ReviewProgressEntity
import com.cailiangzhe.lexidue.data.local.entity.WordEntity
import com.cailiangzhe.lexidue.domain.model.ReviewProgress
import com.cailiangzhe.lexidue.domain.model.StarterDeckImportResult
import java.util.Locale
import javax.inject.Inject

class StarterDeckImporter
    @Inject
    constructor(
        private val database: LexiDueDatabase,
    ) {
        suspend fun importIfNeeded(importedAtEpochMillis: Long): StarterDeckImportResult =
            database.withTransaction {
                val words = StarterDeck.entries.map { it.toWordEntity(importedAtEpochMillis) }
                val meanings = StarterDeck.entries.map { it.toMeaningEntity() }
                val progress =
                    words.map {
                        ReviewProgressEntity(
                            wordId = it.id,
                            reviewBox = ReviewProgress.NEW_REVIEW_BOX,
                            correctCount = 0,
                            incorrectCount = 0,
                            nextReviewAtEpochMillis = 0,
                            updatedAtEpochMillis = importedAtEpochMillis,
                        )
                    }

                StarterDeckImportResult(
                    insertedWords = database.wordDao().insertWords(words).insertedCount(),
                    insertedMeanings = database.wordDao().insertMeanings(meanings).insertedCount(),
                    insertedProgressRows = database.reviewProgressDao().insertAll(progress).insertedCount(),
                )
            }

        private fun StarterDeckEntry.toWordEntity(importedAtEpochMillis: Long): WordEntity {
            val normalized = spelling.trim().lowercase(Locale.ROOT)
            return WordEntity(
                id = wordId(normalized),
                normalizedSpelling = normalized,
                displaySpelling = spelling.trim(),
                deckId = StarterDeck.DECK_ID,
                sourceName = StarterDeck.SOURCE_NAME,
                importedAtEpochMillis = importedAtEpochMillis,
            )
        }

        private fun StarterDeckEntry.toMeaningEntity(): CanonicalMeaningEntity {
            val normalized = spelling.trim().lowercase(Locale.ROOT)
            return CanonicalMeaningEntity(
                id = "canonical:$normalized:1",
                wordId = wordId(normalized),
                partOfSpeech = partOfSpeech,
                definition = definition,
                provenance = StarterDeck.PROVENANCE,
            )
        }

        private fun List<Long>.insertedCount(): Int = count { it != -1L }

        private fun wordId(normalizedSpelling: String): String = "en:$normalizedSpelling"
    }
