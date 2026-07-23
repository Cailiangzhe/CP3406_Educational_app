package com.cailiangzhe.lexidue.domain.usecase

import com.cailiangzhe.lexidue.domain.model.LearningCard
import com.cailiangzhe.lexidue.domain.model.PracticeDifficulty
import com.cailiangzhe.lexidue.domain.model.PracticeSession
import com.cailiangzhe.lexidue.domain.model.QuestionMode
import com.cailiangzhe.lexidue.domain.model.QuestionType
import com.cailiangzhe.lexidue.domain.model.SessionQuestion
import com.cailiangzhe.lexidue.domain.model.SessionStatus
import com.cailiangzhe.lexidue.domain.model.Word
import com.cailiangzhe.lexidue.domain.repository.PracticeSessionRepository
import com.cailiangzhe.lexidue.domain.repository.WordRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class StartPracticeSession
    @Inject
    constructor(
        private val wordRepository: WordRepository,
        private val practiceSessionRepository: PracticeSessionRepository,
        private val timeProvider: TimeProvider,
        private val idProvider: IdProvider,
    ) {
        suspend operator fun invoke(
            plannedWordCount: Int,
            difficulty: PracticeDifficulty,
        ): String {
            require(plannedWordCount > 0) { "Planned word count must be positive" }

            val startedAt = timeProvider.nowEpochMillis()
            wordRepository.importStarterDeck(startedAt)

            val deck = wordRepository.observeDeck().first()
            val cardsByWordId =
                deck
                    .mapNotNull { word -> word.toLearningCardOrNull()?.let { word.id to it } }
                    .toMap()
            val usableDeck = deck.filter { it.id in cardsByWordId }
            check(usableDeck.size >= plannedWordCount) {
                "Not enough reviewed words are available for a $plannedWordCount-word session"
            }

            val dueWords =
                wordRepository
                    .getDueWords(startedAt, plannedWordCount)
                    .filter { it.id in cardsByWordId }
            val selectedWords = selectDueThenFill(dueWords, usableDeck, plannedWordCount)
            check(selectedWords.size == plannedWordCount) {
                "Unable to select $plannedWordCount unique reviewed words"
            }

            val sessionId = idProvider.nextNonBlankId()
            val randomSeed = stableSeed(sessionId, startedAt)
            val questionGenerator = GenerateQuizQuestion(SeededRandomProvider(randomSeed))
            val candidateCards = usableDeck.map { cardsByWordId.getValue(it.id) }
            val optionCount =
                if (difficulty == PracticeDifficulty.FOUNDATION) {
                    FOUNDATION_OPTION_COUNT
                } else {
                    STANDARD_OPTION_COUNT
                }
            val questions =
                selectedWords.mapIndexed { index, word ->
                    val mode =
                        if (index % 2 == 0) {
                            QuestionMode.WORD_TO_MEANING
                        } else {
                            QuestionMode.MEANING_TO_WORD
                        }
                    val questionId = idProvider.nextNonBlankId()
                    val generated =
                        checkNotNull(
                            questionGenerator(
                                target = cardsByWordId.getValue(word.id),
                                deck = candidateCards,
                                mode = mode,
                                optionCount = optionCount,
                                questionId = questionId,
                            ),
                        ) {
                            "The reviewed deck cannot provide enough compatible distractors for ${word.displaySpelling}"
                        }
                    SessionQuestion(
                        id = generated.id,
                        sessionId = sessionId,
                        sequence = index,
                        wordId = word.id,
                        questionType =
                            when (generated.mode) {
                                QuestionMode.WORD_TO_MEANING -> QuestionType.WORD_TO_DEFINITION
                                QuestionMode.MEANING_TO_WORD -> QuestionType.DEFINITION_TO_WORD
                            },
                        prompt = generated.prompt,
                        optionIds = generated.options.map { it.id },
                        correctOptionId = generated.correctOptionId,
                    )
                }

            practiceSessionRepository.createSession(
                session =
                    PracticeSession(
                        id = sessionId,
                        difficulty = difficulty,
                        randomSeed = randomSeed,
                        plannedWordCount = plannedWordCount,
                        status = SessionStatus.ACTIVE,
                        correctCount = 0,
                        currentQuestionId = questions.first().id,
                        startedAtEpochMillis = startedAt,
                        endedAtEpochMillis = null,
                    ),
                questions = questions,
            )
            return sessionId
        }

        private fun selectDueThenFill(
            dueWords: List<Word>,
            deck: List<Word>,
            count: Int,
        ): List<Word> {
            val selected = LinkedHashMap<String, Word>(count)
            dueWords.forEach { word ->
                if (selected.size < count) selected.putIfAbsent(word.id, word)
            }
            deck.forEach { word ->
                if (selected.size < count) selected.putIfAbsent(word.id, word)
            }
            return selected.values.toList()
        }

        private fun Word.toLearningCardOrNull(): LearningCard? {
            val meaning = canonicalMeanings.firstOrNull() ?: return null
            return LearningCard(
                id = id,
                word = displaySpelling,
                meaning = meaning.definition,
                partOfSpeech = meaning.partOfSpeech.name,
            )
        }

        private fun IdProvider.nextNonBlankId(): String =
            newId().also { require(it.isNotBlank()) { "Generated identifiers cannot be blank" } }

        private fun stableSeed(
            sessionId: String,
            startedAtEpochMillis: Long,
        ): Long {
            var hash = SEED_INITIAL_VALUE
            sessionId.forEach { character -> hash = hash * SEED_MULTIPLIER + character.code }
            return hash xor startedAtEpochMillis
        }

        private companion object {
            const val FOUNDATION_OPTION_COUNT = 3
            const val STANDARD_OPTION_COUNT = 4
            const val SEED_INITIAL_VALUE = 1_125_899_906_842_597L
            const val SEED_MULTIPLIER = 31L
        }
    }
