package com.cailiangzhe.lexidue.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.cailiangzhe.lexidue.data.local.entity.CanonicalMeaningEntity
import com.cailiangzhe.lexidue.data.local.entity.WordEntity

data class WordWithMeaningsEntity(
    @Embedded val word: WordEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "word_id",
    )
    val meanings: List<CanonicalMeaningEntity>,
)
