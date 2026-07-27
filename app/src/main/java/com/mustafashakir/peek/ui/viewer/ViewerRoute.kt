package com.mustafashakir.peek.ui.viewer

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mustafashakir.peek.ui.actions.rememberPostActionCallbacks

@Composable
fun ViewerRoute(
    viewModel: ViewerViewModel,
    onBack: () -> Unit,
    onOpenMedia: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewerUiState by viewModel.uiState.collectAsStateWithLifecycle()
    val callbacks = rememberPostActionCallbacks()

    ViewerView(
        uiState = viewerUiState,
        onBack = onBack,
        onRefresh = viewModel::onRefresh,
        onLoadMoreComments = viewModel::onLoadMoreComments,
        onOpenMedia = onOpenMedia,
        onCopyLink = callbacks.onCopyLink,
        onCopyMedia = callbacks.onCopyMedia,
        onDownload = callbacks.onDownload,
        onShare = callbacks.onShare,
        modifier = modifier.fillMaxSize(),
    )
}
