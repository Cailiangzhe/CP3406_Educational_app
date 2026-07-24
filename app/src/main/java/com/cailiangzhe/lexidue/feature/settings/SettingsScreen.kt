package com.cailiangzhe.lexidue.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.cailiangzhe.lexidue.R
import com.cailiangzhe.lexidue.core.designsystem.component.LexiDueScreen
import com.cailiangzhe.lexidue.core.designsystem.component.SectionHeading
import com.cailiangzhe.lexidue.domain.model.PracticeDifficulty
import com.cailiangzhe.lexidue.domain.model.ThemeMode
import com.cailiangzhe.lexidue.domain.model.UserSettings

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onAction: (SettingsUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    LexiDueScreen(
        title = stringResource(R.string.settings_title),
        subtitle = stringResource(R.string.settings_subtitle),
        modifier = modifier,
    ) {
        uiState.errorMessage?.let { errorMessage ->
            val canRetryObservation = !uiState.hasLoadedSettings
            SettingsErrorCard(
                message = errorMessage,
                actionLabel =
                    stringResource(
                        if (canRetryObservation) R.string.action_try_again else R.string.action_dismiss,
                    ),
                actionEnabled = !uiState.isUpdating,
                onAction = {
                    onAction(
                        if (canRetryObservation) {
                            SettingsUiAction.Retry
                        } else {
                            SettingsUiAction.DismissError
                        },
                    )
                },
            )
        }

        if (!uiState.hasLoadedSettings) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
            return@LexiDueScreen
        }

        if (uiState.isUpdating) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        SectionHeading(stringResource(R.string.settings_session_length_heading))
        Text(
            text = stringResource(R.string.settings_session_length_description),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(
            modifier = Modifier.fillMaxWidth().selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            UserSettings.SUPPORTED_SESSION_LENGTHS.sorted().forEach { wordCount ->
                PersistedChoiceButton(
                    label =
                        pluralStringResource(
                            R.plurals.settings_word_count,
                            wordCount,
                            wordCount,
                        ),
                    selected = uiState.sessionLength == wordCount,
                    enabled = uiState.controlsEnabled,
                    onClick = { onAction(SettingsUiAction.SetSessionLength(wordCount)) },
                )
            }
        }

        SectionHeading(stringResource(R.string.settings_difficulty_heading))
        Text(
            text = stringResource(R.string.settings_difficulty_description),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(
            modifier = Modifier.fillMaxWidth().selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PracticeDifficulty.entries.forEach { difficulty ->
                PersistedChoiceButton(
                    label = persistedDifficultyLabel(difficulty),
                    selected = uiState.difficulty == difficulty,
                    enabled = uiState.controlsEnabled,
                    onClick = { onAction(SettingsUiAction.SetDifficulty(difficulty)) },
                )
            }
        }

        SectionHeading(stringResource(R.string.settings_theme_heading))
        Column(
            modifier = Modifier.fillMaxWidth().selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ThemeMode.entries.forEach { themeMode ->
                PersistedChoiceButton(
                    label = persistedThemeModeLabel(themeMode),
                    selected = uiState.themeMode == themeMode,
                    enabled = uiState.controlsEnabled,
                    onClick = { onAction(SettingsUiAction.SetThemeMode(themeMode)) },
                )
            }
        }

        SectionHeading(stringResource(R.string.settings_effects_heading))
        PersistedSettingSwitchRow(
            title = stringResource(R.string.settings_sound_title),
            description = stringResource(R.string.settings_sound_description),
            checked = uiState.soundEnabled,
            enabled = uiState.controlsEnabled,
            onCheckedChange = { onAction(SettingsUiAction.SetSoundEnabled(it)) },
        )
        PersistedSettingSwitchRow(
            title = stringResource(R.string.settings_haptics_title),
            description = stringResource(R.string.settings_haptics_description),
            checked = uiState.hapticsEnabled,
            enabled = uiState.controlsEnabled,
            onCheckedChange = { onAction(SettingsUiAction.SetHapticsEnabled(it)) },
        )
        PersistedSettingSwitchRow(
            title = stringResource(R.string.settings_reduced_motion_title),
            description = stringResource(R.string.settings_reduced_motion_description),
            checked = uiState.reducedMotion,
            enabled = uiState.controlsEnabled,
            onCheckedChange = { onAction(SettingsUiAction.SetReducedMotion(it)) },
        )

        SectionHeading(stringResource(R.string.settings_privacy_heading))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.settings_privacy_local_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.settings_privacy_local_description),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = stringResource(R.string.settings_api_description),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

@Composable
private fun SettingsErrorCard(
    message: String,
    actionLabel: String,
    actionEnabled: Boolean,
    onAction: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics { liveRegion = LiveRegionMode.Polite },
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            ),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
            )
            OutlinedButton(
                onClick = onAction,
                enabled = actionEnabled,
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
private fun PersistedChoiceButton(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .semantics {
                    role = Role.RadioButton
                    this.selected = selected
                },
    ) {
        Text(
            text =
                if (selected) {
                    stringResource(R.string.settings_selected_choice, label)
                } else {
                    label
                },
        )
    }
}

@Composable
private fun PersistedSettingSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .toggleable(
                        value = checked,
                        enabled = enabled,
                        role = Role.Switch,
                        onValueChange = onCheckedChange,
                    ).semantics(mergeDescendants = true) {
                        contentDescription = title
                    }.padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = null,
                enabled = enabled,
            )
        }
    }
}

@Composable
private fun persistedDifficultyLabel(difficulty: PracticeDifficulty): String =
    when (difficulty) {
        PracticeDifficulty.FOUNDATION -> stringResource(R.string.settings_difficulty_foundation)
        PracticeDifficulty.STANDARD -> stringResource(R.string.settings_difficulty_standard)
        PracticeDifficulty.CHALLENGE -> stringResource(R.string.settings_difficulty_challenge)
    }

@Composable
private fun persistedThemeModeLabel(themeMode: ThemeMode): String =
    when (themeMode) {
        ThemeMode.SYSTEM -> stringResource(R.string.settings_theme_system)
        ThemeMode.LIGHT -> stringResource(R.string.settings_theme_light)
        ThemeMode.DARK -> stringResource(R.string.settings_theme_dark)
    }
