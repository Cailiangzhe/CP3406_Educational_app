package com.cailiangzhe.lexidue.domain.model

data class ReviewProgress(
    val wordId: String,
    val reviewBox: Int,
    val correctCount: Int,
    val incorrectCount: Int,
    val nextReviewAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
) {
    val isMastered: Boolean
        get() = reviewBox >= MASTERED_REVIEW_BOX

    companion object {
        const val NEW_REVIEW_BOX = 0
        const val MASTERED_REVIEW_BOX = 5
    }
}

data class ReviewBoxCount(
    val reviewBox: Int,
    val wordCount: Int,
)
