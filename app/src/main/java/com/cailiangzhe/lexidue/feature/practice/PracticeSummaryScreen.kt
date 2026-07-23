package com.cailiangzhe.lexidue.feature.practice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.cailiangzhe.lexidue.R
import com.cailiangzhe.lexidue.core.designsystem.component.LexiDueScreen
import com.cailiangzhe.lexidue.core.designsystem.component.MetricCard
import com.cailiangzhe.lexidue.core.designsystem.component.SectionHeading

object PracticeSummaryTestTags {
    const val CONTENT = "practice_summary_content"
    const val DONE = "practice_summary_done"
}

@Composable
fun PracticeSummaryScreen(
    onDone: () -> Unit,
    onOpenStatistics: () -> Unit,
    modifier: Modifier = Modifier,
    uiState: PracticeSummaryUiState = PracticeSummaryUiState(),
) {
    LexiDueScreen(
        title = stringResource(R.string.practice_summary_title),
        subtitle = stringResource(R.string.practice_summary_subtitle),
        modifier = modifier,
    ) {
        when {
            uiState.isLoading -> {
                SummaryLoading()
            }

            uiState.errorMessage != null -> {
                SummaryError(uiState.errorMessage)
            }

            else -> {
                SummaryContent(
                    uiState = uiState,
                    onDone = onDone,
                    onOpenStatistics = onOpenStatistics,
                )
            }
        }
    }
}

@Composable
private fun SummaryLoading() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CircularProgressIndicator()
        Text(stringResource(R.string.practice_summary_loading))
    }
}

@Composable
private fun SummaryError(message: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.practice_summary_unavailable),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(message)
        }
    }
}

@Composable
private fun SummaryContent(
    uiState: PracticeSummaryUiState,
    onDone: () -> Unit,
    onOpenStatistics: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag(PracticeSummaryTestTags.CONTENT),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SectionHeading(stringResource(R.string.practice_summary_results_heading))
        MetricCard(
            label = stringResource(R.string.practice_summary_accuracy),
            value = stringResource(R.string.practice_summary_accuracy_value, uiState.accuracyPercent),
            supportingText = stringResource(R.string.practice_summary_skips_excluded),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MetricCard(
                label = stringResource(R.string.practice_summary_correct),
                value = uiState.correctCount.toString(),
                modifier = Modifier.weight(1f),
            )
            MetricCard(
                label = stringResource(R.string.practice_summary_not_yet),
                value = uiState.incorrectCount.toString(),
                modifier = Modifier.weight(1f),
            )
        }
        MetricCard(
            label = stringResource(R.string.practice_summary_skipped),
            value = uiState.skippedCount.toString(),
            supportingText = stringResource(R.string.practice_summary_retry_value, uiState.retryCount),
        )

        SectionHeading(stringResource(R.string.practice_summary_review_heading))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (uiState.reviewWords.isEmpty()) {
                    Text(stringResource(R.string.practice_summary_no_review_words))
                } else {
                    uiState.reviewWords.forEach { word ->
                        Text(
                            text = word,
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
                uiState.completedAtLabel?.let { label ->
                    Text(
                        text = stringResource(R.string.practice_summary_completed_at, label),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Button(
            onClick = onDone,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag(PracticeSummaryTestTags.DONE)
                    .semantics { role = Role.Button },
        ) {
            Text(stringResource(R.string.action_return_home))
        }
        OutlinedButton(
            onClick = onOpenStatistics,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .semantics { role = Role.Button },
        ) {
            Text(stringResource(R.string.action_open_statistics))
        }
    }
}
