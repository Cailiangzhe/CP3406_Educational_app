package com.cailiangzhe.lexidue.domain.model

const val FREE_DICTIONARY_API_PROVIDER = "Free Dictionary API"
const val FREE_DICTIONARY_API_SOURCE = "https://dictionaryapi.dev/"

/**
 * Display-only content returned by an external dictionary.
 *
 * [ApiSense] is deliberately separate from [CanonicalMeaning]. API content must not be used as a
 * scored answer, a distractor, or a replacement for reviewed local content.
 */
data class DictionaryEnrichment(
    val normalizedWord: String,
    val senses: List<ApiSense>,
) {
    init {
        require(normalizedWord.isNotBlank()) { "An enrichment must identify its requested word." }
        require(senses.isNotEmpty()) { "An enrichment must contain at least one usable sense." }
        require(senses.all { it.normalizedWord == normalizedWord }) {
            "Every API sense must belong to the requested word."
        }
        require(senses.map(ApiSense::stableId).distinct().size == senses.size) {
            "API sense IDs must be unique within an enrichment."
        }
    }
}

/** Quarantined dictionary metadata for display and optional pronunciation assistance only. */
data class ApiSense(
    val stableId: String,
    val normalizedWord: String,
    val partOfSpeech: PartOfSpeech,
    val definition: String,
    val example: String?,
    val phonetic: String?,
    val audioUrl: String?,
    val provider: String = FREE_DICTIONARY_API_PROVIDER,
    val source: String = FREE_DICTIONARY_API_SOURCE,
) {
    init {
        require(stableId.isNotBlank()) { "An API sense must have a stable ID." }
        require(normalizedWord.isNotBlank()) { "An API sense must identify its requested word." }
        require(partOfSpeech != PartOfSpeech.OTHER) { "Unsupported parts of speech cannot be cached." }
        require(definition.isNotBlank()) { "An API definition cannot be blank." }
        require(example == null || example.isNotBlank()) { "An API example cannot be blank." }
        require(phonetic == null || phonetic.isNotBlank()) { "An API phonetic cannot be blank." }
        require(audioUrl == null || audioUrl.startsWith("https://")) {
            "Pronunciation audio must use HTTPS."
        }
        require(provider == FREE_DICTIONARY_API_PROVIDER) { "Unexpected dictionary provider." }
        require(source == FREE_DICTIONARY_API_SOURCE) { "Unexpected dictionary source." }
    }
}
