package com.cailiangzhe.lexidue.feature.practice

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.cailiangzhe.lexidue.domain.model.AttemptOutcome
import com.cailiangzhe.lexidue.domain.model.SessionSnapshot
import com.cailiangzhe.lexidue.domain.model.SessionStatus
import com.cailiangzhe.lexidue.domain.repository.PracticeSessionRepository
import com.cailiangzhe.lexidue.domain.repository.WordRepository
import com.cailiangzhe.lexidue.navigation.PracticeSummaryRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import java.text.DateFormat
import java.util.Date
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class PracticeSummaryViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val sessionRepository: PracticeSessionRepository,
        private val wordRepository: WordRepository,
    ) : ViewModel() {
        private val sessionId = savedStateHandle.summarySessionId()

        val uiState =
            summaryStateFlow()
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.Eagerly,
                    initialValue = PracticeSummaryUiState(sessionId = sessionId.orEmpty()),
                )

        private fun summaryStateFlow(): Flow<PracticeSummaryUiState> {
            val id = sessionId
            if (id == null) {
                return flowOf(
                    PracticeSummaryUiState(
                        isLoading = false,
                        errorMessage = MISSING_SESSION_ID_MESSAGE,
                    ),
                )
            }

            return sessionRepository
                .observeSession(id)
                .mapLatest { snapshot -> snapshot?.toSummaryState() ?: missingSessionState(id) }
                .catch {
                    emit(
                        PracticeSummaryUiState(
                            isLoading = false,
                            sessionId = id,
                            errorMessage = LOAD_FAILED_MESSAGE,
                        ),
                    )
                }
        }

        private suspend fun SessionSnapshot.toSummaryState(): PracticeSummaryUiState {
            if (session.status == SessionStatus.ACTIVE) {
                return PracticeSummaryUiState(
                    isLoading = false,
                    sessionId = session.id,
                    errorMessage = ACTIVE_SESSION_MESSAGE,
                )
            }

            val attempts = questions.mapNotNull { it.attempt }
            val correctCount = attempts.count { it.outcome == AttemptOutcome.CORRECT }
            val incorrectCount = attempts.count { it.outcome == AttemptOutcome.INCORRECT }
            val scoredCount = correctCount + incorrectCount
            val incorrectWordIds =
                attempts
                    .asSequence()
                    .filter { it.outcome == AttemptOutcome.INCORRECT }
                    .map { it.wordId }
                    .distinct()
                    .toList()
            val wordsById = wordRepository.getWordsByIds(incorrectWordIds).associateBy { it.id }
            val reviewWords =
                incorrectWordIds.map { wordId ->
                    wordsById[wordId]?.displaySpelling ?: wordId.substringAfter(':', wordId)
                }

            return PracticeSummaryUiState(
                isLoading = false,
                sessionId = session.id,
                plannedWordCount = session.plannedWordCount,
                correctCount = correctCount,
                incorrectCount = incorrectCount,
                skippedCount = attempts.count { it.outcome == AttemptOutcome.SKIPPED },
                retryCount = attempts.count { it.isRetry },
                accuracyPercent =
                    if (scoredCount == 0) {
                        0
                    } else {
                        (correctCount * PERCENT_SCALE.toDouble() / scoredCount).roundToInt()
                    },
                completedAtLabel = session.endedAtEpochMillis?.toSummaryTimestamp(),
                reviewWords = reviewWords,
            )
        }

        private fun missingSessionState(sessionId: String): PracticeSummaryUiState =
            PracticeSummaryUiState(
                isLoading = false,
                sessionId = sessionId,
                errorMessage = SESSION_NOT_FOUND_MESSAGE,
            )

        private fun Long.toSummaryTimestamp(): String =
            DateFormat
                .getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                .format(Date(this))

        private companion object {
            const val PERCENT_SCALE = 100
            const val MISSING_SESSION_ID_MESSAGE = "The practice summary has no session identifier."
            const val SESSION_NOT_FOUND_MESSAGE = "This practice session could not be found."
            const val ACTIVE_SESSION_MESSAGE = "This practice session is still active."
            const val LOAD_FAILED_MESSAGE = "The practice summary could not be loaded."
        }
    }

private fun SavedStateHandle.summarySessionId(): String? =
    get<String>(SESSION_ID_KEY)?.takeIf { it.isNotBlank() }
        ?: runCatching { toRoute<PracticeSummaryRoute>().sessionId }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }

private const val SESSION_ID_KEY = "sessionId"
