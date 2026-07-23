package com.cailiangzhe.lexidue.domain.model

data class PracticeSession(
    val id: String,
    val difficulty: PracticeDifficulty,
    val randomSeed: Long,
    val plannedWordCount: Int,
    val status: SessionStatus,
    val correctCount: Int,
    val currentQuestionId: String?,
    val startedAtEpochMillis: Long,
    val endedAtEpochMillis: Long?,
)

enum class PracticeDifficulty {
    FOUNDATION,
    STANDARD,
    CHALLENGE,
}

enum class SessionStatus {
    ACTIVE,
    COMPLETED,
    ABANDONED,
}

data class SessionQuestion(
    val id: String,
    val sessionId: String,
    val sequence: Int,
    val wordId: String,
    val questionType: QuestionType,
    val prompt: String,
    val optionIds: List<String>,
    val correctOptionId: String,
    val retryOfQuestionId: String? = null,
)

enum class QuestionType {
    DEFINITION_TO_WORD,
    WORD_TO_DEFINITION,
}

data class SessionQuestionState(
    val question: SessionQuestion,
    val attempt: Attempt?,
)

data class SessionSnapshot(
    val session: PracticeSession,
    val questions: List<SessionQuestionState>,
) {
    val answeredQuestionCount: Int
        get() = questions.count { it.attempt != null }
}
