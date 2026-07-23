package com.cailiangzhe.lexidue.domain.usecase

import com.cailiangzhe.lexidue.domain.model.LearningCard
import com.cailiangzhe.lexidue.domain.model.QuestionMode
import com.cailiangzhe.lexidue.domain.model.QuizOption
import com.cailiangzhe.lexidue.domain.model.QuizQuestion
import com.cailiangzhe.lexidue.domain.model.normalizedForComparison

/**
 * Builds a scored question only when every distractor is unique and matches the target's part of
 * speech. Returning null is the safe fallback for a deck that cannot satisfy those invariants.
 */
class GenerateQuizQuestion(
    private val randomProvider: RandomProvider,
) {
    operator fun invoke(
        target: LearningCard,
        deck: Collection<LearningCard>,
        mode: QuestionMode,
        optionCount: Int = DEFAULT_OPTION_COUNT,
        questionId: String = "${target.id}:${mode.name.lowercase()}",
    ): QuizQuestion? {
        require(optionCount >= MINIMUM_OPTION_COUNT) {
            "A multiple-choice question needs at least two options."
        }
        require(questionId.isNotBlank()) { "A question identifier cannot be blank." }

        val correctText = target.answerText(mode).trim()
        val compatibleDistractors =
            deck
                .asSequence()
                .filter { it.id != target.id }
                .filter {
                    it.partOfSpeech.normalizedForComparison() ==
                        target.partOfSpeech.normalizedForComparison()
                }.map { QuizOption(id = it.id, text = it.answerText(mode).trim()) }
                .filter { it.text.normalizedForComparison() != correctText.normalizedForComparison() }
                .distinctBy { it.id }
                .distinctBy { it.text.normalizedForComparison() }
                .toList()
                .shuffledWith(randomProvider)

        val requiredDistractorCount = optionCount - 1
        if (compatibleDistractors.size < requiredDistractorCount) return null

        val options =
            (
                compatibleDistractors.take(requiredDistractorCount) +
                    QuizOption(id = target.id, text = correctText)
            ).shuffledWith(randomProvider)

        return QuizQuestion(
            id = questionId,
            sourceCardId = target.id,
            mode = mode,
            prompt = target.promptText(mode).trim(),
            partOfSpeech = target.partOfSpeech.trim(),
            options = options,
            correctOptionId = target.id,
        )
    }

    private fun LearningCard.promptText(mode: QuestionMode): String =
        when (mode) {
            QuestionMode.WORD_TO_MEANING -> word
            QuestionMode.MEANING_TO_WORD -> meaning
        }

    private fun LearningCard.answerText(mode: QuestionMode): String =
        when (mode) {
            QuestionMode.WORD_TO_MEANING -> meaning
            QuestionMode.MEANING_TO_WORD -> word
        }

    private fun <T> List<T>.shuffledWith(randomProvider: RandomProvider): List<T> {
        val result = toMutableList()
        for (index in result.lastIndex downTo 1) {
            val otherIndex = randomProvider.nextInt(index + 1)
            val value = result[index]
            result[index] = result[otherIndex]
            result[otherIndex] = value
        }
        return result
    }

    companion object {
        const val DEFAULT_OPTION_COUNT = 4
        private const val MINIMUM_OPTION_COUNT = 2
    }
}
