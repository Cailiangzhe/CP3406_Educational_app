package com.cailiangzhe.lexidue.feature.statistics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.cailiangzhe.lexidue.R
import com.cailiangzhe.lexidue.core.designsystem.component.LexiDueScreen
import com.cailiangzhe.lexidue.core.designsystem.component.MetricCard
import com.cailiangzhe.lexidue.core.designsystem.component.SectionHeading

@Composable
fun StatisticsScreen(
    modifier: Modifier = Modifier,
    uiState: StatisticsUiState = StatisticsUiState(),
) {
    val accuracy = uiState.overallAccuracyPercent.coerceIn(0, 100)
    val accuracyDescription = stringResource(R.string.statistics_accuracy_value, accuracy)

    LexiDueScreen(
        title = stringResource(R.string.statistics_title),
        subtitle = stringResource(R.string.statistics_subtitle),
        modifier = modifier,
    ) {
        SectionHeading(stringResource(R.string.statistics_overview_heading))
        MetricCard(
            label = stringResource(R.string.statistics_total_sessions),
            value = uiState.totalSessions.toString(),
            supportingText =
                pluralStringResource(
                    R.plurals.statistics_attempts_supporting,
                    uiState.gradedAttempts,
                    uiState.gradedAttempts,
                ),
        )
        MetricCard(
            label = stringResource(R.string.statistics_mastered_words),
            value = uiState.masteredWords.toString(),
            supportingText = stringResource(R.string.statistics_mastered_supporting),
        )
        MetricCard(
            label = stringResource(R.string.statistics_due_words),
            value = uiState.dueWords.toString(),
            supportingText = stringResource(R.string.statistics_due_supporting),
        )

        SectionHeading(stringResource(R.string.statistics_accuracy_heading))
        LinearProgressIndicator(
            progress = { accuracy / 100f },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = accuracyDescription
                        progressBarRangeInfo =
                            ProgressBarRangeInfo(
                                current = accuracy / 100f,
                                range = 0f..1f,
                            )
                    },
        )
        Text(
            text = accuracyDescription,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )

        SectionHeading(stringResource(R.string.statistics_review_boxes_heading))
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
                if (uiState.reviewBoxCounts.all { it == 0 }) {
                    Text(
                        text = stringResource(R.string.statistics_no_review_data),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                } else {
                    uiState.reviewBoxCounts.forEachIndexed { index, count ->
                        Text(
                            text =
                                pluralStringResource(
                                    R.plurals.statistics_review_box_value,
                                    count,
                                    index + 1,
                                    count,
                                ),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        }

        SectionHeading(stringResource(R.string.statistics_words_to_review_heading))
        if (uiState.wordsToReview.isEmpty()) {
            EmptyStatisticsCard(stringResource(R.string.statistics_no_words_to_review))
        } else {
            uiState.wordsToReview.forEach { item ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = item.word,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = item.reason,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        SectionHeading(stringResource(R.string.statistics_recent_sessions_heading))
        if (uiState.recentSessions.isEmpty()) {
            EmptyStatisticsCard(stringResource(R.string.statistics_no_recent_sessions))
        } else {
            uiState.recentSessions.forEach { session ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = session.dateLabel,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = session.resultLabel,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyStatisticsCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(20.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
