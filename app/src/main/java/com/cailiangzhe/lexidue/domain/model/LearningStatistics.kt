package com.cailiangzhe.lexidue.domain.model

data class LearningStatistics(
    val totalSessions: Int,
    val completedSessions: Int,
    val totalAttempts: Int,
    val correctAttempts: Int,
    val incorrectAttempts: Int,
    val skippedAttempts: Int,
    val dueWords: Int,
    val masteredWords: Int,
) {
    val scoredAttempts: Int
        get() = correctAttempts + incorrectAttempts

    val accuracy: Double
        get() = if (scoredAttempts == 0) 0.0 else correctAttempts.toDouble() / scoredAttempts
}
