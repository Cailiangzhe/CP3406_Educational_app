package com.cailiangzhe.lexidue.domain.usecase

import com.cailiangzhe.lexidue.domain.model.AnswerAction
import com.cailiangzhe.lexidue.domain.model.AnswerEvaluation
import com.cailiangzhe.lexidue.domain.model.AnswerOutcome
import com.cailiangzhe.lexidue.domain.model.QuizQuestion

class EvaluateAnswer {
    operator fun invoke(
        question: QuizQuestion,
        action: AnswerAction,
        isRetry: Boolean = false,
    ): AnswerEvaluation {
        val selectedOptionId = (action as? AnswerAction.Submit)?.optionId
        if (selectedOptionId != null) {
            require(question.options.any { it.id == selectedOptionId }) {
                "The submitted option does not belong to this question."
            }
        }

        val outcome =
            when (action) {
                is AnswerAction.Submit -> {
                    if (action.optionId == question.correctOptionId) {
                        AnswerOutcome.CORRECT
                    } else {
                        AnswerOutcome.INCORRECT
                    }
                }

                AnswerAction.Skip -> {
                    AnswerOutcome.SKIPPED
                }

                AnswerAction.Exit -> {
                    AnswerOutcome.EXITED
                }
            }

        return AnswerEvaluation(
            questionId = question.id,
            cardId = question.sourceCardId,
            outcome = outcome,
            selectedOptionId = selectedOptionId,
            isRetry = isRetry,
        )
    }
}
