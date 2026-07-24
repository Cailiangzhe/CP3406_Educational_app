package com.cailiangzhe.lexidue.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cailiangzhe.lexidue.domain.model.DictionaryRefreshResult
import com.cailiangzhe.lexidue.domain.model.SavedDictionaryEnrichment
import com.cailiangzhe.lexidue.domain.repository.DictionaryEnrichmentRepository
import com.cailiangzhe.lexidue.domain.repository.SettingsRepository
import com.cailiangzhe.lexidue.domain.repository.StatisticsRepository
import com.cailiangzhe.lexidue.domain.repository.WordRepository
import com.cailiangzhe.lexidue.domain.repository.isDictionaryEnrichmentFresh
import com.cailiangzhe.lexidue.domain.usecase.StartPracticeSession
import com.cailiangzhe.lexidue.domain.usecase.TimeProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.CancellationException
import javax.inject.Inject

@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        private val wordRepository: WordRepository,
        private val statisticsRepository: StatisticsRepository,
        private val dictionaryEnrichmentRepository: DictionaryEnrichmentRepository,
        private val settingsRepository: SettingsRepository,
        private val startPracticeSession: StartPracticeSession,
        private val timeProvider: TimeProvider,
    ) : ViewModel() {
        private val mutableUiState = MutableStateFlow(HomeUiState(isLoading = true))
        val uiState: StateFlow<HomeUiState> = mutableUiState.asStateFlow()

        private val effectChannel = Channel<HomeEffect>(capacity = Channel.BUFFERED)
        val effects = effectChannel.receiveAsFlow()

        private var overviewJob: Job? = null
        private var enrichmentCacheJob: Job? = null

        init {
            observeLocalOverview()
            observeSettings()
            observeSavedEnrichment()
        }

        fun onAction(action: HomeUiAction) {
            when (action) {
                HomeUiAction.StartPractice -> startPractice()
                HomeUiAction.OpenStatistics -> effectChannel.trySend(HomeEffect.OpenStatistics)
                HomeUiAction.OpenSettings -> effectChannel.trySend(HomeEffect.OpenSettings)
                HomeUiAction.EnrichDeck -> enrichDeck()
                HomeUiAction.RetryLocalData -> observeLocalOverview()
                HomeUiAction.RetryEnrichmentCache -> observeSavedEnrichment()
            }
        }

        private fun observeLocalOverview() {
            overviewJob?.cancel()
            mutableUiState.update {
                it.copy(
                    isLoading = true,
                    localDataReady = false,
                    errorMessage = null,
                )
            }
            overviewJob =
                viewModelScope.launch {
                    val observedAt = timeProvider.nowEpochMillis()
                    try {
                        wordRepository.importStarterDeck(observedAt)
                        statisticsRepository.observeStatistics(observedAt).collect { statistics ->
                            mutableUiState.update { current ->
                                current.copy(
                                    dueWordCount = statistics.dueWords,
                                    masteredWordCount = statistics.masteredWords,
                                    isLoading = false,
                                    localDataReady = true,
                                    errorMessage = null,
                                )
                            }
                        }
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (error: Throwable) {
                        mutableUiState.update {
                            it.copy(
                                isLoading = false,
                                localDataReady = false,
                                errorMessage = error.userFacingMessage(),
                            )
                        }
                    }
                }
        }

        private fun observeSettings() {
            viewModelScope.launch {
                try {
                    settingsRepository.observeSettings().collect { settings ->
                        mutableUiState.update { current ->
                            current.copy(
                                sessionLength = settings.sessionLength,
                                difficulty = settings.difficulty,
                                hasLoadedSettings = true,
                            )
                        }
                    }
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: Throwable) {
                    mutableUiState.update { it.copy(hasLoadedSettings = true) }
                }
            }
        }

        private fun observeSavedEnrichment() {
            enrichmentCacheJob?.cancel()
            mutableUiState.update {
                it.copy(
                    isLoadingEnrichmentCache = true,
                    cacheReadError = false,
                )
            }
            enrichmentCacheJob =
                viewModelScope.launch {
                    try {
                        dictionaryEnrichmentRepository.observeSavedEnrichment().collect { savedEntries ->
                            val observedAt = timeProvider.nowEpochMillis()
                            mutableUiState.update { current ->
                                current.copy(
                                    enrichmentCards = savedEntries.map { it.toUiCard(observedAt) },
                                    lastSuccessfulRefreshAtEpochMillis =
                                        savedEntries.maxOfOrNull(SavedDictionaryEnrichment::fetchedAtEpochMillis),
                                    isLoadingEnrichmentCache = false,
                                    cacheReadError = false,
                                )
                            }
                        }
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (_: Throwable) {
                        mutableUiState.update {
                            it.copy(
                                isLoadingEnrichmentCache = false,
                                cacheReadError = true,
                            )
                        }
                    }
                }
        }

        private fun enrichDeck() {
            val current = mutableUiState.value
            if (!current.enrichEnabled) return

            mutableUiState.update {
                it.copy(
                    isEnriching = true,
                    enrichmentStatus = HomeEnrichmentStatus.Refreshing,
                )
            }
            viewModelScope.launch {
                try {
                    val result =
                        dictionaryEnrichmentRepository.refreshDueWords(
                            nowEpochMillis = timeProvider.nowEpochMillis(),
                        )
                    mutableUiState.update {
                        it.copy(
                            isEnriching = false,
                            enrichmentStatus = result.toUiStatus(),
                        )
                    }
                    if (mutableUiState.value.cacheReadError) {
                        observeSavedEnrichment()
                    }
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: Throwable) {
                    mutableUiState.update {
                        it.copy(
                            isEnriching = false,
                            enrichmentStatus =
                                HomeEnrichmentStatus.Failed(
                                    failedWordCount =
                                        it.dueWordCount
                                            .coerceAtMost(DictionaryEnrichmentRepository.MAX_REFRESH_WORDS)
                                            .coerceAtLeast(1),
                                ),
                        )
                    }
                }
            }
        }

        private fun startPractice() {
            val current = mutableUiState.value
            if (!current.startEnabled) return

            mutableUiState.update { it.copy(isStarting = true, errorMessage = null) }
            viewModelScope.launch {
                try {
                    val sessionId =
                        startPracticeSession(
                            plannedWordCount = mutableUiState.value.sessionLength,
                            difficulty = mutableUiState.value.difficulty,
                        )
                    mutableUiState.update { it.copy(isStarting = false) }
                    effectChannel.send(HomeEffect.OpenPractice(sessionId))
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (error: Throwable) {
                    mutableUiState.update {
                        it.copy(
                            isStarting = false,
                            errorMessage = error.userFacingMessage(),
                        )
                    }
                }
            }
        }

        private fun Throwable.userFacingMessage(): String =
            message?.takeIf(String::isNotBlank) ?: "Local learning data is temporarily unavailable."
    }

private fun SavedDictionaryEnrichment.toUiCard(nowEpochMillis: Long): HomeEnrichmentCard =
    HomeEnrichmentCard(
        wordId = wordId,
        displayWord = displayWord,
        senses =
            senses.map { sense ->
                HomeEnrichmentSense(
                    partOfSpeech = sense.partOfSpeech,
                    definition = sense.definition,
                    example = sense.example,
                    phonetic = sense.phonetic,
                    provider = sense.provider,
                    source = sense.source,
                )
            },
        fetchedAtEpochMillis = fetchedAtEpochMillis,
        isStale = !isDictionaryEnrichmentFresh(fetchedAtEpochMillis, nowEpochMillis),
    )

private fun DictionaryRefreshResult.toUiStatus(): HomeEnrichmentStatus =
    when {
        selectedWordCount == 0 -> {
            HomeEnrichmentStatus.NoDueWords
        }

        failures.isEmpty() -> {
            HomeEnrichmentStatus.Saved(
                refreshedWordCount = refreshedWordCount,
                freshWordCount = freshWordCount,
            )
        }

        refreshedWordCount + freshWordCount > 0 -> {
            HomeEnrichmentStatus.PartialFailure(
                refreshedWordCount = refreshedWordCount,
                freshWordCount = freshWordCount,
                failedWordCount = failures.size,
            )
        }

        else -> {
            HomeEnrichmentStatus.Failed(failedWordCount = failures.size)
        }
    }
