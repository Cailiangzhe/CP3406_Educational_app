package com.cailiangzhe.lexidue.domain.model

/** A reviewed local word. Remote dictionary content never replaces these meanings. */
data class Word(
    val id: String,
    val normalizedSpelling: String,
    val displaySpelling: String,
    val deckId: String,
    val sourceName: String,
    val canonicalMeanings: List<CanonicalMeaning>,
)

data class CanonicalMeaning(
    val id: String,
    val wordId: String,
    val partOfSpeech: PartOfSpeech,
    val definition: String,
    val provenance: String,
)

enum class PartOfSpeech {
    NOUN,
    VERB,
    ADJECTIVE,
    ADVERB,
    OTHER,
}
