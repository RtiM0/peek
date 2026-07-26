package com.mustafashakir.peek.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.mustafashakir.peek.R
import com.mustafashakir.peek.domain.usecase.ExtractUrlFromTextUseCase
import com.mustafashakir.peek.ui.model.HomeUiState
import kotlinx.coroutines.launch

@Composable
fun HomeRoute(
    uiState: HomeUiState,
    onOpenLink: (String) -> Unit,
    modifier: Modifier = Modifier,
    extractUrlFromText: ExtractUrlFromTextUseCase = ExtractUrlFromTextUseCase(),
) {
    val clipboard = LocalClipboard.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val invalidClipboardMessage = stringResource(R.string.clipboard_url_unavailable)

    Box(modifier = modifier.fillMaxSize()) {
        HomeView(
            uiState = uiState,
            onPasteClick = {
                scope.launch {
                    val clipData = clipboard.getClipEntry()?.clipData
                    val clipboardText = clipData
                        ?.takeIf { it.itemCount > 0 }
                        ?.getItemAt(0)
                        ?.coerceToText(context)
                    val url = extractUrlFromText(clipboardText)

                    if (url != null) {
                        onOpenLink(url)
                    } else {
                        snackbarHostState.showSnackbar(invalidClipboardMessage)
                    }
                }
            },
            onRecentLink = onOpenLink,
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}
