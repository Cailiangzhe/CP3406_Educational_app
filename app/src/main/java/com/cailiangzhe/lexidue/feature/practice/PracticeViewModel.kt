package com.cailiangzhe.lexidue.feature.practice

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cailiangzhe.lexidue.domain.model.Attempt
import com.cailiangzhe.lexidue.domain.model.AttemptOutcome
import com.cailiangzhe.lexidue.domain.model.QuestionType
import com.cailiangzhe.lexidue.domain.model.RecordAttemptResult
import com.cailiangzhe.lexidue.domain.model.SessionQuestion
import com.cailiangzhe.lexidue.domain.model.SessionQuestionState
import com.cailiangzhe.lexidue.domain.model.SessionSnapshot
import com.cailiangzhe.lexidue.domain.model.SessionStatus
import com.cailiangzhe.lexidue.domain.model.Word
import com.cailiangzhe.lexidue.domain.repository.PracticeSessionRepository
import com.cailiangzhe.lexidue.domain.repository.WordRepository
import com.cailiangzhe.lexidue.domain.usecase.TimeProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

@HiltViewModel
class PracticeViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val sessionRepository: PracticeSessionRepository,
        private val wordRepository: WordRepository,
        private val timeProvider: TimeProvider,
    ) : ViewModel() {
        private val sessionId: String =
            requireNotNull(savedStateHandle[SESSION_ID_ARGUMENT]) {
                "Practice requires a session identifier"
            }
        private val _uiState = MutableStateFlow(PracticeUiState(sessionId = sessionId))
        val uiState: StateFlow<PracticeUiState> = _uiState

        private val effectChannel = Channel<PracticeEffect>(capacity = Channel.BUFFERED)
        val effects = effectChannel.receiveAsFlow()

        private val showExitConfirmation = MutableStateFlow(false)
        private val submittingQuestionId = MutableStateFlow<String?>(null)
        private val actionErrorMessage = MutableStateFlow<String?>(null)
        private val reloadRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        private val actionMutex = Mutex()

        private var latestSnapshot: SessionSnapshot? = null
        private var latestDeck: List<Word> = emptyList()
        private var terminalEffectSessionStatus: SessionStatus? = null
        private var recoveringSkippedQuestionId: String? = null

        init {
            observeSavedSession()
        }

        fun onAction(action: PracticeUiAction) {
            when (action) {
                is PracticeUiAction.SelectAnswer -> submitAnswer(action.questionId, action.choiceId)
                is PracticeUiAction.Continue -> continueFrom(action.questionId)
                is PracticeUiAction.Skip -> skipQuestion(action.questionId)
                PracticeUiAction.RequestExit -> showExitConfirmation.value = true
                PracticeUiAction.DismissExit -> showExitConfirmation.value = false
                PracticeUiAction.ConfirmExit -> confirmExit()
                PracticeUiAction.RetryLoad -> refreshFromLatestState()
            }
        }

        private fun observeSavedSession() {
            viewModelScope.launch {
                reloadRequests
                    .onStart { emit(Unit) }
                    .flatMapLatest {
                        combine(
                            sessionRepository.observeSession(sessionId),
                            wordRepository.observeDeck(),
                            showExitConfirmation,
                            submittingQuestionId,
                            actionErrorMessage,
                        ) { snapshot, deck, showExit, submitting, actionError ->
                            latestSnapshot = snapshot
                            latestDeck = deck
                            buildUiState(snapshot, deck, showExit, submitting, actionError)
                        }.catch { error ->
                            if (error is CancellationException) throw error
                            emit(
                                PracticeUiState(
                                    sessionId = sessionId,
                                    content =
                                        PracticeContent.Error(
                                            "The saved session could not be loaded. Try again.",
                                        ),
                                ),
                            )
                        }
                    }.collect { state ->
                        _uiState.value = state
                        handleTerminalOrInterruptedState(latestSnapshot)
                    }
            }
        }

        private fun buildUiState(
            snapshot: SessionSnapshot?,
            deck: List<Word>,
            showExit: Boolean,
            submitting: String?,
            actionError: String?,
        ): PracticeUiState {
            if (actionError != null) {
                return PracticeUiState(
                    sessionId = sessionId,
                    content = PracticeContent.Error(actionError),
                    showExitConfirmation = showExit,
                )
            }
            if (snapshot == null) {
                return PracticeUiState(
                    sessionId = sessionId,
                    content = PracticeContent.Error("This saved practice session could not be found."),
                    showExitConfirmation = showExit,
                )
            }
            if (snapshot.session.status != SessionStatus.ACTIVE) {
                return PracticeUiState(
                    sessionId = sessionId,
                    content = PracticeContent.Loading,
                    showExitConfirmation = false,
                )
            }

            val currentQuestionId = snapshot.session.currentQuestionId
            val currentState =
                snapshot.questions.firstOrNull { it.question.id == currentQuestionId }
                    ?: return PracticeUiState(
                        sessionId = sessionId,
                        content = PracticeContent.Error("The current saved question is unavailable."),
                        showExitConfirmation = showExit,
                    )
            val questionUi =
                currentState.toUiQuestion(snapshot, deck)
                    ?: return PracticeUiState(
                        sessionId = sessionId,
                        content = PracticeContent.Error("Saved answer choices could not be reconstructed."),
                        showExitConfirmation = showExit,
                    )
            val content =
                when (val attempt = currentState.attempt) {
                    null -> {
                        PracticeContent.Question(
                            question = questionUi,
                            answersEnabled = submitting != currentQuestionId,
                        )
                    }

                    else -> {
                        when (attempt.outcome) {
                            AttemptOutcome.SKIPPED -> {
                                PracticeContent.Loading
                            }

                            AttemptOutcome.CORRECT,
                            AttemptOutcome.INCORRECT,
                            -> {
                                PracticeContent.Feedback(
                                    question = questionUi,
                                    selectedChoiceId = attempt.selectedOptionId,
                                    feedback =
                                        PracticeFeedback(
                                            kind =
                                                if (attempt.outcome == AttemptOutcome.CORRECT) {
                                                    PracticeFeedbackKind.CORRECT
                                                } else {
                                                    PracticeFeedbackKind.INCORRECT
                                                },
                                            message = feedbackMessage(currentState.question, deck),
                                            correctChoiceId = currentState.question.correctOptionId,
                                        ),
                                )
                            }
                        }
                    }
                }

            return PracticeUiState(
                sessionId = sessionId,
                content = content,
                showExitConfirmation = showExit,
            )
        }

        private fun SessionQuestionState.toUiQuestion(
            snapshot: SessionSnapshot,
            deck: List<Word>,
        ): PracticeQuestionUi? {
            val wordById = deck.associateBy(Word::id)
            val choices =
                question.optionIds.map { optionId ->
                    val word = wordById[optionId] ?: return null
                    val text =
                        when (question.questionType) {
                            QuestionType.WORD_TO_DEFINITION -> {
                                word.canonicalMeanings.firstOrNull()?.definition ?: return null
                            }

                            QuestionType.DEFINITION_TO_WORD -> {
                                word.displaySpelling
                            }
                        }
                    PracticeChoice(id = optionId, text = text)
                }
            val orderedQuestions = snapshot.questions.sortedBy { it.question.sequence }
            val questionIndex = orderedQuestions.indexOfFirst { it.question.id == question.id }
            if (questionIndex < 0) return null
            return PracticeQuestionUi(
                id = question.id,
                questionNumber = questionIndex + 1,
                totalQuestions = orderedQuestions.size,
                prompt = question.prompt,
                mode =
                    when (question.questionType) {
                        QuestionType.WORD_TO_DEFINITION -> PracticePromptMode.WORD_TO_DEFINITION
                        QuestionType.DEFINITION_TO_WORD -> PracticePromptMode.DEFINITION_TO_WORD
                    },
                choices = choices,
                isRetry = question.retryOfQuestionId != null,
            )
        }

        private fun submitAnswer(
            questionId: String,
            choiceId: String,
        ) {
            viewModelScope.launch {
                actionMutex.withLock {
                    val snapshot = currentActiveSnapshot(questionId) ?: return@withLock
                    val current = snapshot.currentQuestionState(questionId) ?: return@withLock
                    if (current.attempt != null || choiceId !in current.question.optionIds) return@withLock

                    submittingQuestionId.value = questionId
                    try {
                        val outcome =
                            if (choiceId == current.question.correctOptionId) {
                                AttemptOutcome.CORRECT
                            } else {
                                AttemptOutcome.INCORRECT
                            }
                        val retry =
                            if (outcome == AttemptOutcome.INCORRECT) {
                                buildDelayedRetry(snapshot, current.question)
                            } else {
                                null
                            }
                        sessionRepository.recordAttempt(
                            attempt =
                                Attempt(
                                    id = stableAttemptId(questionId),
                                    questionId = questionId,
                                    sessionId = sessionId,
                                    wordId = current.question.wordId,
                                    selectedOptionId = choiceId,
                                    outcome = outcome,
                                    isRetry = current.question.retryOfQuestionId != null,
                                    answeredAtEpochMillis = timeProvider.nowEpochMillis(),
                                ),
                            retryQuestion = retry,
                        )
                    } catch (error: Throwable) {
                        handleActionError(error)
                    } finally {
                        submittingQuestionId.value = null
                    }
                }
            }
        }

        private fun skipQuestion(questionId: String) {
            viewModelScope.launch {
                actionMutex.withLock {
                    val snapshot = currentActiveSnapshot(questionId) ?: return@withLock
                    val current = snapshot.currentQuestionState(questionId) ?: return@withLock
                    if (current.attempt != null) return@withLock

                    submittingQuestionId.value = questionId
                    try {
                        sessionRepository.recordAttempt(
                            Attempt(
                                id = stableAttemptId(questionId),
                                questionId = questionId,
                                sessionId = sessionId,
                                wordId = current.question.wordId,
                                selectedOptionId = null,
                                outcome = AttemptOutcome.SKIPPED,
                                isRetry = current.question.retryOfQuestionId != null,
                                answeredAtEpochMillis = timeProvider.nowEpochMillis(),
                            ),
                        )
                        advanceSavedSession(snapshot, current.question)
                    } catch (error: Throwable) {
                        handleActionError(error)
                    } finally {
                        submittingQuestionId.value = null
                    }
                }
            }
        }

        private fun continueFrom(questionId: String) {
            viewModelScope.launch {
                actionMutex.withLock {
                    val snapshot = currentActiveSnapshot(questionId) ?: return@withLock
                    val current = snapshot.currentQuestionState(questionId) ?: return@withLock
                    if (current.attempt == null) return@withLock
                    try {
                        advanceSavedSession(snapshot, current.question)
                    } catch (error: Throwable) {
                        handleActionError(error)
                    }
                }
            }
        }

        private suspend fun advanceSavedSession(
            snapshot: SessionSnapshot,
            currentQuestion: SessionQuestion,
        ) {
            val nextQuestion =
                snapshot.questions
                    .asSequence()
                    .filter { it.question.sequence > currentQuestion.sequence }
                    .filter { it.attempt == null }
                    .minByOrNull { it.question.sequence }
                    ?.question
            val advanced =
                sessionRepository.advanceFrom(
                    sessionId = sessionId,
                    expectedCurrentQuestionId = currentQuestion.id,
                    nextQuestionId = nextQuestion?.id,
                    completedAtEpochMillis = timeProvider.nowEpochMillis(),
                )
            if (advanced && nextQuestion == null) {
                emitTerminalEffect(SessionStatus.COMPLETED)
            }
        }

        private fun buildDelayedRetry(
            snapshot: SessionSnapshot,
            original: SessionQuestion,
        ): SessionQuestion? {
            if (original.retryOfQuestionId != null) return null
            if (snapshot.questions.any { it.question.retryOfQuestionId == original.id }) return null
            val laterUnanswered =
                snapshot.questions.count {
                    it.question.sequence > original.sequence && it.attempt == null
                }
            if (laterUnanswered < MINIMUM_INTERVENING_QUESTIONS) return null
            val nextSequence = snapshot.questions.maxOf { it.question.sequence } + 1
            return original.copy(
                id = "${original.id}:retry",
                sequence = nextSequence,
                retryOfQuestionId = original.id,
            )
        }

        private fun confirmExit() {
            viewModelScope.launch {
                actionMutex.withLock {
                    showExitConfirmation.value = false
                    try {
                        val changed =
                            sessionRepository.updateStatus(
                                sessionId = sessionId,
                                status = SessionStatus.ABANDONED,
                                endedAtEpochMillis = timeProvider.nowEpochMillis(),
                            )
                        if (changed) emitTerminalEffect(SessionStatus.ABANDONED)
                    } catch (error: Throwable) {
                        handleActionError(error)
                    }
                }
            }
        }

        private fun handleTerminalOrInterruptedState(snapshot: SessionSnapshot?) {
            when (snapshot?.session?.status) {
                SessionStatus.COMPLETED -> {
                    emitTerminalEffect(SessionStatus.COMPLETED)
                }

                SessionStatus.ABANDONED -> {
                    emitTerminalEffect(SessionStatus.ABANDONED)
                }

                SessionStatus.ACTIVE -> {
                    val current =
                        snapshot.questions.firstOrNull {
                            it.question.id == snapshot.session.currentQuestionId
                        }
                    if (
                        current?.attempt?.outcome == AttemptOutcome.SKIPPED &&
                        recoveringSkippedQuestionId != current.question.id
                    ) {
                        recoveringSkippedQuestionId = current.question.id
                        viewModelScope.launch {
                            actionMutex.withLock {
                                try {
                                    advanceSavedSession(snapshot, current.question)
                                } catch (error: Throwable) {
                                    handleActionError(error)
                                } finally {
                                    recoveringSkippedQuestionId = null
                                }
                            }
                        }
                    }
                }

                null -> {
                    Unit
                }
            }
        }

        private fun emitTerminalEffect(status: SessionStatus) {
            if (terminalEffectSessionStatus == status) return
            terminalEffectSessionStatus = status
            when (status) {
                SessionStatus.COMPLETED -> effectChannel.trySend(PracticeEffect.OpenSummary(sessionId))
                SessionStatus.ABANDONED -> effectChannel.trySend(PracticeEffect.ReturnHome)
                SessionStatus.ACTIVE -> Unit
            }
        }

        private fun currentActiveSnapshot(questionId: String): SessionSnapshot? {
            val snapshot = latestSnapshot ?: return null
            if (snapshot.session.status != SessionStatus.ACTIVE) return null
            if (snapshot.session.currentQuestionId != questionId) return null
            return snapshot
        }

        private fun SessionSnapshot.currentQuestionState(questionId: String): SessionQuestionState? =
            questions.firstOrNull { it.question.id == questionId }

        private fun stableAttemptId(questionId: String): String = "$sessionId:$questionId:attempt"

        private fun feedbackMessage(
            question: SessionQuestion,
            deck: List<Word>,
        ): String {
            val word = deck.firstOrNull { it.id == question.wordId }
            val spelling = word?.displaySpelling ?: question.prompt
            val meaning = word?.canonicalMeanings?.firstOrNull()?.definition
            return if (meaning == null) spelling else "$spelling — $meaning"
        }

        private fun handleActionError(error: Throwable) {
            if (error is CancellationException) throw error
            actionErrorMessage.value = error.message ?: "The learning update could not be saved."
        }

        private fun refreshFromLatestState() {
            actionErrorMessage.value = null
            reloadRequests.tryEmit(Unit)
        }

        private companion object {
            const val SESSION_ID_ARGUMENT = "sessionId"
            const val MINIMUM_INTERVENING_QUESTIONS = 2
        }
    }
