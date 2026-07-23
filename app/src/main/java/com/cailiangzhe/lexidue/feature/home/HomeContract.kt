package com.cailiangzhe.lexidue.feature.home

data class HomeUiState(
    val dueWordCount: Int = 0,
    val masteredWordCount: Int = 0,
    val sessionLength: Int = 10,
    val isLoading: Boolean = false,
    val isStarting: Boolean = false,
    val isEnriching: Boolean = false,
    val lastRefreshLabel: String? = null,
    val errorMessage: String? = null,
) {
    val startEnabled: Boolean
        get() = !isLoading && !isStarting
}

sealed interface HomeUiAction {
    data object StartPractice : HomeUiAction

    data object OpenStatistics : HomeUiAction

    data object OpenSettings : HomeUiAction

    data object EnrichDeck : HomeUiAction
}

sealed interface HomeEffect {
    data class OpenPractice(
        val sessionId: String,
    ) : HomeEffect

    data object OpenStatistics : HomeEffect

    data object OpenSettings : HomeEffect
}
