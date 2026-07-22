package com.cailiangzhe.lexidue.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.cailiangzhe.lexidue.R
import com.cailiangzhe.lexidue.core.designsystem.component.LexiDueScreen
import com.cailiangzhe.lexidue.core.designsystem.component.SectionHeading

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    uiState: SettingsUiState = SettingsUiState(),
    onAction: (SettingsUiAction) -> Unit = {},
) {
    LexiDueScreen(
        title = stringResource(R.string.settings_title),
        subtitle = stringResource(R.string.settings_subtitle),
        modifier = modifier,
    ) {
        SectionHeading(stringResource(R.string.settings_session_length_heading))
        Text(
            text = stringResource(R.string.settings_session_length_description),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        listOf(5, 10, 15).forEach { wordCount ->
            ChoiceButton(
                label =
                    pluralStringResource(
                        R.plurals.settings_word_count,
                        wordCount,
                        wordCount,
                    ),
                selected = uiState.sessionLength == wordCount,
                onClick = { onAction(SettingsUiAction.SetSessionLength(wordCount)) },
            )
        }

        SectionHeading(stringResource(R.string.settings_difficulty_heading))
        Text(
            text = stringResource(R.string.settings_difficulty_description),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        PracticeDifficulty.entries.forEach { difficulty ->
            ChoiceButton(
                label = difficultyLabel(difficulty),
                selected = uiState.difficulty == difficulty,
                onClick = { onAction(SettingsUiAction.SetDifficulty(difficulty)) },
            )
        }

        SectionHeading(stringResource(R.string.settings_theme_heading))
        ThemeMode.entries.forEach { themeMode ->
            ChoiceButton(
                label = themeModeLabel(themeMode),
                selected = uiState.themeMode == themeMode,
                onClick = { onAction(SettingsUiAction.SetThemeMode(themeMode)) },
            )
        }

        SectionHeading(stringResource(R.string.settings_effects_heading))
        SettingSwitchRow(
            title = stringResource(R.string.settings_sound_title),
            description = stringResource(R.string.settings_sound_description),
            checked = uiState.soundEnabled,
            onCheckedChange = { onAction(SettingsUiAction.SetSoundEnabled(it)) },
        )
        SettingSwitchRow(
            title = stringResource(R.string.settings_haptics_title),
            description = stringResource(R.string.settings_haptics_description),
            checked = uiState.hapticsEnabled,
            onCheckedChange = { onAction(SettingsUiAction.SetHapticsEnabled(it)) },
        )
        SettingSwitchRow(
            title = stringResource(R.string.settings_reduced_motion_title),
            description = stringResource(R.string.settings_reduced_motion_description),
            checked = uiState.reducedMotion,
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

        OutlinedButton(
            onClick = { onAction(SettingsUiAction.RequestResetData) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .semantics { role = Role.Button },
        ) {
            Text(stringResource(R.string.action_reset_learning_data))
        }
        Text(
            text = stringResource(R.string.settings_reset_note),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ChoiceButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val selectedDescription =
        stringResource(
            if (selected) R.string.state_selected else R.string.state_not_selected,
        )
    OutlinedButton(
        onClick = onClick,
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .semantics {
                    role = Role.Button
                    stateDescription = selectedDescription
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
private fun SettingSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
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
                onCheckedChange = onCheckedChange,
                modifier =
                    Modifier
                        .minimumInteractiveComponentSize()
                        .semantics {
                            contentDescription = title
                            role = Role.Switch
                        },
            )
        }
    }
}

@Composable
private fun difficultyLabel(difficulty: PracticeDifficulty): String =
    when (difficulty) {
        PracticeDifficulty.FOUNDATION -> stringResource(R.string.settings_difficulty_foundation)
        PracticeDifficulty.STANDARD -> stringResource(R.string.settings_difficulty_standard)
        PracticeDifficulty.CHALLENGE -> stringResource(R.string.settings_difficulty_challenge)
    }

@Composable
private fun themeModeLabel(themeMode: ThemeMode): String =
    when (themeMode) {
        ThemeMode.SYSTEM -> stringResource(R.string.settings_theme_system)
        ThemeMode.LIGHT -> stringResource(R.string.settings_theme_light)
        ThemeMode.DARK -> stringResource(R.string.settings_theme_dark)
    }
