package com.cailiangzhe.lexidue.domain.model

/** A validated API snapshot joined to its local word for offline display. */
data class SavedDictionaryEnrichment(
    val wordId: String,
    val displayWord: String,
    val senses: List<ApiSense>,
    val fetchedAtEpochMillis: Long,
) {
    init {
        require(wordId.isNotBlank()) { "Saved enrichment must identify a local word." }
        require(displayWord.isNotBlank()) { "Saved enrichment must have display text." }
        require(senses.isNotEmpty()) { "Saved enrichment must contain at least one sense." }
        require(fetchedAtEpochMillis >= 0L) { "Fetch time cannot be negative." }
    }
}

data class DictionaryRefreshResult(
    val selectedWordCount: Int,
    val refreshedWordCount: Int,
    val freshWordCount: Int,
    val failures: List<DictionaryRefreshFailure>,
) {
    init {
        require(selectedWordCount >= 0)
        require(refreshedWordCount >= 0)
        require(freshWordCount >= 0)
        require(refreshedWordCount + freshWordCount + failures.size == selectedWordCount) {
            "Every selected word must be refreshed, fresh, or failed."
        }
    }

    val networkRequestCount: Int
        get() = refreshedWordCount + failures.size
}

data class DictionaryRefreshFailure(
    val displayWord: String,
    val reason: DictionaryRefreshFailureReason,
)

enum class DictionaryRefreshFailureReason {
    INVALID_REQUEST,
    NOT_FOUND,
    MALFORMED_PAYLOAD,
    TIMEOUT,
    NETWORK,
    HTTP,
    UNUSABLE_CONTENT,
}
