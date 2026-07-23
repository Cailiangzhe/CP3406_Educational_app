package com.cailiangzhe.lexidue.domain.model

sealed interface AnswerAction {
    data class Submit(
        val optionId: String,
    ) : AnswerAction {
        init {
            require(optionId.isNotBlank()) { "A submitted option identifier cannot be blank." }
        }
    }

    data object Skip : AnswerAction

    data object Exit : AnswerAction
}

enum class AnswerOutcome {
    CORRECT,
    INCORRECT,
    SKIPPED,
    EXITED,
    ;

    val isGraded: Boolean
        get() = this == CORRECT || this == INCORRECT
}

data class AnswerEvaluation(
    val questionId: String,
    val cardId: String,
    val outcome: AnswerOutcome,
    val selectedOptionId: String? = null,
    val isRetry: Boolean = false,
) {
    init {
        require(questionId.isNotBlank()) { "An evaluation must reference a question." }
        require(cardId.isNotBlank()) { "An evaluation must reference a learning card." }
        require(
            if (outcome.isGraded) {
                !selectedOptionId.isNullOrBlank()
            } else {
                selectedOptionId == null
            },
        ) { "Only graded outcomes have a selected option." }
    }
}
