package com.mustafashakir.peek

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.mustafashakir.peek.ui.home.HomeView
import com.mustafashakir.peek.ui.preview.PeekPreviewFixtures
import com.mustafashakir.peek.ui.theme.PeekTheme
import com.mustafashakir.peek.ui.viewer.ViewerView

@PreviewTest
@Preview(name = "Home Reference", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
fun homeReference() {
    PeekTheme { HomeView(PeekPreviewFixtures.home, {}, {}) }
}

@PreviewTest
@Preview(name = "Post Reference", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
fun postReference() {
    PeekTheme { ViewerView(PeekPreviewFixtures.post, onBack = {}, onRefresh = {}, onOpenMedia = {}) }
}

@PreviewTest
@Preview(name = "Video Reference", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
fun videoReference() {
    PeekTheme { ViewerView(PeekPreviewFixtures.video, onBack = {}, onRefresh = {}, onOpenMedia = {}) }
}

@PreviewTest
@Preview(name = "Loading Reference", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
fun loadingReference() {
    PeekTheme { ViewerView(PeekPreviewFixtures.loading, onBack = {}, onRefresh = {}, onOpenMedia = {}) }
}

@PreviewTest
@Preview(name = "Unsupported Link Reference", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
fun unsupportedLinkReference() {
    PeekTheme { ViewerView(PeekPreviewFixtures.unavailable, onBack = {}, onRefresh = {}, onOpenMedia = {}) }
}

@PreviewTest
@Preview(name = "Home Compact", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
fun homeCompact() {
    PeekTheme { HomeView(PeekPreviewFixtures.home, {}, {}) }
}

@PreviewTest
@Preview(name = "Viewer Wide Phone", widthDp = 411, heightDp = 891, showBackground = true)
@Composable
fun viewerWidePhone() {
    PeekTheme { ViewerView(PeekPreviewFixtures.video, onBack = {}, onRefresh = {}, onOpenMedia = {}) }
}
