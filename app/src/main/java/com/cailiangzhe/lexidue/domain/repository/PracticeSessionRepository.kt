package com.cailiangzhe.lexidue.domain.repository

import com.cailiangzhe.lexidue.domain.model.Attempt
import com.cailiangzhe.lexidue.domain.model.PracticeSession
import com.cailiangzhe.lexidue.domain.model.RecordAttemptResult
import com.cailiangzhe.lexidue.domain.model.SessionQuestion
import com.cailiangzhe.lexidue.domain.model.SessionSnapshot
import com.cailiangzhe.lexidue.domain.model.SessionStatus
import kotlinx.coroutines.flow.Flow

interface PracticeSessionRepository {
    suspend fun createSession(
        session: PracticeSession,
        questions: List<SessionQuestion>,
    )

    suspend fun getSession(sessionId: String): SessionSnapshot?

    fun observeSession(sessionId: String): Flow<SessionSnapshot?>

    suspend fun recordAttempt(
        attempt: Attempt,
        retryQuestion: SessionQuestion? = null,
    ): RecordAttemptResult

    /**
     * Compare-and-set advancement keeps repeated Continue events harmless. A null next question
     * completes the session; an answered current question remains selected until this is called.
     */
    suspend fun advanceFrom(
        sessionId: String,
        expectedCurrentQuestionId: String,
        nextQuestionId: String?,
        completedAtEpochMillis: Long,
    ): Boolean

    suspend fun updateStatus(
        sessionId: String,
        status: SessionStatus,
        endedAtEpochMillis: Long?,
    ): Boolean

    fun observeRecentSessions(limit: Int): Flow<List<PracticeSession>>
}
