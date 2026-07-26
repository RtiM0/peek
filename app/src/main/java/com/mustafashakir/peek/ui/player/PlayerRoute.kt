package com.mustafashakir.peek.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun PlayerRoute(
    viewModel: PlayerViewModel,
    initialMediaIndex: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    PlayerView(
        uiState = uiState,
        initialMediaIndex = initialMediaIndex,
        onBack = onBack,
        onMore = {},
        onLoadMoreComments = viewModel::onLoadMoreComments,
        modifier = modifier,
    )
}
