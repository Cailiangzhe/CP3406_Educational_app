package com.cailiangzhe.lexidue.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "review_progress",
    foreignKeys = [
        ForeignKey(
            entity = WordEntity::class,
            parentColumns = ["id"],
            childColumns = ["word_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("next_review_at_epoch_millis")],
)
data class ReviewProgressEntity(
    @PrimaryKey
    @ColumnInfo(name = "word_id")
    val wordId: String,
    @ColumnInfo(name = "review_box") val reviewBox: Int,
    @ColumnInfo(name = "correct_count") val correctCount: Int,
    @ColumnInfo(name = "incorrect_count") val incorrectCount: Int,
    @ColumnInfo(name = "next_review_at_epoch_millis") val nextReviewAtEpochMillis: Long,
    @ColumnInfo(name = "updated_at_epoch_millis") val updatedAtEpochMillis: Long,
)
