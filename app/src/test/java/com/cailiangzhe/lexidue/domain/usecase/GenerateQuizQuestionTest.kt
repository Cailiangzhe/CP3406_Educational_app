package com.cailiangzhe.lexidue.domain.usecase

import com.cailiangzhe.lexidue.domain.model.LearningCard
import com.cailiangzhe.lexidue.domain.model.QuestionMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GenerateQuizQuestionTest {
    private val deck =
        listOf(
            card("analyse", "analyse", "examine in detail", " Verb "),
            card("assess", "assess", "judge the quality", "verb"),
            card("derive", "derive", "obtain from a source", "VERB"),
            card("establish", "establish", "set up firmly", "verb"),
            card("concept", "concept", "an abstract idea", "noun"),
        )

    @Test
    fun wordToMeaning_usesWordPromptAndUniqueCompatibleMeanings() {
        val question =
            GenerateQuizQuestion(SeededRandomProvider(42))(
                target = deck.first(),
                deck = deck,
                mode = QuestionMode.WORD_TO_MEANING,
            )

        assertNotNull(question)
        requireNotNull(question)
        assertEquals("analyse", question.prompt)
        assertEquals("examine in detail", question.options.single { it.id == "analyse" }.text)
        assertEquals(setOf("analyse", "assess", "derive", "establish"), question.options.map { it.id }.toSet())
        assertEquals(
            4,
            question.options
                .map { it.text.lowercase() }
                .distinct()
                .size,
        )
        assertEquals("analyse", question.correctOptionId)
    }

    @Test
    fun meaningToWord_usesMeaningPromptAndWordOptions() {
        val question =
            requireNotNull(
                GenerateQuizQuestion(SeededRandomProvider(9))(
                    target = deck.first(),
                    deck = deck,
                    mode = QuestionMode.MEANING_TO_WORD,
                    optionCount = 3,
                ),
            )

        assertEquals("examine in detail", question.prompt)
        assertTrue(question.options.any { it.id == "analyse" && it.text == "analyse" })
        assertTrue(question.options.all { it.id != "concept" })
        assertEquals(
            3,
            question.options
                .map { it.text.lowercase() }
                .distinct()
                .size,
        )
    }

    @Test
    fun sameSeedAndInputs_produceTheSameQuestion() {
        val first =
            GenerateQuizQuestion(SeededRandomProvider(1234))(
                target = deck.first(),
                deck = deck,
                mode = QuestionMode.WORD_TO_MEANING,
                optionCount = 3,
            )
        val second =
            GenerateQuizQuestion(SeededRandomProvider(1234))(
                target = deck.first(),
                deck = deck,
                mode = QuestionMode.WORD_TO_MEANING,
                optionCount = 3,
            )

        assertEquals(first, second)
    }

    @Test
    fun insufficientCompatibleDistractors_returnsNullInsteadOfUnsafeOptions() {
        val sparseDeck =
            listOf(
                card("target", "target", "the correct meaning", "noun"),
                card("one", "one", "first alternative", "noun"),
                card("duplicate", "duplicate", " FIRST ALTERNATIVE ", "NOUN"),
                card("wrong-pos", "wrong", "incompatible alternative", "verb"),
            )

        val question =
            GenerateQuizQuestion(SeededRandomProvider(1))(
                target = sparseDeck.first(),
                deck = sparseDeck,
                mode = QuestionMode.WORD_TO_MEANING,
                optionCount = 3,
            )

        assertNull(question)
    }

    @Test
    fun duplicateCardIdentifiers_returnNullInsteadOfCreatingInvalidOptions() {
        val duplicateIds =
            listOf(
                card("target", "target", "correct", "noun"),
                card("duplicate", "one", "first", "noun"),
                card("duplicate", "two", "second", "noun"),
            )

        val question =
            GenerateQuizQuestion(SeededRandomProvider(4))(
                target = duplicateIds.first(),
                deck = duplicateIds,
                mode = QuestionMode.WORD_TO_MEANING,
                optionCount = 3,
            )

        assertNull(question)
    }

    @Test
    fun minimumTwoOptions_usesOneCompatibleDistractor() {
        val sparseDeck =
            listOf(
                card("target", "target", "correct", "noun"),
                card("other", "other", "alternative", "noun"),
            )

        val question =
            GenerateQuizQuestion(SeededRandomProvider(0))(
                target = sparseDeck.first(),
                deck = sparseDeck,
                mode = QuestionMode.WORD_TO_MEANING,
                optionCount = 2,
            )

        assertEquals(2, requireNotNull(question).options.size)
    }

    private fun card(
        id: String,
        word: String,
        meaning: String,
        partOfSpeech: String,
    ) = LearningCard(id, word, meaning, partOfSpeech)
}
