package com.cailiangzhe.lexidue.domain.repository

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DictionaryEnrichmentFreshnessTest {
    @Test
    fun `accepts current and exactly thirty day old timestamps`() {
        assertTrue(isDictionaryEnrichmentFresh(fetchedAtEpochMillis = NOW, nowEpochMillis = NOW))
        assertTrue(
            isDictionaryEnrichmentFresh(
                fetchedAtEpochMillis = NOW - DictionaryEnrichmentRepository.STALE_AFTER_MILLIS,
                nowEpochMillis = NOW,
            ),
        )
    }

    @Test
    fun `rejects expired negative and future timestamps`() {
        assertFalse(
            isDictionaryEnrichmentFresh(
                fetchedAtEpochMillis = NOW - DictionaryEnrichmentRepository.STALE_AFTER_MILLIS - 1L,
                nowEpochMillis = NOW,
            ),
        )
        assertFalse(isDictionaryEnrichmentFresh(fetchedAtEpochMillis = -1L, nowEpochMillis = NOW))
        assertFalse(isDictionaryEnrichmentFresh(fetchedAtEpochMillis = NOW + 1L, nowEpochMillis = NOW))
    }

    private companion object {
        const val NOW = 1_800_000_000_000L
    }
}
