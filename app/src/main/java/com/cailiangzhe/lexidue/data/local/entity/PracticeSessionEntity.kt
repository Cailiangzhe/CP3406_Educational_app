package com.cailiangzhe.lexidue.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.cailiangzhe.lexidue.domain.model.PracticeDifficulty
import com.cailiangzhe.lexidue.domain.model.SessionStatus

@Entity(
    tableName = "practice_sessions",
    indices = [Index("started_at_epoch_millis"), Index("status")],
)
data class PracticeSessionEntity(
    @PrimaryKey val id: String,
    val difficulty: PracticeDifficulty,
    @ColumnInfo(name = "random_seed") val randomSeed: Long,
    @ColumnInfo(name = "planned_word_count") val plannedWordCount: Int,
    val status: SessionStatus,
    @ColumnInfo(name = "correct_count") val correctCount: Int,
    @ColumnInfo(name = "current_question_id") val currentQuestionId: String?,
    @ColumnInfo(name = "started_at_epoch_millis") val startedAtEpochMillis: Long,
    @ColumnInfo(name = "ended_at_epoch_millis") val endedAtEpochMillis: Long?,
)
