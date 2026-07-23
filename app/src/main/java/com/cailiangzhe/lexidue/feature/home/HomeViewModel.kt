package com.cailiangzhe.lexidue.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cailiangzhe.lexidue.domain.model.PracticeDifficulty
import com.cailiangzhe.lexidue.domain.repository.StatisticsRepository
import com.cailiangzhe.lexidue.domain.repository.WordRepository
import com.cailiangzhe.lexidue.domain.usecase.StartPracticeSession
import com.cailiangzhe.lexidue.domain.usecase.TimeProvider
import dagger.hilt.android.lifecycle.HiltViewModel
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
        private val startPracticeSession: StartPracticeSession,
        private val timeProvider: TimeProvider,
    ) : ViewModel() {
        private val mutableUiState = MutableStateFlow(HomeUiState(isLoading = true))
        val uiState: StateFlow<HomeUiState> = mutableUiState.asStateFlow()

        private val effectChannel = Channel<HomeEffect>(capacity = Channel.BUFFERED)
        val effects = effectChannel.receiveAsFlow()

        init {
            observeLocalOverview()
        }

        fun onAction(action: HomeUiAction) {
            when (action) {
                HomeUiAction.StartPractice -> startPractice()
                HomeUiAction.OpenStatistics -> effectChannel.trySend(HomeEffect.OpenStatistics)
                HomeUiAction.OpenSettings -> effectChannel.trySend(HomeEffect.OpenSettings)
                HomeUiAction.EnrichDeck -> Unit
            }
        }

        private fun observeLocalOverview() {
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
                            errorMessage = error.userFacingMessage(),
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
                            difficulty = PracticeDifficulty.STANDARD,
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
