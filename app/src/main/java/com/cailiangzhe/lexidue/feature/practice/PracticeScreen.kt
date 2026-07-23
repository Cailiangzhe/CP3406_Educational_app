package com.cailiangzhe.lexidue.feature.practice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.cailiangzhe.lexidue.R
import com.cailiangzhe.lexidue.core.designsystem.component.LexiDueScreen
import com.cailiangzhe.lexidue.core.designsystem.component.SectionHeading

object PracticeTestTags {
    const val LOADING = "practice_loading"
    const val QUESTION = "practice_question"
    const val FEEDBACK = "practice_feedback"
    const val CHOICE_PREFIX = "practice_choice_"
    const val CONTINUE = "practice_continue"
    const val EXIT = "practice_exit"
    const val CONFIRM_EXIT = "practice_confirm_exit"
}

@Composable
fun PracticeScreen(
    modifier: Modifier = Modifier,
    uiState: PracticeUiState = PracticeUiState(),
    onAction: (PracticeUiAction) -> Unit = {},
) {
    LexiDueScreen(
        title = stringResource(R.string.practice_title),
        subtitle = stringResource(R.string.practice_subtitle),
        modifier = modifier,
    ) {
        when (val content = uiState.content) {
            PracticeContent.Loading -> {
                PracticeLoading()
            }

            is PracticeContent.Error -> {
                PracticeError(content.message, onAction)
            }

            is PracticeContent.Question -> {
                PracticeQuestion(
                    question = content.question,
                    answersEnabled = content.answersEnabled,
                    selectedChoiceId = null,
                    feedback = null,
                    onAction = onAction,
                )
            }

            is PracticeContent.Feedback -> {
                PracticeQuestion(
                    question = content.question,
                    answersEnabled = false,
                    selectedChoiceId = content.selectedChoiceId,
                    feedback = content.feedback,
                    onAction = onAction,
                )
            }
        }

        OutlinedButton(
            onClick = { onAction(PracticeUiAction.RequestExit) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag(PracticeTestTags.EXIT)
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

    if (uiState.showExitConfirmation) {
        AlertDialog(
            onDismissRequest = { onAction(PracticeUiAction.DismissExit) },
            title = { Text(stringResource(R.string.practice_exit_dialog_title)) },
            text = { Text(stringResource(R.string.practice_exit_dialog_message)) },
            confirmButton = {
                TextButton(
                    onClick = { onAction(PracticeUiAction.ConfirmExit) },
                    modifier = Modifier.testTag(PracticeTestTags.CONFIRM_EXIT),
                ) {
                    Text(stringResource(R.string.practice_exit_dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { onAction(PracticeUiAction.DismissExit) }) {
                    Text(stringResource(R.string.action_keep_practising))
                }
            },
        )
    }
}

@Composable
private fun PracticeLoading() {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag(PracticeTestTags.LOADING),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CircularProgressIndicator()
        Text(stringResource(R.string.practice_loading))
    }
}

@Composable
private fun PracticeError(
    message: String,
    onAction: (PracticeUiAction) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.practice_error_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(message)
            Button(
                onClick = { onAction(PracticeUiAction.RetryLoad) },
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.action_try_again))
            }
        }
    }
}

@Composable
private fun PracticeQuestion(
    question: PracticeQuestionUi,
    answersEnabled: Boolean,
    selectedChoiceId: String?,
    feedback: PracticeFeedback?,
    onAction: (PracticeUiAction) -> Unit,
) {
    val safeTotal = question.totalQuestions.coerceAtLeast(1)
    val safeQuestion = question.questionNumber.coerceIn(1, safeTotal)
    val progress = safeQuestion.toFloat() / safeTotal
    val progressDescription =
        stringResource(
            R.string.practice_progress_description,
            safeQuestion,
            safeTotal,
        )
    val selectedDescription = stringResource(R.string.state_selected)

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
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = progressDescription,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (question.isRetry) {
            Text(
                text = stringResource(R.string.practice_delayed_retry),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
    }

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag(PracticeTestTags.QUESTION),
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
                text =
                    stringResource(
                        when (question.mode) {
                            PracticePromptMode.WORD_TO_DEFINITION -> R.string.practice_word_to_definition_instruction
                            PracticePromptMode.DEFINITION_TO_WORD -> R.string.practice_definition_to_word_instruction
                        },
                    ),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = question.prompt,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }

    SectionHeading(stringResource(R.string.practice_answers_heading))
    question.choices.forEachIndexed { index, choice ->
        val answerDescription =
            stringResource(
                R.string.practice_answer_description,
                index + 1,
                question.choices.size,
                choice.text,
            )
        val isSelected = choice.id == selectedChoiceId
        OutlinedButton(
            onClick = {
                onAction(
                    PracticeUiAction.SelectAnswer(
                        questionId = question.id,
                        choiceId = choice.id,
                    ),
                )
            },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag(PracticeTestTags.CHOICE_PREFIX + index)
                    .semantics {
                        contentDescription = answerDescription
                        role = Role.Button
                        if (isSelected) {
                            stateDescription = selectedDescription
                        }
                    },
            enabled = answersEnabled,
        ) {
            Text(choice.text)
        }
    }

    if (feedback != null) {
        PracticeFeedbackCard(feedback)
        Button(
            onClick = { onAction(PracticeUiAction.Continue(question.id)) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag(PracticeTestTags.CONTINUE),
        ) {
            Text(stringResource(R.string.action_continue_practice))
        }
    } else {
        Button(
            onClick = { onAction(PracticeUiAction.Skip(question.id)) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .semantics { role = Role.Button },
            enabled = answersEnabled,
        ) {
            Text(stringResource(R.string.action_skip_question))
        }
    }
}

@Composable
private fun PracticeFeedbackCard(feedback: PracticeFeedback) {
    val feedbackLabel =
        when (feedback.kind) {
            PracticeFeedbackKind.CORRECT -> stringResource(R.string.practice_correct_label)
            PracticeFeedbackKind.INCORRECT -> stringResource(R.string.practice_incorrect_label)
        }
    val feedbackSymbol =
        when (feedback.kind) {
            PracticeFeedbackKind.CORRECT -> "✓"
            PracticeFeedbackKind.INCORRECT -> "↺"
        }
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag(PracticeTestTags.FEEDBACK)
                .semantics(mergeDescendants = true) {
                    contentDescription = "$feedbackLabel. ${feedback.message}"
                    liveRegion = LiveRegionMode.Polite
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
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = feedbackSymbol,
                style = MaterialTheme.typography.headlineSmall,
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
}
