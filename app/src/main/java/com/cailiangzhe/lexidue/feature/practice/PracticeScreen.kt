package com.cailiangzhe.lexidue.feature.practice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.cailiangzhe.lexidue.R
import com.cailiangzhe.lexidue.core.designsystem.component.LexiDueScreen
import com.cailiangzhe.lexidue.core.designsystem.component.SectionHeading

@Composable
fun PracticeScreen(
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    uiState: PracticeUiState = PracticeUiState(),
    onAction: (PracticeUiAction) -> Unit = {},
) {
    val safeTotal = uiState.totalQuestions.coerceAtLeast(1)
    val safeQuestion = uiState.questionNumber.coerceIn(1, safeTotal)
    val progress = safeQuestion.toFloat() / safeTotal
    val progressDescription =
        stringResource(
            R.string.practice_progress_description,
            safeQuestion,
            safeTotal,
        )
    val selectedDescription = stringResource(R.string.state_selected)

    LexiDueScreen(
        title = stringResource(R.string.practice_title),
        subtitle = stringResource(R.string.practice_subtitle),
        modifier = modifier,
    ) {
        LinearProgressIndicator(
            progress = { progress },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = progressDescription
                        progressBarRangeInfo =
                            ProgressBarRangeInfo(
                                current = progress,
                                range = 0f..1f,
                            )
                    },
        )
        Text(
            text = progressDescription,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.practice_question_instruction),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = uiState.promptWord,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }

        SectionHeading(stringResource(R.string.practice_answers_heading))
        uiState.choices.forEachIndexed { index, choice ->
            val answerDescription =
                stringResource(
                    R.string.practice_answer_description,
                    index + 1,
                    uiState.choices.size,
                    choice.text,
                )
            val isSelected = choice.id == uiState.selectedChoiceId
            OutlinedButton(
                onClick = { onAction(PracticeUiAction.SelectAnswer(choice.id)) },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .semantics {
                            contentDescription = answerDescription
                            role = Role.Button
                            if (isSelected) {
                                stateDescription = selectedDescription
                            }
                        },
                enabled = uiState.answersEnabled,
            ) {
                Text(choice.text)
            }
        }

        uiState.feedback?.let { feedback ->
            val feedbackLabel =
                when (feedback.kind) {
                    PracticeFeedbackKind.CORRECT -> stringResource(R.string.practice_correct_label)
                    PracticeFeedbackKind.INCORRECT -> stringResource(R.string.practice_incorrect_label)
                }
            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .semantics(mergeDescendants = true) {
                            contentDescription = "$feedbackLabel. ${feedback.message}"
                        },
                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            if (feedback.kind == PracticeFeedbackKind.CORRECT) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.tertiaryContainer
                            },
                    ),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = feedbackLabel,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = feedback.message,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }

        Button(
            onClick = { onAction(PracticeUiAction.Skip) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .semantics { role = Role.Button },
        ) {
            Text(stringResource(R.string.action_skip_question))
        }
        OutlinedButton(
            onClick = {
                onAction(PracticeUiAction.Exit)
                onExit()
            },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .semantics { role = Role.Button },
        ) {
            Text(stringResource(R.string.action_exit_practice))
        }
        Text(
            text = stringResource(R.string.practice_skip_exit_note),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
