package com.cailiangzhe.lexidue.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cailiangzhe.lexidue.domain.model.UserSettings
import com.cailiangzhe.lexidue.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.CancellationException
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val settingsRepository: SettingsRepository,
    ) : ViewModel() {
        private val mutableUiState = MutableStateFlow(SettingsUiState())
        val uiState: StateFlow<SettingsUiState> = mutableUiState.asStateFlow()

        private var observationJob: Job? = null

        init {
            observeSettings()
        }

        fun onAction(action: SettingsUiAction) {
            when (action) {
                is SettingsUiAction.SetSessionLength -> {
                    setSessionLength(action.wordCount)
                }

                is SettingsUiAction.SetDifficulty -> {
                    updateSetting { settingsRepository.setDifficulty(action.difficulty) }
                }

                is SettingsUiAction.SetThemeMode -> {
                    updateSetting { settingsRepository.setThemeMode(action.themeMode) }
                }

                is SettingsUiAction.SetSoundEnabled -> {
                    updateSetting { settingsRepository.setSoundEnabled(action.enabled) }
                }

                is SettingsUiAction.SetHapticsEnabled -> {
                    updateSetting { settingsRepository.setHapticsEnabled(action.enabled) }
                }

                is SettingsUiAction.SetReducedMotion -> {
                    updateSetting { settingsRepository.setReducedMotion(action.enabled) }
                }

                SettingsUiAction.Retry -> {
                    observeSettings()
                }

                SettingsUiAction.DismissError -> {
                    mutableUiState.update { it.copy(errorMessage = null) }
                }
            }
        }

        private fun observeSettings() {
            observationJob?.cancel()
            mutableUiState.update { current ->
                current.copy(
                    isLoading = !current.hasLoadedSettings,
                    errorMessage = null,
                )
            }
            observationJob =
                viewModelScope.launch {
                    try {
                        settingsRepository.observeSettings().collect { settings ->
                            mutableUiState.update { current -> current.withSettings(settings) }
                        }
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (error: Throwable) {
                        mutableUiState.update { current ->
                            current.copy(
                                isLoading = false,
                                hasLoadedSettings = false,
                                errorMessage = error.userFacingMessage(),
                            )
                        }
                    }
                }
        }

        private fun setSessionLength(wordCount: Int) {
            if (wordCount !in UserSettings.SUPPORTED_SESSION_LENGTHS) {
                mutableUiState.update { current ->
                    current.copy(errorMessage = "Session length must be 5, 10, or 15 words.")
                }
                return
            }
            updateSetting { settingsRepository.setSessionLength(wordCount) }
        }

        private fun updateSetting(update: suspend () -> Unit) {
            val current = mutableUiState.value
            if (!current.hasLoadedSettings || current.isUpdating) return

            mutableUiState.update { it.copy(isUpdating = true, errorMessage = null) }
            viewModelScope.launch {
                try {
                    update()
                    mutableUiState.update { it.copy(isUpdating = false) }
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (error: Throwable) {
                    mutableUiState.update {
                        it.copy(
                            isUpdating = false,
                            errorMessage = error.userFacingMessage(),
                        )
                    }
                }
            }
        }

        private fun SettingsUiState.withSettings(settings: UserSettings): SettingsUiState =
            copy(
                sessionLength = settings.sessionLength,
                difficulty = settings.difficulty,
                themeMode = settings.themeMode,
                soundEnabled = settings.soundEnabled,
                hapticsEnabled = settings.hapticsEnabled,
                reducedMotion = settings.reducedMotion,
                isLoading = false,
                hasLoadedSettings = true,
            )

        private fun Throwable.userFacingMessage(): String = message?.takeIf(String::isNotBlank) ?: "Settings are temporarily unavailable."
    }
