package com.cailiangzhe.lexidue.domain.repository

import com.cailiangzhe.lexidue.domain.model.PracticeDifficulty
import com.cailiangzhe.lexidue.domain.model.ThemeMode
import com.cailiangzhe.lexidue.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observeSettings(): Flow<UserSettings>

    suspend fun setSessionLength(wordCount: Int)

    suspend fun setDifficulty(difficulty: PracticeDifficulty)

    suspend fun setThemeMode(themeMode: ThemeMode)

    suspend fun setSoundEnabled(enabled: Boolean)

    suspend fun setHapticsEnabled(enabled: Boolean)

    suspend fun setReducedMotion(enabled: Boolean)

    suspend fun setOnboardingCompleted(completed: Boolean)
}
