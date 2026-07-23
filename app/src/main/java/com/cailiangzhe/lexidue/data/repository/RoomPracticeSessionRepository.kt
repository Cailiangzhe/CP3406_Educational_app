package com.cailiangzhe.lexidue.data.repository

import androidx.room.withTransaction
import com.cailiangzhe.lexidue.data.local.LexiDueDatabase
import com.cailiangzhe.lexidue.data.local.dao.LearningTransactionDao
import com.cailiangzhe.lexidue.data.local.dao.RecordAttemptTransactionResult
import com.cailiangzhe.lexidue.data.local.dao.SessionDao
import com.cailiangzhe.lexidue.domain.model.Attempt
import com.cailiangzhe.lexidue.domain.model.PracticeSession
import com.cailiangzhe.lexidue.domain.model.RecordAttemptResult
import com.cailiangzhe.lexidue.domain.model.SessionQuestion
import com.cailiangzhe.lexidue.domain.model.SessionSnapshot
import com.cailiangzhe.lexidue.domain.model.SessionStatus
import com.cailiangzhe.lexidue.domain.repository.PracticeSessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomPracticeSessionRepository
    @Inject
    constructor(
        private val database: LexiDueDatabase,
        private val sessionDao: SessionDao,
        private val learningTransactionDao: LearningTransactionDao,
    ) : PracticeSessionRepository {
        override suspend fun createSession(
            session: PracticeSession,
            questions: List<SessionQuestion>,
        ) {
            validateNewSession(session, questions)
            database.withTransaction {
                sessionDao.insertSession(session.toEntity())
                sessionDao.insertQuestions(questions.map { it.toEntity() })
            }
        }

        override suspend fun getSession(sessionId: String): SessionSnapshot? = sessionDao.getSession(sessionId)?.toDomain()

        override fun observeSession(sessionId: String): Flow<SessionSnapshot?> = sessionDao.observeSession(sessionId).map { it?.toDomain() }

        override suspend fun recordAttempt(
            attempt: Attempt,
            retryQuestion: SessionQuestion?,
        ): RecordAttemptResult =
            when (
                val result =
                    learningTransactionDao.recordAttempt(
                        attempt = attempt.toEntity(),
                        retryQuestion = retryQuestion?.toEntity(),
                    )
            ) {
                is RecordAttemptTransactionResult.Recorded -> {
                    RecordAttemptResult.Recorded(
                        attempt = result.attempt.toDomain(),
                        progress = result.progress?.toDomain(),
                    )
                }

                is RecordAttemptTransactionResult.AlreadyRecorded -> {
                    RecordAttemptResult.AlreadyRecorded(result.attempt.toDomain())
                }
            }

        override suspend fun advanceFrom(
            sessionId: String,
            expectedCurrentQuestionId: String,
            nextQuestionId: String?,
            completedAtEpochMillis: Long,
        ): Boolean =
            database.withTransaction {
                if (nextQuestionId != null) {
                    require(sessionDao.questionBelongsToSession(sessionId, nextQuestionId) == 1) {
                        "Next question does not belong to the session"
                    }
                }
                sessionDao.compareAndSetCurrentQuestion(
                    sessionId = sessionId,
                    expectedCurrentQuestionId = expectedCurrentQuestionId,
                    nextQuestionId = nextQuestionId,
                    completedAtEpochMillis = completedAtEpochMillis,
                ) == 1
            }

        override suspend fun updateStatus(
            sessionId: String,
            status: SessionStatus,
            endedAtEpochMillis: Long?,
        ): Boolean {
            require(status != SessionStatus.COMPLETED) {
                "Complete a session by advancing from its final question"
            }
            require(status != SessionStatus.ACTIVE || endedAtEpochMillis == null) {
                "An active session cannot have an end time"
            }
            return sessionDao.updateStatus(sessionId, status, endedAtEpochMillis) == 1
        }

        override fun observeRecentSessions(limit: Int): Flow<List<PracticeSession>> {
            require(limit > 0) { "Recent-session limit must be positive" }
            return sessionDao.observeRecentSessions(limit).map { rows -> rows.map { it.toDomain() } }
        }

        private fun validateNewSession(
            session: PracticeSession,
            questions: List<SessionQuestion>,
        ) {
            require(session.status == SessionStatus.ACTIVE) { "A new session must be active" }
            require(session.correctCount == 0) { "A new session cannot have recorded correct answers" }
            require(session.endedAtEpochMillis == null) { "A new session cannot have an end time" }
            require(session.plannedWordCount > 0) { "Planned word count must be positive" }
            require(questions.isNotEmpty()) { "A session requires at least one question" }
            require(questions.all { it.sessionId == session.id }) {
                "Every question must belong to the new session"
            }
            require(questions.map { it.id }.distinct().size == questions.size) {
                "Question ids must be unique"
            }
            require(questions.map { it.sequence }.distinct().size == questions.size) {
                "Question sequence values must be unique"
            }
            val firstQuestion = questions.minBy { it.sequence }
            require(session.currentQuestionId == firstQuestion.id) {
                "Current question must point to the first stored question"
            }
            questions.forEach(::validateQuestion)
        }

        private fun validateQuestion(question: SessionQuestion) {
            require(question.sequence >= 0) { "Question sequence cannot be negative" }
            require(question.prompt.isNotBlank()) { "Question prompt cannot be blank" }
            require(question.optionIds.size >= 2) { "A question needs at least two options" }
            require(question.optionIds.distinct().size == question.optionIds.size) {
                "Question options must be unique"
            }
            require(question.correctOptionId in question.optionIds) {
                "The correct option must be present"
            }
        }
    }
