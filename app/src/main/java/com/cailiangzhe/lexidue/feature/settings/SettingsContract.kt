package com.cailiangzhe.lexidue.feature.settings

import com.cailiangzhe.lexidue.domain.model.PracticeDifficulty
import com.cailiangzhe.lexidue.domain.model.ThemeMode

data class SettingsUiState(
    val sessionLength: Int = 10,
    val difficulty: PracticeDifficulty = PracticeDifficulty.STANDARD,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val soundEnabled: Boolean = false,
    val hapticsEnabled: Boolean = true,
    val reducedMotion: Boolean = false,
    val isLoading: Boolean = true,
    val hasLoadedSettings: Boolean = false,
    val isUpdating: Boolean = false,
    val errorMessage: String? = null,
) {
    val controlsEnabled: Boolean
        get() = hasLoadedSettings && !isUpdating
}

sealed interface SettingsUiAction {
    data class SetSessionLength(
        val wordCount: Int,
    ) : SettingsUiAction

    data class SetDifficulty(
        val difficulty: PracticeDifficulty,
    ) : SettingsUiAction

    data class SetThemeMode(
        val themeMode: ThemeMode,
    ) : SettingsUiAction

    data class SetSoundEnabled(
        val enabled: Boolean,
    ) : SettingsUiAction

    data class SetHapticsEnabled(
        val enabled: Boolean,
    ) : SettingsUiAction

    data class SetReducedMotion(
        val enabled: Boolean,
    ) : SettingsUiAction

    data object Retry : SettingsUiAction

    data object DismissError : SettingsUiAction
}
