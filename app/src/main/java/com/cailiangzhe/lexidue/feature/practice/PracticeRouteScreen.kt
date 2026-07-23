package com.cailiangzhe.lexidue.feature.practice

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun PracticeRouteScreen(
    onOpenSummary: (String) -> Unit,
    onReturnHome: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PracticeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is PracticeEffect.OpenSummary -> onOpenSummary(effect.sessionId)
                PracticeEffect.ReturnHome -> onReturnHome()
            }
        }
    }

    BackHandler(enabled = !uiState.showExitConfirmation) {
        viewModel.onAction(PracticeUiAction.RequestExit)
    }

    PracticeScreen(
        modifier = modifier,
        uiState = uiState,
        onAction = viewModel::onAction,
    )
}
