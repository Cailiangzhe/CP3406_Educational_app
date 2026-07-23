package com.cailiangzhe.lexidue.testing

import com.cailiangzhe.lexidue.domain.model.Attempt
import com.cailiangzhe.lexidue.domain.model.CanonicalMeaning
import com.cailiangzhe.lexidue.domain.model.LearningStatistics
import com.cailiangzhe.lexidue.domain.model.PartOfSpeech
import com.cailiangzhe.lexidue.domain.model.PracticeSession
import com.cailiangzhe.lexidue.domain.model.RecordAttemptResult
import com.cailiangzhe.lexidue.domain.model.ReviewBoxCount
import com.cailiangzhe.lexidue.domain.model.SessionQuestion
import com.cailiangzhe.lexidue.domain.model.SessionQuestionState
import com.cailiangzhe.lexidue.domain.model.SessionSnapshot
import com.cailiangzhe.lexidue.domain.model.SessionStatus
import com.cailiangzhe.lexidue.domain.model.StarterDeckImportResult
import com.cailiangzhe.lexidue.domain.model.Word
import com.cailiangzhe.lexidue.domain.repository.PracticeSessionRepository
import com.cailiangzhe.lexidue.domain.repository.StatisticsRepository
import com.cailiangzhe.lexidue.domain.repository.WordRepository
import com.cailiangzhe.lexidue.domain.usecase.IdProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

internal class FakeWordRepository(
    words: List<Word>,
    private val dueWordIds: List<String> = words.map(Word::id),
) : WordRepository {
    private val deck = MutableStateFlow(words)
    var importCallCount: Int = 0
        private set

    override fun observeDeck(): Flow<List<Word>> = deck

    override fun observeDueWords(
        nowEpochMillis: Long,
        limit: Int,
    ): Flow<List<Word>> = flowOf(dueWords(limit))

    override suspend fun getDueWords(
        nowEpochMillis: Long,
        limit: Int,
    ): List<Word> = dueWords(limit)

    override suspend fun getWordsByIds(ids: List<String>): List<Word> {
        val byId = deck.value.associateBy(Word::id)
        return ids.mapNotNull(byId::get)
    }

    override suspend fun importStarterDeck(importedAtEpochMillis: Long): StarterDeckImportResult {
        importCallCount += 1
        return StarterDeckImportResult(0, 0, 0)
    }

    private fun dueWords(limit: Int): List<Word> {
        val byId = deck.value.associateBy(Word::id)
        return dueWordIds.mapNotNull(byId::get).take(limit)
    }
}

internal class RecordingPracticeSessionRepository : PracticeSessionRepository {
    var createSessionCallCount: Int = 0
        private set
    var beforeCreate: suspend () -> Unit = {}
    var createdSession: PracticeSession? = null
        private set
    var createdQuestions: List<SessionQuestion> = emptyList()
        private set
    private val snapshot = MutableStateFlow<SessionSnapshot?>(null)

    override suspend fun createSession(
        session: PracticeSession,
        questions: List<SessionQuestion>,
    ) {
        createSessionCallCount += 1
        beforeCreate()
        createdSession = session
        createdQuestions = questions
        snapshot.value =
            SessionSnapshot(
                session = session,
                questions = questions.map { SessionQuestionState(it, attempt = null) },
            )
    }

    override suspend fun getSession(sessionId: String): SessionSnapshot? = snapshot.value?.takeIf { it.session.id == sessionId }

    override fun observeSession(sessionId: String): Flow<SessionSnapshot?> = snapshot

    override suspend fun recordAttempt(
        attempt: Attempt,
        retryQuestion: SessionQuestion?,
    ): RecordAttemptResult = error("recordAttempt is not used by these tests")

    override suspend fun advanceFrom(
        sessionId: String,
        expectedCurrentQuestionId: String,
        nextQuestionId: String?,
        completedAtEpochMillis: Long,
    ): Boolean = false

    override suspend fun updateStatus(
        sessionId: String,
        status: SessionStatus,
        endedAtEpochMillis: Long?,
    ): Boolean = false

    override fun observeRecentSessions(limit: Int): Flow<List<PracticeSession>> = flowOf(emptyList())
}

internal class FakeStatisticsRepository(
    statistics: LearningStatistics,
) : StatisticsRepository {
    val statisticsFlow = MutableStateFlow(statistics)

    override fun observeStatistics(nowEpochMillis: Long): Flow<LearningStatistics> = statisticsFlow

    override fun observeReviewBoxDistribution(): Flow<List<ReviewBoxCount>> = flowOf(emptyList())
}

internal class SequenceIdProvider(
    ids: List<String>,
) : IdProvider {
    private val iterator = ids.iterator()

    override fun newId(): String {
        check(iterator.hasNext()) { "The test did not provide enough identifiers" }
        return iterator.next()
    }
}

internal fun testWord(
    number: Int,
    partOfSpeech: PartOfSpeech = PartOfSpeech.NOUN,
): Word {
    val id = "en:word$number"
    return Word(
        id = id,
        normalizedSpelling = "word$number",
        displaySpelling = "Word $number",
        deckId = "test-deck",
        sourceName = "Test",
        canonicalMeanings =
            listOf(
                CanonicalMeaning(
                    id = "meaning:$number",
                    wordId = id,
                    partOfSpeech = partOfSpeech,
                    definition = "Distinct reviewed meaning number $number.",
                    provenance = "Test",
                ),
            ),
    )
}
