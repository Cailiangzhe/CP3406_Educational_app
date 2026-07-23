package com.cailiangzhe.lexidue.data.local.query

import androidx.room.ColumnInfo

data class LearningStatisticsRow(
    @ColumnInfo(name = "total_sessions") val totalSessions: Int,
    @ColumnInfo(name = "completed_sessions") val completedSessions: Int,
    @ColumnInfo(name = "total_attempts") val totalAttempts: Int,
    @ColumnInfo(name = "correct_attempts") val correctAttempts: Int,
    @ColumnInfo(name = "incorrect_attempts") val incorrectAttempts: Int,
    @ColumnInfo(name = "skipped_attempts") val skippedAttempts: Int,
    @ColumnInfo(name = "due_words") val dueWords: Int,
    @ColumnInfo(name = "mastered_words") val masteredWords: Int,
)

data class ReviewBoxCountRow(
    @ColumnInfo(name = "review_box") val reviewBox: Int,
    @ColumnInfo(name = "word_count") val wordCount: Int,
)
