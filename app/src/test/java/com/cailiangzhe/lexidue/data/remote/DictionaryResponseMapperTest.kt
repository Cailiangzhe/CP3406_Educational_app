package com.cailiangzhe.lexidue.data.remote

import com.cailiangzhe.lexidue.domain.model.FREE_DICTIONARY_API_PROVIDER
import com.cailiangzhe.lexidue.domain.model.FREE_DICTIONARY_API_SOURCE
import com.cailiangzhe.lexidue.domain.model.PartOfSpeech
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class DictionaryResponseMapperTest {
    @Test
    fun `maps exact normalized word and supported parts of speech`() {
        val result =
            DictionaryResponseMapper
                .map(
                    requestedWord = " Analyse ",
                    entries =
                        listOf(
                            entry(
                                word = "ANALYSE",
                                meanings =
                                    listOf(
                                        meaning("verb", definition("  Examine   in detail.  ")),
                                        meaning("adjective", definition("Relating to analysis.")),
                                    ),
                            ),
                        ),
                ).success()

        assertEquals("analyse", result.normalizedWord)
        assertEquals(listOf(PartOfSpeech.VERB, PartOfSpeech.ADJECTIVE), result.senses.map { it.partOfSpeech })
        assertEquals("Examine in detail.", result.senses.first().definition)
        assertTrue(result.senses.all { it.normalizedWord == "analyse" })
        assertTrue(result.senses.all { it.provider == FREE_DICTIONARY_API_PROVIDER })
        assertTrue(result.senses.all { it.source == FREE_DICTIONARY_API_SOURCE })
    }

    @Test
    fun `rejects entries for a different normalized word`() {
        val result =
            DictionaryResponseMapper.map(
                requestedWord = "analyse",
                entries = listOf(entry(word = "analysis")),
            )

        assertSame(DictionaryLookupResult.UnusableContent, result)
    }

    @Test
    fun `skips unsupported parts of speech and unsafe definitions`() {
        val overlong = "d".repeat(DictionaryResponseMapper.MAX_DEFINITION_LENGTH + 1)
        val result =
            DictionaryResponseMapper.map(
                requestedWord = "analyse",
                entries =
                    listOf(
                        entry(
                            meanings =
                                listOf(
                                    meaning("interjection", definition("A valid but unsupported sense.")),
                                    meaning("verb", definition("<b>Injected markup</b>")),
                                    meaning("verb", definition("Contains\u0000control text.")),
                                    meaning("verb", definition(overlong)),
                                ),
                        ),
                    ),
            )

        assertSame(DictionaryLookupResult.UnusableContent, result)
    }

    @Test
    fun `keeps valid sense while dropping unsafe optional text`() {
        val result =
            DictionaryResponseMapper
                .map(
                    requestedWord = "analyse",
                    entries =
                        listOf(
                            entry(
                                phonetic = "p".repeat(DictionaryResponseMapper.MAX_PHONETIC_LENGTH + 1),
                                meanings =
                                    listOf(
                                        meaning(
                                            "verb",
                                            definition(
                                                text = "Examine in detail.",
                                                example = "<script>unsafe</script>",
                                            ),
                                        ),
                                    ),
                            ),
                        ),
                ).success()

        assertEquals(1, result.senses.size)
        assertNull(result.senses.single().example)
        assertNull(result.senses.single().phonetic)
        assertNull(result.senses.single().audioUrl)
    }

    @Test
    fun `normalizes protocol relative audio to HTTPS when phonetic text exists`() {
        val result =
            mapAudio(
                phonetic = "/əˈnælɪz/",
                audioUrl = "//ssl.gstatic.com/dictionary/static/analyse.mp3",
            )

        assertEquals("/əˈnælɪz/", result.senses.single().phonetic)
        assertEquals(
            "https://ssl.gstatic.com/dictionary/static/analyse.mp3",
            result.senses.single().audioUrl,
        )
    }

    @Test
    fun `rejects insecure malformed local and unpaired audio URLs`() {
        val urls =
            listOf(
                "http://ssl.gstatic.com/analyse.mp3",
                "javascript:alert(1)",
                "https://localhost/analyse.mp3",
                "https://192.168.1.2/analyse.mp3",
                "https:///missing-host.mp3",
            )

        urls.forEach { url ->
            assertNull(mapAudio(phonetic = "/ə/", audioUrl = url).senses.single().audioUrl)
        }
        assertNull(
            mapAudio(phonetic = null, audioUrl = "//ssl.gstatic.com/analyse.mp3")
                .senses
                .single()
                .audioUrl,
        )
    }

    @Test
    fun `deduplicates senses with a stable content ID`() {
        val first =
            DictionaryResponseMapper
                .map(
                    requestedWord = "analyse",
                    entries =
                        listOf(
                            entry(
                                meanings =
                                    listOf(
                                        meaning("verb", definition("Examine in detail.")),
                                        meaning("verb", definition("  examine   in DETAIL. ")),
                                    ),
                            ),
                        ),
                ).success()
        val second =
            DictionaryResponseMapper
                .map(
                    requestedWord = "analyse",
                    entries =
                        listOf(
                            entry(meanings = listOf(meaning("verb", definition("examine in detail.")))),
                        ),
                ).success()

        assertEquals(1, first.senses.size)
        assertEquals(first.senses.single().stableId, second.senses.single().stableId)
        assertTrue(
            first.senses
                .single()
                .stableId
                .startsWith("free-dictionary:"),
        )
    }

    @Test
    fun `bounds the number of mapped senses`() {
        val definitions =
            (1..DictionaryResponseMapper.MAX_SENSES_PER_WORD + 5).map { number ->
                definition("Distinct definition $number.")
            }
        val result =
            DictionaryResponseMapper
                .map(
                    requestedWord = "analyse",
                    entries = listOf(entry(meanings = listOf(meaning("verb", *definitions.toTypedArray())))),
                ).success()

        assertEquals(DictionaryResponseMapper.MAX_SENSES_PER_WORD, result.senses.size)
    }

    @Test
    fun `rejects invalid lookup words before mapping`() {
        assertSame(
            DictionaryLookupResult.InvalidRequest,
            DictionaryResponseMapper.map("en:analyse", emptyList()),
        )
        assertSame(
            DictionaryLookupResult.InvalidRequest,
            DictionaryResponseMapper.map("a".repeat(DictionaryResponseMapper.MAX_WORD_LENGTH + 1), emptyList()),
        )
    }

    private fun mapAudio(
        phonetic: String?,
        audioUrl: String,
    ) = DictionaryResponseMapper
        .map(
            requestedWord = "analyse",
            entries =
                listOf(
                    entry(
                        phonetic = phonetic,
                        phonetics = listOf(PhoneticDto(audio = audioUrl)),
                    ),
                ),
        ).success()

    private fun DictionaryLookupResult.success() = (this as DictionaryLookupResult.Success).enrichment

    private fun entry(
        word: String = "analyse",
        phonetic: String? = null,
        phonetics: List<PhoneticDto>? = null,
        meanings: List<MeaningDto> = listOf(meaning("verb", definition("Examine in detail."))),
    ) = DictionaryEntryDto(
        word = word,
        phonetic = phonetic,
        phonetics = phonetics,
        meanings = meanings,
    )

    private fun meaning(
        partOfSpeech: String,
        vararg definitions: DefinitionDto,
    ) = MeaningDto(partOfSpeech = partOfSpeech, definitions = definitions.toList())

    private fun definition(
        text: String,
        example: String? = null,
    ) = DefinitionDto(definition = text, example = example)
}
