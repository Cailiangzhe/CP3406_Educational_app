package com.cailiangzhe.lexidue.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.cailiangzhe.lexidue.domain.model.PracticeDifficulty
import com.cailiangzhe.lexidue.domain.model.ThemeMode
import com.cailiangzhe.lexidue.domain.model.UserSettings
import com.cailiangzhe.lexidue.domain.repository.SettingsRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import java.io.IOException
import javax.inject.Inject

class PreferencesSettingsRepository
    @Inject
    constructor(
        @param:SettingsPreferencesDataStore private val dataStore: DataStore<Preferences>,
    ) : SettingsRepository {
        override fun observeSettings(): Flow<UserSettings> =
            dataStore.data
                .recoverFromIoFailures()
                .map(Preferences::toUserSettings)
                .distinctUntilChanged()

        override suspend fun setSessionLength(wordCount: Int) {
            require(wordCount in UserSettings.SUPPORTED_SESSION_LENGTHS) {
                "Session length must be one of ${UserSettings.SUPPORTED_SESSION_LENGTHS}"
            }
            dataStore.edit { preferences ->
                preferences[UserSettingsPreferenceKeys.SESSION_LENGTH] = wordCount
            }
        }

        override suspend fun setDifficulty(difficulty: PracticeDifficulty) {
            dataStore.edit { preferences ->
                preferences[UserSettingsPreferenceKeys.DIFFICULTY] = difficulty.name
            }
        }

        override suspend fun setThemeMode(themeMode: ThemeMode) {
            dataStore.edit { preferences ->
                preferences[UserSettingsPreferenceKeys.THEME_MODE] = themeMode.name
            }
        }

        override suspend fun setSoundEnabled(enabled: Boolean) {
            dataStore.edit { preferences ->
                preferences[UserSettingsPreferenceKeys.SOUND_ENABLED] = enabled
            }
        }

        override suspend fun setHapticsEnabled(enabled: Boolean) {
            dataStore.edit { preferences ->
                preferences[UserSettingsPreferenceKeys.HAPTICS_ENABLED] = enabled
            }
        }

        override suspend fun setReducedMotion(enabled: Boolean) {
            dataStore.edit { preferences ->
                preferences[UserSettingsPreferenceKeys.REDUCED_MOTION] = enabled
            }
        }

        override suspend fun setOnboardingCompleted(completed: Boolean) {
            dataStore.edit { preferences ->
                preferences[UserSettingsPreferenceKeys.ONBOARDING_COMPLETED] = completed
            }
        }
    }

internal fun Flow<Preferences>.recoverFromIoFailures(retryDelayMillis: Long = SETTINGS_IO_RETRY_DELAY_MILLIS): Flow<Preferences> {
    require(retryDelayMillis >= 0L) { "Settings retry delay cannot be negative." }
    return retryWhen { error, _ ->
        if (error !is IOException) {
            false
        } else {
            emit(emptyPreferences())
            if (retryDelayMillis > 0L) delay(retryDelayMillis)
            true
        }
    }
}

private const val SETTINGS_IO_RETRY_DELAY_MILLIS = 1_000L

internal object UserSettingsPreferenceKeys {
    val SESSION_LENGTH = intPreferencesKey("session_length")
    val DIFFICULTY = stringPreferencesKey("difficulty")
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
    val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
    val REDUCED_MOTION = booleanPreferencesKey("reduced_motion")
    val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
}

internal fun Preferences.toUserSettings(): UserSettings {
    val defaults = UserSettings()
    return UserSettings(
        sessionLength =
            this[UserSettingsPreferenceKeys.SESSION_LENGTH]
                ?.takeIf { it in UserSettings.SUPPORTED_SESSION_LENGTHS }
                ?: defaults.sessionLength,
        difficulty =
            enumValueOrDefault(
                storedValue = this[UserSettingsPreferenceKeys.DIFFICULTY],
                defaultValue = defaults.difficulty,
            ),
        themeMode =
            enumValueOrDefault(
                storedValue = this[UserSettingsPreferenceKeys.THEME_MODE],
                defaultValue = defaults.themeMode,
            ),
        soundEnabled = this[UserSettingsPreferenceKeys.SOUND_ENABLED] ?: defaults.soundEnabled,
        hapticsEnabled = this[UserSettingsPreferenceKeys.HAPTICS_ENABLED] ?: defaults.hapticsEnabled,
        reducedMotion = this[UserSettingsPreferenceKeys.REDUCED_MOTION] ?: defaults.reducedMotion,
        onboardingCompleted =
            this[UserSettingsPreferenceKeys.ONBOARDING_COMPLETED] ?: defaults.onboardingCompleted,
    )
}

private inline fun <reified T : Enum<T>> enumValueOrDefault(
    storedValue: String?,
    defaultValue: T,
): T = enumValues<T>().firstOrNull { it.name == storedValue } ?: defaultValue
