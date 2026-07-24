package com.cailiangzhe.lexidue.data.remote

import com.cailiangzhe.lexidue.domain.model.ApiSense
import com.cailiangzhe.lexidue.domain.model.DictionaryEnrichment
import com.cailiangzhe.lexidue.domain.model.FREE_DICTIONARY_API_PROVIDER
import com.cailiangzhe.lexidue.domain.model.FREE_DICTIONARY_API_SOURCE
import com.cailiangzhe.lexidue.domain.model.PartOfSpeech
import java.net.URI
import java.security.MessageDigest
import java.text.Normalizer
import java.util.Locale

internal object DictionaryResponseMapper {
    const val MAX_WORD_LENGTH = 64
    const val MAX_DEFINITION_LENGTH = 500
    const val MAX_EXAMPLE_LENGTH = 500
    const val MAX_PHONETIC_LENGTH = 100
    const val MAX_AUDIO_URL_LENGTH = 2_048
    const val MAX_SENSES_PER_WORD = 20

    fun normalizeRequestedWord(rawWord: String): String? {
        val normalized =
            Normalizer
                .normalize(rawWord.trim(), Normalizer.Form.NFC)
                .lowercase(Locale.ROOT)
        return normalized.takeIf {
            it.length in 1..MAX_WORD_LENGTH && SAFE_ENGLISH_WORD.matches(it)
        }
    }

    fun map(
        requestedWord: String,
        entries: List<DictionaryEntryDto>,
    ): DictionaryLookupResult {
        val normalizedRequestedWord =
            normalizeRequestedWord(requestedWord) ?: return DictionaryLookupResult.InvalidRequest
        val sensesByIdentity = linkedMapOf<String, ApiSense>()

        for (entry in entries) {
            if (sensesByIdentity.size >= MAX_SENSES_PER_WORD) break
            val responseWord = entry.word?.let(::normalizeRequestedWord)
            if (responseWord != normalizedRequestedWord) continue

            val pronunciation = entry.validPronunciation()
            for (meaning in entry.meanings.orEmpty()) {
                if (sensesByIdentity.size >= MAX_SENSES_PER_WORD) break
                val partOfSpeech = meaning.partOfSpeech.toSupportedPartOfSpeech() ?: continue
                for (definitionDto in meaning.definitions.orEmpty()) {
                    if (sensesByIdentity.size >= MAX_SENSES_PER_WORD) break
                    val definition =
                        definitionDto.definition.validText(MAX_DEFINITION_LENGTH) ?: continue
                    val identity = senseIdentity(partOfSpeech, definition)
                    if (identity !in sensesByIdentity) {
                        sensesByIdentity[identity] =
                            ApiSense(
                                stableId = stableSenseId(normalizedRequestedWord, identity),
                                normalizedWord = normalizedRequestedWord,
                                partOfSpeech = partOfSpeech,
                                definition = definition,
                                example = definitionDto.example.validText(MAX_EXAMPLE_LENGTH),
                                phonetic = pronunciation.phonetic,
                                audioUrl = pronunciation.audioUrl,
                                provider = FREE_DICTIONARY_API_PROVIDER,
                                source = FREE_DICTIONARY_API_SOURCE,
                            )
                    }
                }
            }
        }

        if (sensesByIdentity.isEmpty()) return DictionaryLookupResult.UnusableContent
        return DictionaryLookupResult.Success(
            DictionaryEnrichment(
                normalizedWord = normalizedRequestedWord,
                senses = sensesByIdentity.values.toList(),
            ),
        )
    }

    private fun DictionaryEntryDto.validPronunciation(): ValidPronunciation {
        val entryPhonetic = phonetic.validText(MAX_PHONETIC_LENGTH)
        val candidates =
            phonetics.orEmpty().map { dto ->
                ValidPronunciation(
                    phonetic = dto.text.validText(MAX_PHONETIC_LENGTH),
                    audioUrl = dto.audio.toSafeHttpsAudioUrl(),
                )
            }
        val selectedPhonetic = candidates.firstNotNullOfOrNull { it.phonetic } ?: entryPhonetic
        val selectedAudio =
            candidates
                .firstOrNull { it.audioUrl != null && (it.phonetic != null || entryPhonetic != null) }
                ?.audioUrl
        return ValidPronunciation(
            phonetic = selectedPhonetic,
            audioUrl = selectedAudio?.takeIf { selectedPhonetic != null },
        )
    }

    private fun String?.toSupportedPartOfSpeech(): PartOfSpeech? =
        when (this?.trim()?.lowercase(Locale.ROOT)) {
            "noun" -> PartOfSpeech.NOUN
            "verb" -> PartOfSpeech.VERB
            "adjective" -> PartOfSpeech.ADJECTIVE
            "adverb" -> PartOfSpeech.ADVERB
            else -> null
        }

    private fun String?.validText(maxLength: Int): String? {
        val raw = this ?: return null
        if (raw.any(Char::isISOControl) || raw.contains('<') || raw.contains('>')) return null
        val normalized =
            Normalizer
                .normalize(raw, Normalizer.Form.NFC)
                .trim()
                .replace(WHITESPACE, " ")
        return normalized.takeIf { it.isNotBlank() && it.length <= maxLength }
    }

    private fun String?.toSafeHttpsAudioUrl(): String? {
        val raw =
            this?.trim()?.takeIf { it.isNotEmpty() && it.length <= MAX_AUDIO_URL_LENGTH }
                ?: return null
        val httpsUrl = if (raw.startsWith("//")) "https:$raw" else raw
        val uri = runCatching { URI(httpsUrl) }.getOrNull() ?: return null
        if (!uri.scheme.equals("https", ignoreCase = true)) return null
        if (uri.rawUserInfo != null || uri.rawFragment != null) return null
        val host = uri.host?.lowercase(Locale.ROOT) ?: return null
        if (host.isUnsafeAudioHost()) return null
        val canonicalHttpsUrl = "https${httpsUrl.substring(httpsUrl.indexOf(':'))}"
        return URI(canonicalHttpsUrl).normalize().toASCIIString()
    }

    private fun String.isUnsafeAudioHost(): Boolean {
        if (this == "localhost" || endsWith(".localhost") || endsWith(".local")) return true
        if (!contains('.') || contains(':')) return true

        val octets = split('.').map { it.toIntOrNull() }
        if (octets.any { it == null }) return false
        val values = octets.filterNotNull()
        if (values.size != 4 || values.any { it !in 0..255 }) return true
        return values[0] == 0 ||
            values[0] == 10 ||
            values[0] == 127 ||
            (values[0] == 169 && values[1] == 254) ||
            (values[0] == 172 && values[1] in 16..31) ||
            (values[0] == 192 && values[1] == 168) ||
            values[0] >= 224
    }

    private fun senseIdentity(
        partOfSpeech: PartOfSpeech,
        definition: String,
    ): String = "${partOfSpeech.name}\u0000${definition.lowercase(Locale.ROOT)}"

    private fun stableSenseId(
        normalizedWord: String,
        identity: String,
    ): String {
        val digest =
            MessageDigest
                .getInstance("SHA-256")
                .digest("$normalizedWord\u0000$identity".toByteArray(Charsets.UTF_8))
        return buildString(STABLE_ID_PREFIX.length + digest.size * 2) {
            append(STABLE_ID_PREFIX)
            digest.forEach { byte ->
                val value = byte.toInt() and 0xff
                append(HEX_DIGITS[value ushr 4])
                append(HEX_DIGITS[value and 0x0f])
            }
        }
    }

    private data class ValidPronunciation(
        val phonetic: String?,
        val audioUrl: String?,
    )

    private const val STABLE_ID_PREFIX = "free-dictionary:"
    private const val HEX_DIGITS = "0123456789abcdef"
    private val SAFE_ENGLISH_WORD = Regex("[a-z]+(?:['-][a-z]+)*")
    private val WHITESPACE = Regex("\\s+")
}
