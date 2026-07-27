package com.mustafashakir.peek.ui.player

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mustafashakir.peek.ui.actions.rememberPostActionCallbacks

@Composable
fun PlayerRoute(
    viewModel: PlayerViewModel,
    initialMediaIndex: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val callbacks = rememberPostActionCallbacks()

    PlayerView(
        uiState = uiState,
        initialMediaIndex = initialMediaIndex,
        onBack = onBack,
        onMore = {},
        onLoadMoreComments = viewModel::onLoadMoreComments,
        onCopyLink = callbacks.onCopyLink,
        onCopyMedia = callbacks.onCopyMedia,
        onDownload = callbacks.onDownload,
        onShare = callbacks.onShare,
        modifier = modifier.fillMaxSize(),
    )
}
