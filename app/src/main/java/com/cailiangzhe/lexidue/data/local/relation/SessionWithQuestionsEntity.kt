package com.cailiangzhe.lexidue.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.cailiangzhe.lexidue.data.local.entity.AttemptEntity
import com.cailiangzhe.lexidue.data.local.entity.PracticeSessionEntity
import com.cailiangzhe.lexidue.data.local.entity.SessionQuestionEntity

data class SessionQuestionWithAttemptEntity(
    @Embedded val question: SessionQuestionEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "question_id",
    )
    val attempts: List<AttemptEntity>,
)

data class SessionWithQuestionsEntity(
    @Embedded val session: PracticeSessionEntity,
    @Relation(
        entity = SessionQuestionEntity::class,
        parentColumn = "id",
        entityColumn = "session_id",
    )
    val questions: List<SessionQuestionWithAttemptEntity>,
)
