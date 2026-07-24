package com.cailiangzhe.lexidue.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class UserSettingsTest {
    @Test
    fun defaults_areSafeAndPredictable() {
        val settings = UserSettings()

        assertEquals(10, settings.sessionLength)
        assertEquals(PracticeDifficulty.STANDARD, settings.difficulty)
        assertEquals(ThemeMode.SYSTEM, settings.themeMode)
        assertFalse(settings.soundEnabled)
        assertTrue(settings.hapticsEnabled)
        assertFalse(settings.reducedMotion)
        assertFalse(settings.onboardingCompleted)
    }

    @Test
    fun unsupportedSessionLength_cannotEnterDomainModel() {
        assertThrows(IllegalArgumentException::class.java) {
            UserSettings(sessionLength = 12)
        }
    }
}
