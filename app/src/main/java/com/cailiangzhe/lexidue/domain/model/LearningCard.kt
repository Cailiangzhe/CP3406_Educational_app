package com.cailiangzhe.lexidue.domain.model

/** A reviewed word/meaning pair that is safe to use in scored practice. */
data class LearningCard(
    val id: String,
    val word: String,
    val meaning: String,
    val partOfSpeech: String,
) {
    init {
        require(id.isNotBlank()) { "A learning card must have an identifier." }
        require(word.isNotBlank()) { "A learning card must have a word." }
        require(meaning.isNotBlank()) { "A learning card must have a meaning." }
        require(partOfSpeech.isNotBlank()) { "A learning card must have a part of speech." }
    }
}
