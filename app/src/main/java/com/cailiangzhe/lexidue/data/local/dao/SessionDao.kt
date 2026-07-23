package com.cailiangzhe.lexidue.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.cailiangzhe.lexidue.data.local.entity.PracticeSessionEntity
import com.cailiangzhe.lexidue.data.local.entity.SessionQuestionEntity
import com.cailiangzhe.lexidue.data.local.relation.SessionWithQuestionsEntity
import com.cailiangzhe.lexidue.domain.model.SessionStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSession(session: PracticeSessionEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertQuestions(questions: List<SessionQuestionEntity>)

    @Transaction
    @Query("SELECT * FROM practice_sessions WHERE id = :sessionId")
    suspend fun getSession(sessionId: String): SessionWithQuestionsEntity?

    @Transaction
    @Query("SELECT * FROM practice_sessions WHERE id = :sessionId")
    fun observeSession(sessionId: String): Flow<SessionWithQuestionsEntity?>

    @Query(
        """
        UPDATE practice_sessions
        SET status = :status,
            current_question_id = CASE WHEN :status = 'ACTIVE' THEN current_question_id ELSE NULL END,
            ended_at_epoch_millis = :endedAtEpochMillis
        WHERE id = :sessionId AND status = 'ACTIVE'
        """,
    )
    suspend fun updateStatus(
        sessionId: String,
        status: SessionStatus,
        endedAtEpochMillis: Long?,
    ): Int

    @Query(
        """
        UPDATE practice_sessions
        SET current_question_id = :nextQuestionId,
            status = CASE WHEN :nextQuestionId IS NULL THEN 'COMPLETED' ELSE status END,
            ended_at_epoch_millis = CASE
                WHEN :nextQuestionId IS NULL THEN :completedAtEpochMillis
                ELSE ended_at_epoch_millis
            END
        WHERE id = :sessionId
            AND status = 'ACTIVE'
            AND current_question_id = :expectedCurrentQuestionId
        """,
    )
    suspend fun compareAndSetCurrentQuestion(
        sessionId: String,
        expectedCurrentQuestionId: String,
        nextQuestionId: String?,
        completedAtEpochMillis: Long,
    ): Int

    @Query(
        """
        SELECT COUNT(*) FROM session_questions
        WHERE id = :questionId AND session_id = :sessionId
        """,
    )
    suspend fun questionBelongsToSession(
        sessionId: String,
        questionId: String,
    ): Int

    @Query("SELECT * FROM practice_sessions ORDER BY started_at_epoch_millis DESC LIMIT :limit")
    fun observeRecentSessions(limit: Int): Flow<List<PracticeSessionEntity>>
}
