package com.cailiangzhe.lexidue.feature.statistics

data class RecentSessionSummary(
    val id: String,
    val dateLabel: String,
    val resultLabel: String,
)

data class ReviewWordSummary(
    val word: String,
    val reason: String,
)

data class StatisticsUiState(
    val totalSessions: Int = 0,
    val gradedAttempts: Int = 0,
    val overallAccuracyPercent: Int = 0,
    val masteredWords: Int = 0,
    val dueWords: Int = 0,
    val reviewBoxCounts: List<Int> = listOf(0, 0, 0, 0, 0),
    val recentSessions: List<RecentSessionSummary> = emptyList(),
    val wordsToReview: List<ReviewWordSummary> = emptyList(),
)
