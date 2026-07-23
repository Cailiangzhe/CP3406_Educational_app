package com.cailiangzhe.lexidue.domain.model

enum class QuestionMode {
    WORD_TO_MEANING,
    MEANING_TO_WORD,
}

data class QuizOption(
    val id: String,
    val text: String,
) {
    init {
        require(id.isNotBlank()) { "A quiz option must have an identifier." }
        require(text.isNotBlank()) { "A quiz option must have text." }
    }
}

data class QuizQuestion(
    val id: String,
    val sourceCardId: String,
    val mode: QuestionMode,
    val prompt: String,
    val partOfSpeech: String,
    val options: List<QuizOption>,
    val correctOptionId: String,
) {
    init {
        require(id.isNotBlank()) { "A quiz question must have an identifier." }
        require(sourceCardId.isNotBlank()) { "A quiz question must reference a learning card." }
        require(prompt.isNotBlank()) { "A quiz question must have a prompt." }
        require(partOfSpeech.isNotBlank()) { "A quiz question must have a part of speech." }
        require(options.size >= 2) { "A multiple-choice question needs at least two options." }
        require(options.map(QuizOption::id).distinct().size == options.size) {
            "Quiz option identifiers must be unique."
        }
        require(options.map { it.text.normalizedForComparison() }.distinct().size == options.size) {
            "Quiz option text must be unique."
        }
        require(options.count { it.id == correctOptionId } == 1) {
            "The correct option must occur exactly once."
        }
    }
}

internal fun String.normalizedForComparison(): String = trim().lowercase()
