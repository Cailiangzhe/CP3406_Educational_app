package com.cailiangzhe.lexidue.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.cailiangzhe.lexidue.domain.model.AttemptOutcome

@Entity(
    tableName = "attempts",
    foreignKeys = [
        ForeignKey(
            entity = SessionQuestionEntity::class,
            parentColumns = ["id"],
            childColumns = ["question_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = PracticeSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = WordEntity::class,
            parentColumns = ["id"],
            childColumns = ["word_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["question_id"], unique = true),
        Index("session_id"),
        Index("word_id"),
        Index("answered_at_epoch_millis"),
    ],
)
data class AttemptEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "question_id") val questionId: String,
    @ColumnInfo(name = "session_id") val sessionId: String,
    @ColumnInfo(name = "word_id") val wordId: String,
    @ColumnInfo(name = "selected_option_id") val selectedOptionId: String?,
    val outcome: AttemptOutcome,
    @ColumnInfo(name = "is_retry") val isRetry: Boolean,
    @ColumnInfo(name = "answered_at_epoch_millis") val answeredAtEpochMillis: Long,
)
