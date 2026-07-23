package com.cailiangzhe.lexidue.data.local

import com.cailiangzhe.lexidue.domain.model.PartOfSpeech
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StarterDeckTest {
    @Test
    fun `starter deck has unique reviewed content and enough same-part-of-speech distractors`() {
        assertEquals(24, StarterDeck.entries.size)
        assertEquals(
            StarterDeck.entries.size,
            StarterDeck.entries
                .map { it.spelling.lowercase() }
                .distinct()
                .size,
        )
        assertEquals(8, StarterDeck.entries.count { it.partOfSpeech == PartOfSpeech.VERB })
        assertEquals(8, StarterDeck.entries.count { it.partOfSpeech == PartOfSpeech.NOUN })
        assertEquals(8, StarterDeck.entries.count { it.partOfSpeech == PartOfSpeech.ADJECTIVE })
        assertTrue(StarterDeck.entries.all { it.definition.endsWith('.') })
        assertTrue(StarterDeck.entries.all { it.definition.length >= 30 })
    }
}
