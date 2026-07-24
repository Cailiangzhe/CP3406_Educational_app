package com.cailiangzhe.lexidue.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.cailiangzhe.lexidue.domain.model.PartOfSpeech

/**
 * Validated dictionary content cached from an external provider.
 *
 * These rows are deliberately stored outside [CanonicalMeaningEntity], so remote content can be
 * shown as enrichment without ever becoming a scored answer or a quiz distractor.
 */
@Entity(
    tableName = "api_senses",
    foreignKeys = [
        ForeignKey(
            entity = WordEntity::class,
            parentColumns = ["id"],
            childColumns = ["word_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("word_id"),
        Index("fetched_at_epoch_millis"),
    ],
)
data class ApiSenseEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "word_id") val wordId: String,
    @ColumnInfo(name = "part_of_speech") val partOfSpeech: PartOfSpeech,
    val definition: String,
    val example: String?,
    val phonetic: String?,
    @ColumnInfo(name = "audio_url") val audioUrl: String?,
    val provider: String,
    val source: String,
    @ColumnInfo(name = "fetched_at_epoch_millis") val fetchedAtEpochMillis: Long,
)
