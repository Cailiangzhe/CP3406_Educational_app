package com.cailiangzhe.lexidue.domain.model

/** Pure learning-engine state; persistence maps this to its own ReviewProgress model. */
data class ReviewScheduleState(
    val cardId: String,
    val reviewBox: Int = 0,
    val correctCount: Int = 0,
    val incorrectCount: Int = 0,
    val nextReviewAtEpochMillis: Long? = null,
) {
    init {
        require(cardId.isNotBlank()) { "Review state must reference a learning card." }
        require(reviewBox in 0..MAX_REVIEW_BOX) { "Review box must be between 0 and 5." }
        require(correctCount >= 0) { "Correct count cannot be negative." }
        require(incorrectCount >= 0) { "Incorrect count cannot be negative." }
    }

    val isMastered: Boolean
        get() = reviewBox == MAX_REVIEW_BOX

    companion object {
        const val MAX_REVIEW_BOX = 5
    }
}
