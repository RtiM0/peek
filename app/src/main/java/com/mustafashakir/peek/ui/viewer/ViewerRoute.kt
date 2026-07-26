package com.mustafashakir.peek.ui.viewer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ViewerRoute(
    viewModel: ViewerViewModel,
    onBack: () -> Unit,
    onOpenMedia: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewerUiState by viewModel.uiState.collectAsStateWithLifecycle()
    ViewerView(
        uiState = viewerUiState,
        onBack = onBack,
        onRefresh = viewModel::onRefresh,
        onLoadMoreComments = viewModel::onLoadMoreComments,
        onOpenMedia = onOpenMedia,
        modifier = modifier,
    )
}
