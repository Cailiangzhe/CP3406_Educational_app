package com.cailiangzhe.lexidue.feature.settings

enum class PracticeDifficulty {
    FOUNDATION,
    STANDARD,
    CHALLENGE,
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

data class SettingsUiState(
    val sessionLength: Int = 10,
    val difficulty: PracticeDifficulty = PracticeDifficulty.STANDARD,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val soundEnabled: Boolean = false,
    val hapticsEnabled: Boolean = true,
    val reducedMotion: Boolean = false,
)

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

    data object RequestResetData : SettingsUiAction
}
