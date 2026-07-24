package com.cailiangzhe.lexidue.feature.settings

import com.cailiangzhe.lexidue.domain.model.PracticeDifficulty
import com.cailiangzhe.lexidue.domain.model.ThemeMode
import com.cailiangzhe.lexidue.domain.model.UserSettings
import com.cailiangzhe.lexidue.domain.repository.SettingsRepository
import com.cailiangzhe.lexidue.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun persistedSettings_areRenderedAfterLoading() =
        runTest(mainDispatcherRule.dispatcher) {
            val persisted =
                UserSettings(
                    sessionLength = 15,
                    difficulty = PracticeDifficulty.CHALLENGE,
                    themeMode = ThemeMode.DARK,
                    soundEnabled = true,
                    hapticsEnabled = false,
                    reducedMotion = true,
                )
            val viewModel = SettingsViewModel(FakeSettingsRepository(persisted))

            assertTrue(viewModel.uiState.value.isLoading)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state.hasLoadedSettings)
            assertFalse(state.isLoading)
            assertTrue(state.controlsEnabled)
            assertEquals(15, state.sessionLength)
            assertEquals(PracticeDifficulty.CHALLENGE, state.difficulty)
            assertEquals(ThemeMode.DARK, state.themeMode)
            assertTrue(state.soundEnabled)
            assertFalse(state.hapticsEnabled)
            assertTrue(state.reducedMotion)
        }

    @Test
    fun updateActions_persistEveryVisiblePreference() =
        runTest(mainDispatcherRule.dispatcher) {
            val repository = FakeSettingsRepository()
            val viewModel = SettingsViewModel(repository)
            advanceUntilIdle()

            viewModel.onAction(SettingsUiAction.SetSessionLength(5))
            advanceUntilIdle()
            viewModel.onAction(SettingsUiAction.SetDifficulty(PracticeDifficulty.FOUNDATION))
            advanceUntilIdle()
            viewModel.onAction(SettingsUiAction.SetThemeMode(ThemeMode.LIGHT))
            advanceUntilIdle()
            viewModel.onAction(SettingsUiAction.SetSoundEnabled(true))
            advanceUntilIdle()
            viewModel.onAction(SettingsUiAction.SetHapticsEnabled(false))
            advanceUntilIdle()
            viewModel.onAction(SettingsUiAction.SetReducedMotion(true))
            advanceUntilIdle()

            val expected =
                UserSettings(
                    sessionLength = 5,
                    difficulty = PracticeDifficulty.FOUNDATION,
                    themeMode = ThemeMode.LIGHT,
                    soundEnabled = true,
                    hapticsEnabled = false,
                    reducedMotion = true,
                )
            assertEquals(expected, repository.settings.value)
            assertEquals(expected.sessionLength, viewModel.uiState.value.sessionLength)
            assertEquals(expected.difficulty, viewModel.uiState.value.difficulty)
            assertEquals(expected.themeMode, viewModel.uiState.value.themeMode)
            assertFalse(viewModel.uiState.value.isUpdating)
            assertEquals(null, viewModel.uiState.value.errorMessage)
        }

    @Test
    fun failedUpdate_keepsPersistedValueAndShowsRecoverableError() =
        runTest(mainDispatcherRule.dispatcher) {
            val repository = FakeSettingsRepository()
            val viewModel = SettingsViewModel(repository)
            advanceUntilIdle()
            repository.updateError = IllegalStateException("Storage unavailable")

            viewModel.onAction(SettingsUiAction.SetSoundEnabled(true))
            advanceUntilIdle()

            assertFalse(repository.settings.value.soundEnabled)
            assertFalse(viewModel.uiState.value.soundEnabled)
            assertFalse(viewModel.uiState.value.isUpdating)
            assertEquals("Storage unavailable", viewModel.uiState.value.errorMessage)
            assertTrue(viewModel.uiState.value.controlsEnabled)

            viewModel.onAction(SettingsUiAction.DismissError)

            assertEquals(null, viewModel.uiState.value.errorMessage)
        }

    @Test
    fun failedPostLoadObservation_disablesControlsUntilRetry() =
        runTest(mainDispatcherRule.dispatcher) {
            val repository = FakeSettingsRepository(UserSettings(sessionLength = 15))
            repository.observationErrorAfterFirstEmission = IllegalStateException("Stream stopped")
            val viewModel = SettingsViewModel(repository)
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.hasLoadedSettings)
            assertFalse(viewModel.uiState.value.controlsEnabled)
            assertEquals("Stream stopped", viewModel.uiState.value.errorMessage)

            repository.observationErrorAfterFirstEmission = null
            viewModel.onAction(SettingsUiAction.Retry)
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.hasLoadedSettings)
            assertTrue(viewModel.uiState.value.controlsEnabled)
            assertEquals(15, viewModel.uiState.value.sessionLength)
        }

    @Test
    fun failedInitialLoad_canBeRetried() =
        runTest(mainDispatcherRule.dispatcher) {
            val repository = FakeSettingsRepository(UserSettings(sessionLength = 15))
            repository.observationError = IllegalStateException("Read failed")
            val viewModel = SettingsViewModel(repository)
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isLoading)
            assertFalse(viewModel.uiState.value.hasLoadedSettings)
            assertEquals("Read failed", viewModel.uiState.value.errorMessage)

            repository.observationError = null
            viewModel.onAction(SettingsUiAction.Retry)
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.hasLoadedSettings)
            assertEquals(15, viewModel.uiState.value.sessionLength)
            assertEquals(null, viewModel.uiState.value.errorMessage)
        }

    @Test
    fun invalidSessionLength_isRejectedWithoutRepositoryWrite() =
        runTest(mainDispatcherRule.dispatcher) {
            val repository = FakeSettingsRepository()
            val viewModel = SettingsViewModel(repository)
            advanceUntilIdle()

            viewModel.onAction(SettingsUiAction.SetSessionLength(9))
            advanceUntilIdle()

            assertEquals(UserSettings.DEFAULT_SESSION_LENGTH, repository.settings.value.sessionLength)
            assertEquals(
                "Session length must be 5, 10, or 15 words.",
                viewModel.uiState.value.errorMessage,
            )
        }

    private class FakeSettingsRepository(
        initialSettings: UserSettings = UserSettings(),
    ) : SettingsRepository {
        val settings = MutableStateFlow(initialSettings)
        var observationError: Throwable? = null
        var observationErrorAfterFirstEmission: Throwable? = null
        var updateError: Throwable? = null

        override fun observeSettings(): Flow<UserSettings> =
            flow {
                observationError?.let { throw it }
                emit(settings.value)
                observationErrorAfterFirstEmission?.let { throw it }
                emitAll(settings.drop(1))
            }

        override suspend fun setSessionLength(wordCount: Int) = update { copy(sessionLength = wordCount) }

        override suspend fun setDifficulty(difficulty: PracticeDifficulty) = update { copy(difficulty = difficulty) }

        override suspend fun setThemeMode(themeMode: ThemeMode) = update { copy(themeMode = themeMode) }

        override suspend fun setSoundEnabled(enabled: Boolean) = update { copy(soundEnabled = enabled) }

        override suspend fun setHapticsEnabled(enabled: Boolean) = update { copy(hapticsEnabled = enabled) }

        override suspend fun setReducedMotion(enabled: Boolean) = update { copy(reducedMotion = enabled) }

        override suspend fun setOnboardingCompleted(completed: Boolean) = update { copy(onboardingCompleted = completed) }

        private fun update(transform: UserSettings.() -> UserSettings) {
            updateError?.let { throw it }
            settings.value = settings.value.transform()
        }
    }
}
