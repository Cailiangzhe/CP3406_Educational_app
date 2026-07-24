package com.cailiangzhe.lexidue.domain.model

data class UserSettings(
    val sessionLength: Int = DEFAULT_SESSION_LENGTH,
    val difficulty: PracticeDifficulty = PracticeDifficulty.STANDARD,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val soundEnabled: Boolean = false,
    val hapticsEnabled: Boolean = true,
    val reducedMotion: Boolean = false,
    val onboardingCompleted: Boolean = false,
) {
    init {
        require(sessionLength in SUPPORTED_SESSION_LENGTHS) {
            "Session length must be one of $SUPPORTED_SESSION_LENGTHS"
        }
    }

    companion object {
        const val DEFAULT_SESSION_LENGTH = 10
        val SUPPORTED_SESSION_LENGTHS: Set<Int> = setOf(5, 10, 15)
    }
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}
