package com.cailiangzhe.lexidue

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.cailiangzhe.lexidue.core.designsystem.LexiDueTheme
import com.cailiangzhe.lexidue.domain.model.ThemeMode
import com.cailiangzhe.lexidue.domain.model.UserSettings
import com.cailiangzhe.lexidue.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@Composable
fun LexiDueRoot(viewModel: LexiDueRootViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val systemDarkTheme = isSystemInDarkTheme()
    val darkTheme =
        when (settings.themeMode) {
            ThemeMode.SYSTEM -> systemDarkTheme
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }

    LexiDueTheme(darkTheme = darkTheme) {
        LexiDueApp()
    }
}

@HiltViewModel
class LexiDueRootViewModel
    @Inject
    constructor(
        settingsRepository: SettingsRepository,
    ) : ViewModel() {
        val settings: StateFlow<UserSettings> =
            settingsRepository
                .observeSettings()
                .catch { emit(UserSettings()) }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000L),
                    initialValue = UserSettings(),
                )
    }
