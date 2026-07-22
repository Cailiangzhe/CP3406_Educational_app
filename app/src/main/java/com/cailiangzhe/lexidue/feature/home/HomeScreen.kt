package com.cailiangzhe.lexidue.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.cailiangzhe.lexidue.R
import com.cailiangzhe.lexidue.core.designsystem.component.LexiDueScreen
import com.cailiangzhe.lexidue.core.designsystem.component.MetricCard
import com.cailiangzhe.lexidue.core.designsystem.component.SectionHeading

@Composable
fun HomeScreen(
    onStartPractice: () -> Unit,
    onOpenStatistics: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    uiState: HomeUiState = HomeUiState(),
    onAction: (HomeUiAction) -> Unit = {},
) {
    LexiDueScreen(
        title = stringResource(R.string.home_title),
        subtitle = stringResource(R.string.home_subtitle),
        modifier = modifier,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.home_ready_heading),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text =
                        pluralStringResource(
                            R.plurals.home_session_summary,
                            uiState.dueWordCount,
                            uiState.sessionLength,
                            uiState.dueWordCount,
                        ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Button(
                    onClick = {
                        onAction(HomeUiAction.StartPractice)
                        onStartPractice()
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .semantics { role = Role.Button },
                ) {
                    Text(stringResource(R.string.action_start_practice))
                }
            }
        }

        SectionHeading(stringResource(R.string.home_overview_heading))
        MetricCard(
            label = stringResource(R.string.home_due_label),
            value = uiState.dueWordCount.toString(),
            supportingText = stringResource(R.string.home_due_supporting),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MetricCard(
                label = stringResource(R.string.home_review_label),
                value = uiState.reviewWordCount.toString(),
                modifier = Modifier.weight(1f),
            )
            MetricCard(
                label = stringResource(R.string.home_mastered_label),
                value = uiState.masteredWordCount.toString(),
                modifier = Modifier.weight(1f),
            )
        }

        SectionHeading(stringResource(R.string.home_enrichment_heading))
        Text(
            text = stringResource(R.string.home_enrichment_description),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(
            onClick = { onAction(HomeUiAction.EnrichDeck) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .semantics { role = Role.Button },
            enabled = !uiState.isEnriching,
        ) {
            Text(
                if (uiState.isEnriching) {
                    stringResource(R.string.action_enriching_deck)
                } else {
                    stringResource(R.string.action_enrich_deck)
                },
            )
        }
        uiState.lastRefreshLabel?.let {
            Text(
                text = stringResource(R.string.home_last_refresh, it),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SectionHeading(stringResource(R.string.home_navigation_heading))
        OutlinedButton(
            onClick = {
                onAction(HomeUiAction.OpenStatistics)
                onOpenStatistics()
            },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .semantics { role = Role.Button },
        ) {
            Text(stringResource(R.string.action_open_statistics))
        }
        OutlinedButton(
            onClick = {
                onAction(HomeUiAction.OpenSettings)
                onOpenSettings()
            },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .semantics { role = Role.Button },
        ) {
            Text(stringResource(R.string.action_open_settings))
        }
    }
}
