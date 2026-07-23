package com.cailiangzhe.lexidue.domain.model

data class LearningSessionSummary(
    val actionCount: Int,
    val gradedAnswerCount: Int,
    val correctCount: Int,
    val incorrectCount: Int,
    val skippedCount: Int,
    val retryAnswerCount: Int,
    val exitedEarly: Boolean,
    /** Accuracy is a ratio from 0.0 to 1.0 and excludes Skip and Exit actions. */
    val accuracy: Double,
)

data class LearningMasterySnapshot(
    val totalCardCount: Int,
    val masteredCardIds: Set<String>,
    val dueCardIds: Set<String>,
    val needsReviewCardIds: Set<String>,
) {
    val masteredCardCount: Int
        get() = masteredCardIds.size

    val masteryRatio: Double
        get() = if (totalCardCount == 0) 0.0 else masteredCardCount.toDouble() / totalCardCount
}
