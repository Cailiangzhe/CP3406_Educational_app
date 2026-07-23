package com.cailiangzhe.lexidue.feature.practice

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun PracticeSummaryRouteScreen(
    onDone: () -> Unit,
    onOpenStatistics: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PracticeSummaryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    PracticeSummaryScreen(
        onDone = onDone,
        onOpenStatistics = onOpenStatistics,
        modifier = modifier,
        uiState = uiState,
    )
}
