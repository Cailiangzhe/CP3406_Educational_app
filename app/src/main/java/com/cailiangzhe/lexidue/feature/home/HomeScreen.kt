package com.cailiangzhe.lexidue.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.cailiangzhe.lexidue.R
import com.cailiangzhe.lexidue.core.designsystem.component.LexiDueScreen
import com.cailiangzhe.lexidue.core.designsystem.component.MetricCard
import com.cailiangzhe.lexidue.core.designsystem.component.SectionHeading
import com.cailiangzhe.lexidue.domain.model.PracticeDifficulty
import java.text.DateFormat
import java.util.Date

@Composable
fun HomeScreen(
    onStartPractice: () -> Unit,
    onOpenStatistics: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    uiState: HomeUiState = HomeUiState(),
    onAction: (HomeUiAction) -> Unit = { action ->
        when (action) {
            HomeUiAction.StartPractice -> onStartPractice()
            HomeUiAction.OpenStatistics -> onOpenStatistics()
            HomeUiAction.OpenSettings -> onOpenSettings()
            HomeUiAction.EnrichDeck -> Unit
            HomeUiAction.RetryLocalData -> Unit
            HomeUiAction.RetryEnrichmentCache -> Unit
        }
    },
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
                Text(
                    text =
                        stringResource(
                            R.string.home_practice_preferences,
                            uiState.sessionLength,
                            when (uiState.difficulty) {
                                PracticeDifficulty.FOUNDATION -> stringResource(R.string.difficulty_foundation)
                                PracticeDifficulty.STANDARD -> stringResource(R.string.difficulty_standard)
                                PracticeDifficulty.CHALLENGE -> stringResource(R.string.difficulty_challenge)
                            },
                        ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                if (uiState.isLoading) {
                    Text(
                        text = stringResource(R.string.home_loading_local_data),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                if (!uiState.hasLoadedSettings) {
                    Text(
                        text = stringResource(R.string.home_loading_settings),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Button(
                    onClick = { onAction(HomeUiAction.StartPractice) },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .semantics { role = Role.Button },
                    enabled = uiState.startEnabled,
                ) {
                    Text(
                        stringResource(
                            if (uiState.isStarting) {
                                R.string.action_starting_practice
                            } else {
                                R.string.action_start_practice
                            },
                        ),
                    )
                }
                uiState.errorMessage?.let { errorMessage ->
                    Text(
                        text = stringResource(R.string.home_local_error, errorMessage),
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    OutlinedButton(
                        onClick = { onAction(HomeUiAction.RetryLocalData) },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .semantics { role = Role.Button },
                    ) {
                        Text(stringResource(R.string.action_try_again))
                    }
                }
            }
        }

        SectionHeading(stringResource(R.string.home_overview_heading))
        MetricCard(
            label = stringResource(R.string.home_due_label),
            value = uiState.dueWordCount.toString(),
            supportingText = stringResource(R.string.home_due_supporting),
        )
        MetricCard(
            label = stringResource(R.string.home_mastered_label),
            value = uiState.masteredWordCount.toString(),
        )

        SectionHeading(stringResource(R.string.home_enrichment_heading))
        Text(
            text = stringResource(R.string.home_enrichment_description),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.home_enrichment_progress),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        OutlinedButton(
            onClick = { onAction(HomeUiAction.EnrichDeck) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .semantics { role = Role.Button },
            enabled = uiState.enrichEnabled,
        ) {
            Text(
                stringResource(
                    if (uiState.isEnriching) {
                        R.string.action_enriching_deck
                    } else {
                        R.string.action_enrich_deck
                    },
                ),
            )
        }
        val enrichmentStatusMessage =
            when (val status = uiState.enrichmentStatus) {
                HomeEnrichmentStatus.Idle -> {
                    null
                }

                HomeEnrichmentStatus.Refreshing -> {
                    stringResource(R.string.home_enrichment_refreshing)
                }

                HomeEnrichmentStatus.NoDueWords -> {
                    stringResource(R.string.home_enrichment_no_due)
                }

                is HomeEnrichmentStatus.Saved -> {
                    if (status.refreshedWordCount == 0) {
                        stringResource(
                            R.string.home_enrichment_all_fresh,
                            status.freshWordCount,
                        )
                    } else {
                        stringResource(
                            R.string.home_enrichment_saved,
                            status.refreshedWordCount,
                            status.freshWordCount,
                        )
                    }
                }

                is HomeEnrichmentStatus.PartialFailure -> {
                    stringResource(
                        R.string.home_enrichment_partial,
                        status.refreshedWordCount,
                        status.freshWordCount,
                        status.failedWordCount,
                    )
                }

                is HomeEnrichmentStatus.Failed -> {
                    stringResource(
                        R.string.home_enrichment_failed,
                        status.failedWordCount,
                    )
                }
            }
        enrichmentStatusMessage?.let { message ->
            Text(
                text = message,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = MaterialTheme.typography.bodyMedium,
                color =
                    when (uiState.enrichmentStatus) {
                        is HomeEnrichmentStatus.Failed,
                        is HomeEnrichmentStatus.PartialFailure,
                        -> MaterialTheme.colorScheme.error

                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
            )
        }
        uiState.lastSuccessfulRefreshAtEpochMillis?.let { fetchedAt ->
            val formattedTime =
                remember(fetchedAt) {
                    DateFormat
                        .getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                        .format(Date(fetchedAt))
                }
            Text(
                text = stringResource(R.string.home_last_refresh, formattedTime),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SectionHeading(stringResource(R.string.home_saved_enrichment_heading))
        if (uiState.isLoadingEnrichmentCache) {
            Text(
                text = stringResource(R.string.home_loading_enrichment_cache),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (uiState.cacheReadError) {
            Text(
                text = stringResource(R.string.home_enrichment_cache_unavailable),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
            OutlinedButton(
                onClick = { onAction(HomeUiAction.RetryEnrichmentCache) },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .semantics { role = Role.Button },
            ) {
                Text(stringResource(R.string.action_retry_saved_content))
            }
        }
        if (
            !uiState.isLoadingEnrichmentCache &&
            !uiState.cacheReadError &&
            uiState.enrichmentCards.isEmpty()
        ) {
            Text(
                text = stringResource(R.string.home_no_saved_enrichment),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (uiState.enrichmentCards.isNotEmpty()) {
            uiState.enrichmentCards.forEach { card ->
                DictionaryEnrichmentCard(card)
            }
        }

        SectionHeading(stringResource(R.string.home_navigation_heading))
        OutlinedButton(
            onClick = { onAction(HomeUiAction.OpenStatistics) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .semantics { role = Role.Button },
        ) {
            Text(stringResource(R.string.action_open_statistics))
        }
        OutlinedButton(
            onClick = { onAction(HomeUiAction.OpenSettings) },
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

@Composable
private fun DictionaryEnrichmentCard(card: HomeEnrichmentCard) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = card.displayWord,
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            card.senses.forEachIndexed { index, sense ->
                val partOfSpeech =
                    sense.partOfSpeech.name
                        .lowercase()
                        .replaceFirstChar(Char::uppercase)
                Text(
                    text = listOfNotNull(partOfSpeech, sense.phonetic).joinToString("  "),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = sense.definition,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                sense.example?.let { example ->
                    Text(
                        text = stringResource(R.string.home_enrichment_example, example),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (index != card.senses.lastIndex) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                }
            }
            Text(
                text =
                    stringResource(
                        if (card.isStale) {
                            R.string.home_enrichment_stale
                        } else {
                            R.string.home_enrichment_fresh
                        },
                    ),
                style = MaterialTheme.typography.labelLarge,
                color =
                    if (card.isStale) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
            )
            card.senses.firstOrNull()?.let { sense ->
                Text(
                    text =
                        stringResource(
                            R.string.home_enrichment_source,
                            sense.provider,
                            sense.source,
                        ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
