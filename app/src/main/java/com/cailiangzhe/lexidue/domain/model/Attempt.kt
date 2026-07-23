package com.cailiangzhe.lexidue.domain.model

data class Attempt(
    val id: String,
    val questionId: String,
    val sessionId: String,
    val wordId: String,
    val selectedOptionId: String?,
    val outcome: AttemptOutcome,
    val isRetry: Boolean,
    val answeredAtEpochMillis: Long,
)

enum class AttemptOutcome {
    CORRECT,
    INCORRECT,
    SKIPPED,
}

sealed interface RecordAttemptResult {
    data class Recorded(
        val attempt: Attempt,
        val progress: ReviewProgress?,
    ) : RecordAttemptResult

    data class AlreadyRecorded(
        val attempt: Attempt,
    ) : RecordAttemptResult
}
