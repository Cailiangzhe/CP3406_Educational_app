package com.cailiangzhe.lexidue.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.cailiangzhe.lexidue.domain.model.PartOfSpeech

@Entity(
    tableName = "canonical_meanings",
    foreignKeys = [
        ForeignKey(
            entity = WordEntity::class,
            parentColumns = ["id"],
            childColumns = ["word_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("word_id")],
)
data class CanonicalMeaningEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "word_id") val wordId: String,
    @ColumnInfo(name = "part_of_speech") val partOfSpeech: PartOfSpeech,
    val definition: String,
    val provenance: String,
)
