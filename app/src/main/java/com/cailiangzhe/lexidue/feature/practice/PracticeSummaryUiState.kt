package com.cailiangzhe.lexidue.feature.practice

data class PracticeSummaryUiState(
    val isLoading: Boolean = true,
    val sessionId: String = "",
    val plannedWordCount: Int = 0,
    val correctCount: Int = 0,
    val incorrectCount: Int = 0,
    val skippedCount: Int = 0,
    val retryCount: Int = 0,
    val accuracyPercent: Int = 0,
    val completedAtLabel: String? = null,
    val reviewWords: List<String> = emptyList(),
    val errorMessage: String? = null,
)
