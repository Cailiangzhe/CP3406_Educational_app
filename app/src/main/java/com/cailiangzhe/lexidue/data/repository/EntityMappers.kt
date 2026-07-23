package com.cailiangzhe.lexidue.data.repository

import com.cailiangzhe.lexidue.data.local.entity.AttemptEntity
import com.cailiangzhe.lexidue.data.local.entity.CanonicalMeaningEntity
import com.cailiangzhe.lexidue.data.local.entity.PracticeSessionEntity
import com.cailiangzhe.lexidue.data.local.entity.ReviewProgressEntity
import com.cailiangzhe.lexidue.data.local.entity.SessionQuestionEntity
import com.cailiangzhe.lexidue.data.local.relation.SessionWithQuestionsEntity
import com.cailiangzhe.lexidue.data.local.relation.WordWithMeaningsEntity
import com.cailiangzhe.lexidue.domain.model.Attempt
import com.cailiangzhe.lexidue.domain.model.CanonicalMeaning
import com.cailiangzhe.lexidue.domain.model.PracticeSession
import com.cailiangzhe.lexidue.domain.model.ReviewProgress
import com.cailiangzhe.lexidue.domain.model.SessionQuestion
import com.cailiangzhe.lexidue.domain.model.SessionQuestionState
import com.cailiangzhe.lexidue.domain.model.SessionSnapshot
import com.cailiangzhe.lexidue.domain.model.Word

internal fun WordWithMeaningsEntity.toDomain(): Word =
    Word(
        id = word.id,
        normalizedSpelling = word.normalizedSpelling,
        displaySpelling = word.displaySpelling,
        deckId = word.deckId,
        sourceName = word.sourceName,
        canonicalMeanings = meanings.sortedBy { it.id }.map(CanonicalMeaningEntity::toDomain),
    )

private fun CanonicalMeaningEntity.toDomain(): CanonicalMeaning =
    CanonicalMeaning(
        id = id,
        wordId = wordId,
        partOfSpeech = partOfSpeech,
        definition = definition,
        provenance = provenance,
    )

internal fun ReviewProgressEntity.toDomain(): ReviewProgress =
    ReviewProgress(
        wordId = wordId,
        reviewBox = reviewBox,
        correctCount = correctCount,
        incorrectCount = incorrectCount,
        nextReviewAtEpochMillis = nextReviewAtEpochMillis,
        updatedAtEpochMillis = updatedAtEpochMillis,
    )

internal fun PracticeSession.toEntity(): PracticeSessionEntity =
    PracticeSessionEntity(
        id = id,
        difficulty = difficulty,
        randomSeed = randomSeed,
        plannedWordCount = plannedWordCount,
        status = status,
        correctCount = correctCount,
        currentQuestionId = currentQuestionId,
        startedAtEpochMillis = startedAtEpochMillis,
        endedAtEpochMillis = endedAtEpochMillis,
    )

internal fun PracticeSessionEntity.toDomain(): PracticeSession =
    PracticeSession(
        id = id,
        difficulty = difficulty,
        randomSeed = randomSeed,
        plannedWordCount = plannedWordCount,
        status = status,
        correctCount = correctCount,
        currentQuestionId = currentQuestionId,
        startedAtEpochMillis = startedAtEpochMillis,
        endedAtEpochMillis = endedAtEpochMillis,
    )

internal fun SessionQuestion.toEntity(): SessionQuestionEntity =
    SessionQuestionEntity(
        id = id,
        sessionId = sessionId,
        sequence = sequence,
        wordId = wordId,
        questionType = questionType,
        prompt = prompt,
        optionIds = optionIds,
        correctOptionId = correctOptionId,
        retryOfQuestionId = retryOfQuestionId,
    )

private fun SessionQuestionEntity.toDomain(): SessionQuestion =
    SessionQuestion(
        id = id,
        sessionId = sessionId,
        sequence = sequence,
        wordId = wordId,
        questionType = questionType,
        prompt = prompt,
        optionIds = optionIds,
        correctOptionId = correctOptionId,
        retryOfQuestionId = retryOfQuestionId,
    )

internal fun Attempt.toEntity(): AttemptEntity =
    AttemptEntity(
        id = id,
        questionId = questionId,
        sessionId = sessionId,
        wordId = wordId,
        selectedOptionId = selectedOptionId,
        outcome = outcome,
        isRetry = isRetry,
        answeredAtEpochMillis = answeredAtEpochMillis,
    )

internal fun AttemptEntity.toDomain(): Attempt =
    Attempt(
        id = id,
        questionId = questionId,
        sessionId = sessionId,
        wordId = wordId,
        selectedOptionId = selectedOptionId,
        outcome = outcome,
        isRetry = isRetry,
        answeredAtEpochMillis = answeredAtEpochMillis,
    )

internal fun SessionWithQuestionsEntity.toDomain(): SessionSnapshot =
    SessionSnapshot(
        session = session.toDomain(),
        questions =
            questions
                .sortedBy { it.question.sequence }
                .map {
                    check(it.attempts.size <= 1) { "A question cannot have multiple attempts" }
                    SessionQuestionState(
                        question = it.question.toDomain(),
                        attempt = it.attempts.singleOrNull()?.toDomain(),
                    )
                },
    )
