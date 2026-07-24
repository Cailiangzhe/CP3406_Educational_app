package com.cailiangzhe.lexidue.feature.home

import com.cailiangzhe.lexidue.domain.model.PartOfSpeech
import com.cailiangzhe.lexidue.domain.model.PracticeDifficulty

data class HomeUiState(
    val dueWordCount: Int = 0,
    val masteredWordCount: Int = 0,
    val sessionLength: Int = 10,
    val difficulty: PracticeDifficulty = PracticeDifficulty.STANDARD,
    val isLoading: Boolean = false,
    val localDataReady: Boolean = false,
    val hasLoadedSettings: Boolean = false,
    val isLoadingEnrichmentCache: Boolean = true,
    val cacheReadError: Boolean = false,
    val isStarting: Boolean = false,
    val isEnriching: Boolean = false,
    val enrichmentCards: List<HomeEnrichmentCard> = emptyList(),
    val enrichmentStatus: HomeEnrichmentStatus = HomeEnrichmentStatus.Idle,
    val lastSuccessfulRefreshAtEpochMillis: Long? = null,
    val errorMessage: String? = null,
) {
    val startEnabled: Boolean
        get() = localDataReady && hasLoadedSettings && !isStarting

    val enrichEnabled: Boolean
        get() =
            localDataReady &&
                !isLoadingEnrichmentCache &&
                !cacheReadError &&
                !isEnriching &&
                dueWordCount > 0
}

data class HomeEnrichmentCard(
    val wordId: String,
    val displayWord: String,
    val senses: List<HomeEnrichmentSense>,
    val fetchedAtEpochMillis: Long,
    val isStale: Boolean,
)

data class HomeEnrichmentSense(
    val partOfSpeech: PartOfSpeech,
    val definition: String,
    val example: String?,
    val phonetic: String?,
    val provider: String,
    val source: String,
)

sealed interface HomeEnrichmentStatus {
    data object Idle : HomeEnrichmentStatus

    data object Refreshing : HomeEnrichmentStatus

    data object NoDueWords : HomeEnrichmentStatus

    data class Saved(
        val refreshedWordCount: Int,
        val freshWordCount: Int,
    ) : HomeEnrichmentStatus

    data class PartialFailure(
        val refreshedWordCount: Int,
        val freshWordCount: Int,
        val failedWordCount: Int,
    ) : HomeEnrichmentStatus

    data class Failed(
        val failedWordCount: Int,
    ) : HomeEnrichmentStatus
}

sealed interface HomeUiAction {
    data object StartPractice : HomeUiAction

    data object OpenStatistics : HomeUiAction

    data object OpenSettings : HomeUiAction

    data object EnrichDeck : HomeUiAction

    data object RetryLocalData : HomeUiAction

    data object RetryEnrichmentCache : HomeUiAction
}

sealed interface HomeEffect {
    data class OpenPractice(
        val sessionId: String,
    ) : HomeEffect

    data object OpenStatistics : HomeEffect

    data object OpenSettings : HomeEffect
}
