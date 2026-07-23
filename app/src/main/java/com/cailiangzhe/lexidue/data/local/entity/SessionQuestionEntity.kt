package com.cailiangzhe.lexidue.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.cailiangzhe.lexidue.domain.model.QuestionType

@Entity(
    tableName = "session_questions",
    foreignKeys = [
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
        Index("session_id"),
        Index("word_id"),
        Index(value = ["session_id", "sequence"], unique = true),
    ],
)
data class SessionQuestionEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "session_id") val sessionId: String,
    val sequence: Int,
    @ColumnInfo(name = "word_id") val wordId: String,
    @ColumnInfo(name = "question_type") val questionType: QuestionType,
    val prompt: String,
    @ColumnInfo(name = "option_ids") val optionIds: List<String>,
    @ColumnInfo(name = "correct_option_id") val correctOptionId: String,
    @ColumnInfo(name = "retry_of_question_id") val retryOfQuestionId: String?,
)
