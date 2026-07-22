package com.cailiangzhe.lexidue.feature.home

data class HomeUiState(
    val dueWordCount: Int = 0,
    val reviewWordCount: Int = 0,
    val masteredWordCount: Int = 0,
    val sessionLength: Int = 10,
    val isEnriching: Boolean = false,
    val lastRefreshLabel: String? = null,
)

sealed interface HomeUiAction {
    data object StartPractice : HomeUiAction

    data object OpenStatistics : HomeUiAction

    data object OpenSettings : HomeUiAction

    data object EnrichDeck : HomeUiAction
}
