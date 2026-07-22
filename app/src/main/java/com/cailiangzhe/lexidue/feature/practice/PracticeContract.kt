package com.cailiangzhe.lexidue.feature.practice

data class PracticeChoice(
    val id: String,
    val text: String,
)

enum class PracticeFeedbackKind {
    CORRECT,
    INCORRECT,
}

data class PracticeFeedback(
    val kind: PracticeFeedbackKind,
    val message: String,
)

data class PracticeUiState(
    val questionNumber: Int = 1,
    val totalQuestions: Int = 10,
    val promptWord: String = "analyse",
    val choices: List<PracticeChoice> =
        listOf(
            PracticeChoice("a", "Examine something carefully and in detail"),
            PracticeChoice("b", "Combine separate parts into one object"),
            PracticeChoice("c", "State that something will happen in the future"),
            PracticeChoice("d", "Make something easier to understand"),
        ),
    val selectedChoiceId: String? = null,
    val feedback: PracticeFeedback? = null,
    val answersEnabled: Boolean = true,
)

sealed interface PracticeUiAction {
    data class SelectAnswer(
        val choiceId: String,
    ) : PracticeUiAction

    data object Skip : PracticeUiAction

    data object Exit : PracticeUiAction
}
