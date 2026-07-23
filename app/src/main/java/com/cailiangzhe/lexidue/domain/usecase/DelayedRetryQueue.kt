package com.cailiangzhe.lexidue.domain.usecase

import com.cailiangzhe.lexidue.domain.model.AnswerEvaluation
import com.cailiangzhe.lexidue.domain.model.AnswerOutcome
import com.cailiangzhe.lexidue.domain.model.QuizQuestion

/** FIFO retry queue that never releases a question before enough other questions were shown. */
class DelayedRetryQueue(
    private val minimumInterveningQuestions: Int = DEFAULT_MINIMUM_INTERVENING_QUESTIONS,
) {
    private data class PendingRetry(
        val question: QuizQuestion,
        val eligibleAtPresentation: Long,
    )

    private val pendingRetries = mutableListOf<PendingRetry>()
    private val latestPresentationByQuestionId = mutableMapOf<String, Long>()
    private var presentationCount = 0L

    val pendingCount: Int
        get() = pendingRetries.size

    init {
        require(minimumInterveningQuestions >= 2) {
            "A delayed retry needs at least two intervening questions."
        }
    }

    /** Record every displayed question, including a retry, before its answer is handled. */
    fun recordQuestionPresented(question: QuizQuestion) {
        presentationCount += 1
        latestPresentationByQuestionId[question.id] = presentationCount
    }

    /** Returns false for a non-incorrect result or when the retry is already pending. */
    fun scheduleRetry(
        question: QuizQuestion,
        evaluation: AnswerEvaluation,
    ): Boolean {
        require(evaluation.questionId == question.id && evaluation.cardId == question.sourceCardId) {
            "The answer evaluation must belong to the question being scheduled."
        }
        if (evaluation.outcome != AnswerOutcome.INCORRECT) return false

        val lastPresented =
            requireNotNull(latestPresentationByQuestionId[question.id]) {
                "A question must be presented before it can be scheduled for retry."
            }
        if (pendingRetries.any { it.question.id == question.id }) return false

        pendingRetries +=
            PendingRetry(
                question = question,
                eligibleAtPresentation = lastPresented + minimumInterveningQuestions,
            )
        return true
    }

    fun pollEligibleRetry(): QuizQuestion? {
        val index =
            pendingRetries.indexOfFirst {
                presentationCount >= it.eligibleAtPresentation
            }
        if (index == -1) return null
        return pendingRetries.removeAt(index).question
    }

    companion object {
        const val DEFAULT_MINIMUM_INTERVENING_QUESTIONS = 2
    }
}
