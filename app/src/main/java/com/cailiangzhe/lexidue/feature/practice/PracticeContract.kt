package com.cailiangzhe.lexidue.feature.practice

data class PracticeChoice(
    val id: String,
    val text: String,
)

enum class PracticePromptMode {
    WORD_TO_DEFINITION,
    DEFINITION_TO_WORD,
}

data class PracticeQuestionUi(
    val id: String,
    val questionNumber: Int,
    val totalQuestions: Int,
    val prompt: String,
    val mode: PracticePromptMode,
    val choices: List<PracticeChoice>,
    val isRetry: Boolean = false,
)

enum class PracticeFeedbackKind {
    CORRECT,
    INCORRECT,
}

data class PracticeFeedback(
    val kind: PracticeFeedbackKind,
    val message: String,
    val correctChoiceId: String,
)

sealed interface PracticeContent {
    data object Loading : PracticeContent

    data class Question(
        val question: PracticeQuestionUi,
        val answersEnabled: Boolean = true,
    ) : PracticeContent

    data class Feedback(
        val question: PracticeQuestionUi,
        val selectedChoiceId: String?,
        val feedback: PracticeFeedback,
    ) : PracticeContent

    data class Error(
        val message: String,
    ) : PracticeContent
}

data class PracticeUiState(
    val sessionId: String = "",
    val content: PracticeContent = PracticeContent.Loading,
    val showExitConfirmation: Boolean = false,
)

sealed interface PracticeUiAction {
    data class SelectAnswer(
        val questionId: String,
        val choiceId: String,
    ) : PracticeUiAction

    data class Continue(
        val questionId: String,
    ) : PracticeUiAction

    data class Skip(
        val questionId: String,
    ) : PracticeUiAction

    data object RequestExit : PracticeUiAction

    data object DismissExit : PracticeUiAction

    data object ConfirmExit : PracticeUiAction

    data object RetryLoad : PracticeUiAction
}

sealed interface PracticeEffect {
    data class OpenSummary(
        val sessionId: String,
    ) : PracticeEffect

    data object ReturnHome : PracticeEffect
}
