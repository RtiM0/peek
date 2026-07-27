package com.mustafashakir.peek.ui.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.mustafashakir.peek.ui.home.HomeView
import com.mustafashakir.peek.ui.theme.PeekTheme
import com.mustafashakir.peek.ui.viewer.ViewerView

@Preview(name = "Home 390×844", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
fun HomePreview() {
    PeekTheme { HomeView(PeekPreviewFixtures.home, {}, {}) }
}

@Preview(name = "Post 390×844", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
fun PostPreview() {
    PeekTheme { ViewerView(PeekPreviewFixtures.post, onBack = {}, onRefresh = {}, onOpenMedia = {}, onCopyLink = {}, onCopyMedia = {}, onDownload = {}, onShare = {}) }
}

@Preview(name = "Video 390×844", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
fun VideoPreview() {
    PeekTheme { ViewerView(PeekPreviewFixtures.video, onBack = {}, onRefresh = {}, onOpenMedia = {}, onCopyLink = {}, onCopyMedia = {}, onDownload = {}, onShare = {}) }
}

@Preview(name = "Loading 390×844", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
fun LoadingPreview() {
    PeekTheme { ViewerView(PeekPreviewFixtures.loading, onBack = {}, onRefresh = {}, onOpenMedia = {}, onCopyLink = {}, onCopyMedia = {}, onDownload = {}, onShare = {}) }
}

@Preview(name = "Unsupported link 390×844", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
fun UnsupportedLinkPreview() {
    PeekTheme { ViewerView(PeekPreviewFixtures.unavailable, onBack = {}, onRefresh = {}, onOpenMedia = {}, onCopyLink = {}, onCopyMedia = {}, onDownload = {}, onShare = {}) }
}
