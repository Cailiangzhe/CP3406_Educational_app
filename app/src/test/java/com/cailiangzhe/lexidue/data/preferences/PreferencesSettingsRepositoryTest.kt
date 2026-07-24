package com.cailiangzhe.lexidue.data.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.mutablePreferencesOf
import com.cailiangzhe.lexidue.domain.model.PracticeDifficulty
import com.cailiangzhe.lexidue.domain.model.ThemeMode
import com.cailiangzhe.lexidue.domain.model.UserSettings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class PreferencesSettingsRepositoryTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun emptyDataStore_emitsSafeDefaults() =
        runTest {
            val repository = createRepository()

            assertEquals(UserSettings(), repository.observeSettings().first())
        }

    @Test
    fun updates_arePersistedTogetherWithOnboardingState() =
        runTest {
            val repository = createRepository()

            repository.setSessionLength(15)
            repository.setDifficulty(PracticeDifficulty.CHALLENGE)
            repository.setThemeMode(ThemeMode.DARK)
            repository.setSoundEnabled(true)
            repository.setHapticsEnabled(false)
            repository.setReducedMotion(true)
            repository.setOnboardingCompleted(true)

            assertEquals(
                UserSettings(
                    sessionLength = 15,
                    difficulty = PracticeDifficulty.CHALLENGE,
                    themeMode = ThemeMode.DARK,
                    soundEnabled = true,
                    hapticsEnabled = false,
                    reducedMotion = true,
                    onboardingCompleted = true,
                ),
                repository.observeSettings().first(),
            )
        }

    @Test
    fun unknownStoredValues_fallBackWithoutDiscardingValidValues() =
        runTest {
            val dataStore = createDataStore()
            dataStore.edit { preferences ->
                preferences[UserSettingsPreferenceKeys.SESSION_LENGTH] = 42
                preferences[UserSettingsPreferenceKeys.DIFFICULTY] = "EXPERT"
                preferences[UserSettingsPreferenceKeys.THEME_MODE] = "NEON"
                preferences[UserSettingsPreferenceKeys.SOUND_ENABLED] = true
                preferences[UserSettingsPreferenceKeys.HAPTICS_ENABLED] = false
                preferences[UserSettingsPreferenceKeys.REDUCED_MOTION] = true
                preferences[UserSettingsPreferenceKeys.ONBOARDING_COMPLETED] = true
            }

            val settings = PreferencesSettingsRepository(dataStore).observeSettings().first()

            assertEquals(UserSettings.DEFAULT_SESSION_LENGTH, settings.sessionLength)
            assertEquals(PracticeDifficulty.STANDARD, settings.difficulty)
            assertEquals(ThemeMode.SYSTEM, settings.themeMode)
            assertTrue(settings.soundEnabled)
            assertFalse(settings.hapticsEnabled)
            assertTrue(settings.reducedMotion)
            assertTrue(settings.onboardingCompleted)
        }

    @Test
    fun unsupportedSessionLength_isRejectedBeforeWriting() =
        runTest {
            val repository = createRepository()

            val failure =
                try {
                    repository.setSessionLength(7)
                    null
                } catch (error: IllegalArgumentException) {
                    error
                }

            assertTrue(failure?.message?.contains("5, 10, 15") == true)
            assertEquals(UserSettings(), repository.observeSettings().first())
        }

    @Test
    fun corruptedFile_isReplacedWithDefaults_andAcceptsLaterWrites() =
        runTest {
            val file = File(temporaryFolder.root, "settings.preferences_pb")
            file.writeBytes(byteArrayOf(0x80.toByte()))
            val repository = PreferencesSettingsRepository(createDataStore(file))

            assertEquals(UserSettings(), repository.observeSettings().first())

            repository.setSessionLength(15)

            assertEquals(15, repository.observeSettings().first().sessionLength)
        }

    @Test
    fun ioFailure_emitsDefaults_thenResubscribesForLaterSettings() =
        runTest {
            var subscriptionCount = 0
            val persistedPreferences =
                mutablePreferencesOf(
                    UserSettingsPreferenceKeys.SESSION_LENGTH to 15,
                )
            val observed =
                flow<Preferences> {
                    subscriptionCount += 1
                    if (subscriptionCount == 1) throw IOException("Temporary read failure")
                    emit(persistedPreferences)
                }.recoverFromIoFailures(retryDelayMillis = 0L)
                    .map(Preferences::toUserSettings)
                    .take(2)
                    .toList()

            assertEquals(listOf(UserSettings(), UserSettings(sessionLength = 15)), observed)
            assertEquals(2, subscriptionCount)
        }

    private fun kotlinx.coroutines.test.TestScope.createRepository(): PreferencesSettingsRepository =
        PreferencesSettingsRepository(createDataStore())

    private fun kotlinx.coroutines.test.TestScope.createDataStore(file: File = File(temporaryFolder.root, "settings.preferences_pb")) =
        PreferenceDataStoreFactory.create(
            corruptionHandler = SETTINGS_CORRUPTION_HANDLER,
            scope = backgroundScope,
            produceFile = { file },
        )
}
