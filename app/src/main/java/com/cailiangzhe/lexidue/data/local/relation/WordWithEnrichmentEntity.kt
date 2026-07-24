package com.cailiangzhe.lexidue.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.cailiangzhe.lexidue.data.local.entity.ApiSenseEntity
import com.cailiangzhe.lexidue.data.local.entity.CanonicalMeaningEntity
import com.cailiangzhe.lexidue.data.local.entity.WordEntity

/** A word with canonical quiz content and separately quarantined API enrichment. */
data class WordWithEnrichmentEntity(
    @Embedded val word: WordEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "word_id",
    )
    val canonicalMeanings: List<CanonicalMeaningEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "word_id",
    )
    val apiSenses: List<ApiSenseEntity>,
)
