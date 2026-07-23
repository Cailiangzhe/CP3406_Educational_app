package com.cailiangzhe.lexidue.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "words",
    indices = [Index(value = ["normalized_spelling"], unique = true)],
)
data class WordEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "normalized_spelling") val normalizedSpelling: String,
    @ColumnInfo(name = "display_spelling") val displaySpelling: String,
    @ColumnInfo(name = "deck_id") val deckId: String,
    @ColumnInfo(name = "source_name") val sourceName: String,
    @ColumnInfo(name = "imported_at_epoch_millis") val importedAtEpochMillis: Long,
)
