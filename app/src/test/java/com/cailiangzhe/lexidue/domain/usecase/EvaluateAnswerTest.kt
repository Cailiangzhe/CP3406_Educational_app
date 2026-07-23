package com.cailiangzhe.lexidue.domain.usecase

import com.cailiangzhe.lexidue.domain.model.AnswerAction
import com.cailiangzhe.lexidue.domain.model.AnswerOutcome
import com.cailiangzhe.lexidue.domain.model.QuestionMode
import com.cailiangzhe.lexidue.domain.model.QuizOption
import com.cailiangzhe.lexidue.domain.model.QuizQuestion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class EvaluateAnswerTest {
    private val question = question("question-1")
    private val evaluate = EvaluateAnswer()

    @Test
    fun submittedAnswers_areGradedAgainstTheOptionIdentifier() {
        val correct = evaluate(question, AnswerAction.Submit("correct"))
        val incorrect = evaluate(question, AnswerAction.Submit("other"), isRetry = true)

        assertEquals(AnswerOutcome.CORRECT, correct.outcome)
        assertEquals("correct", correct.selectedOptionId)
        assertTrue(correct.outcome.isGraded)
        assertEquals(AnswerOutcome.INCORRECT, incorrect.outcome)
        assertTrue(incorrect.isRetry)
    }

    @Test
    fun skipAndExit_areExplicitlyUnscored() {
        val skipped = evaluate(question, AnswerAction.Skip)
        val exited = evaluate(question, AnswerAction.Exit)

        assertEquals(AnswerOutcome.SKIPPED, skipped.outcome)
        assertEquals(AnswerOutcome.EXITED, exited.outcome)
        assertFalse(skipped.outcome.isGraded)
        assertFalse(exited.outcome.isGraded)
        assertNull(skipped.selectedOptionId)
        assertNull(exited.selectedOptionId)
    }

    @Test
    fun optionFromAnotherQuestion_isRejectedInsteadOfBeingScoredIncorrect() {
        assertThrows(IllegalArgumentException::class.java) {
            evaluate(question, AnswerAction.Submit("not-an-option"))
        }
    }
}

private fun question(id: String): QuizQuestion =
    QuizQuestion(
        id = id,
        sourceCardId = id.removePrefix("question-"),
        mode = QuestionMode.WORD_TO_MEANING,
        prompt = "prompt",
        partOfSpeech = "noun",
        options = listOf(QuizOption("correct", "correct text"), QuizOption("other", "other text")),
        correctOptionId = "correct",
    )
