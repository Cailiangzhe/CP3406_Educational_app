package com.cailiangzhe.lexidue.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun HomeRouteScreen(
    onOpenPractice: (String) -> Unit,
    onOpenStatistics: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is HomeEffect.OpenPractice -> onOpenPractice(effect.sessionId)
                HomeEffect.OpenStatistics -> onOpenStatistics()
                HomeEffect.OpenSettings -> onOpenSettings()
            }
        }
    }

    HomeScreen(
        onStartPractice = {},
        onOpenStatistics = {},
        onOpenSettings = {},
        modifier = modifier,
        uiState = uiState,
        onAction = viewModel::onAction,
    )
}
