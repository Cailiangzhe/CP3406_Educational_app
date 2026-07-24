package com.cailiangzhe.lexidue.data.remote

import kotlinx.serialization.Serializable

/** DTO fields are nullable so omitted optional fields remain a valid payload. */
@Serializable
internal data class DictionaryEntryDto(
    val word: String? = null,
    val phonetic: String? = null,
    val phonetics: List<PhoneticDto>? = null,
    val meanings: List<MeaningDto>? = null,
)

@Serializable
internal data class PhoneticDto(
    val text: String? = null,
    val audio: String? = null,
)

@Serializable
internal data class MeaningDto(
    val partOfSpeech: String? = null,
    val definitions: List<DefinitionDto>? = null,
)

@Serializable
internal data class DefinitionDto(
    val definition: String? = null,
    val example: String? = null,
)
