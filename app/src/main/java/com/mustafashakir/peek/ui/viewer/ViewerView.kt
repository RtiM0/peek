package com.mustafashakir.peek.ui.viewer

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mustafashakir.peek.R
import com.mustafashakir.peek.ui.components.AuthorByline
import com.mustafashakir.peek.ui.components.CaptionText
import com.mustafashakir.peek.ui.components.CommentsSection
import com.mustafashakir.peek.ui.components.PeekBuzzingEyeBadge
import com.mustafashakir.peek.ui.components.PeekImage
import com.mustafashakir.peek.ui.components.PeekLockup
import com.mustafashakir.peek.ui.model.ViewerPostUiModel
import com.mustafashakir.peek.ui.model.ViewerUiState
import com.mustafashakir.peek.ui.model.ViewerMediaItemUiModel
import com.mustafashakir.peek.ui.theme.Geist
import com.mustafashakir.peek.ui.theme.GeistMono
import com.mustafashakir.peek.ui.theme.Inter
import com.mustafashakir.peek.ui.theme.PeekAccent
import com.mustafashakir.peek.ui.theme.PeekBorder
import com.mustafashakir.peek.ui.theme.PeekChip
import com.mustafashakir.peek.ui.theme.PeekGround
import com.mustafashakir.peek.ui.theme.PeekInk
import com.mustafashakir.peek.ui.theme.PeekMuted
import com.mustafashakir.peek.ui.theme.PeekSecondary
import com.mustafashakir.peek.ui.theme.PeekTile
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
fun ViewerView(
    uiState: ViewerUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onLoadMoreComments: () -> Unit = {},
    onOpenMedia: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize().background(PeekGround), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier.widthIn(max = 390.dp).fillMaxWidth().fillMaxHeight(),
        ) {
            when (uiState) {
                is ViewerUiState.Loading -> LoadingViewer(uiState.progress, uiState.message, onBack, onRefresh)
                is ViewerUiState.Unavailable -> UnavailableViewer(uiState.url, onBack)
                is ViewerUiState.Content -> ViewerContent(
                    post = uiState.post,
                    isLoadingMoreComments = uiState.isLoadingMoreComments,
                    onBack = onBack,
                    onRefresh = onRefresh,
                    onLoadMoreComments = onLoadMoreComments,
                    onOpenMedia = onOpenMedia,
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.ViewerContent(
    post: ViewerPostUiModel,
    isLoadingMoreComments: Boolean,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onLoadMoreComments: () -> Unit,
    onOpenMedia: (Int) -> Unit,
) {
    ViewerHeader(post.isVideo, onBack, onRefresh)
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(scrollState).padding(horizontal = 14.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        MediaCanvas(post, onOpenMedia)
        AuthorByline(post)
        CaptionText(post)
        CommentsSection(
            post = post,
            isLoadingMore = isLoadingMoreComments,
            scrollOffset = scrollState.value,
            onLoadMore = onLoadMoreComments,
        )
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun ViewerHeader(isVideo: Boolean, onBack: () -> Unit, onRefresh: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(50.dp).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(
            modifier = Modifier.size(48.dp).offset(x = (-8).dp).clip(CircleShape).clickable(role = Role.Button, onClick = onBack),
            contentAlignment = Alignment.CenterStart,
        ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back), tint = PeekInk, modifier = Modifier.size(22.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(
                text = if (isVideo) "▶" else "+",
                color = PeekAccent,
                style = TextStyle(fontFamily = GeistMono, fontSize = if (isVideo) 10.sp else 15.sp, fontWeight = FontWeight.Bold),
            )
            Text(
                text = stringResource(if (isVideo) R.string.video_preview else R.string.viewing_post),
                color = PeekInk,
                style = TextStyle(fontFamily = GeistMono, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp),
            )
        }
        Box(
            modifier = Modifier.size(48.dp).offset(x = 8.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onRefresh),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Icon(Icons.Rounded.Refresh, contentDescription = stringResource(R.string.refresh), tint = PeekInk, modifier = Modifier.size(21.dp))
        }
    }
}

@Composable
private fun MediaCanvas(post: ViewerPostUiModel, onOpenMedia: (Int) -> Unit) {
    val items = post.mediaItems.ifEmpty {
        listOf(
            ViewerMediaItemUiModel(
                id = "primary",
                image = post.media,
                contentDescription = post.mediaDescription,
                videoUrl = post.videoUrl,
            ),
        )
    }
    val pagerState = rememberPagerState(
        initialPage = post.initialMediaIndex.coerceIn(0, items.lastIndex),
        pageCount = items::size,
    )
    val coroutineScope = rememberCoroutineScope()
    val mediaHeight = if (items.any { it.videoUrl != null }) 288.dp else 244.dp
    Box(
        modifier = Modifier.fillMaxWidth().height(mediaHeight).clip(RoundedCornerShape(12.dp)).border(1.dp, PeekBorder, RoundedCornerShape(12.dp)),
    ) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            val item = items[page]
            Box(Modifier.fillMaxSize()) {
                PeekImage(
                    image = item.image,
                    contentDescription = item.contentDescription,
                    modifier = Modifier.fillMaxSize().clickable(
                        role = Role.Button,
                        onClick = { onOpenMedia(page) },
                    ),
                )
                if (item.videoUrl != null) {
                    Box(
                        modifier = Modifier.align(Alignment.Center).size(62.dp).shadow(8.dp, CircleShape).clip(CircleShape).background(PeekGround.copy(alpha = 0.91f)).clickable(
                            role = Role.Button,
                            onClick = { onOpenMedia(page) },
                        ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = stringResource(R.string.play_video), tint = PeekAccent, modifier = Modifier.size(30.dp))
                    }
                }
            }
        }
        post.mediaBadge?.let { badge ->
            if (items.size == 1 || badge == "CAROUSEL") {
                Row(
                    modifier = Modifier.align(Alignment.TopStart).padding(12.dp).height(28.dp).clip(CircleShape).background(PeekGround.copy(alpha = 0.91f)).padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (items[pagerState.currentPage].videoUrl != null) {
                        Icon(Icons.Rounded.Videocam, contentDescription = null, tint = PeekInk, modifier = Modifier.size(13.dp))
                    }
                    Text(badge, color = PeekInk, style = TextStyle(fontFamily = GeistMono, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.7.sp))
                }
            }
        }
        if (items.size > 1) {
            CarouselButton(
                icon = Icons.Rounded.ChevronLeft,
                contentDescription = stringResource(R.string.previous_media),
                enabled = pagerState.currentPage > 0,
                onClick = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage((pagerState.currentPage - 1).coerceAtLeast(0))
                    }
                },
                modifier = Modifier.align(Alignment.CenterStart).padding(8.dp),
            )
            CarouselButton(
                icon = Icons.Rounded.ChevronRight,
                contentDescription = stringResource(R.string.next_media),
                enabled = pagerState.currentPage < items.lastIndex,
                onClick = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage((pagerState.currentPage + 1).coerceAtMost(items.lastIndex))
                    }
                },
                modifier = Modifier.align(Alignment.CenterEnd).padding(8.dp),
            )
            Box(
                modifier = Modifier.align(Alignment.BottomCenter).padding(10.dp).clip(CircleShape).background(PeekInk.copy(alpha = 0.78f)).padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Text(
                    text = stringResource(R.string.carousel_position, pagerState.currentPage + 1, items.size),
                    color = Color.White,
                    style = TextStyle(fontFamily = GeistMono, fontSize = 9.sp, fontWeight = FontWeight.Bold),
                )
            }
        } else {
            post.duration?.let { duration ->
                Box(
                    modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp).height(28.dp).clip(CircleShape).background(PeekInk.copy(alpha = 0.85f)).padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(duration, color = Color.White, style = TextStyle(fontFamily = GeistMono, fontSize = 9.sp, fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}

@Composable
private fun CarouselButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(36.dp)
            .shadow(4.dp, CircleShape)
            .clip(CircleShape)
            .background(PeekGround.copy(alpha = if (enabled) 0.92f else 0.55f))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = if (enabled) PeekInk else PeekMuted,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun LoadingViewer(progress: Float, message: String, onBack: () -> Unit, onRefresh: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        LoadingHeader(onBack, onRefresh)
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PeekBuzzingEyeBadge()
            Spacer(Modifier.height(18.dp))
            Text(
                text = stringResource(R.string.peeking_title),
                color = PeekInk,
                style = TextStyle(fontFamily = Geist, fontSize = 27.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-1.1).sp),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.peeking_subtitle),
                color = PeekSecondary,
                style = TextStyle(fontFamily = Inter, fontSize = 14.sp),
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            Spacer(Modifier.height(24.dp))
            LoadingProgressCard(progress, message)
        }
        LoadingFooter(onBack)
    }
}

@Composable
private fun LoadingHeader(onBack: () -> Unit, onRefresh: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        LoadingHeaderPill(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back), tint = PeekInk, modifier = Modifier.size(18.dp))
        }
        PeekLockup()
        LoadingHeaderPill(onClick = onRefresh) {
            Icon(Icons.Rounded.Refresh, contentDescription = stringResource(R.string.refresh), tint = PeekInk, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun LoadingHeaderPill(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(PeekChip)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun LoadingProgressCard(progress: Float, message: String) {
    val animatedProgress by animateFloatAsState(targetValue = progress.coerceIn(0f, 1f), animationSpec = tween(300), label = "loadingProgress")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(PeekChip)
            .padding(start = 16.dp, top = 16.dp, end = 14.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = stringResource(R.string.fetching_post_label),
                color = PeekSecondary,
                style = TextStyle(fontFamily = GeistMono, fontSize = 9.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.sp),
            )
            Text(
                text = "${(animatedProgress * 100).roundToInt()}%",
                color = PeekAccent,
                style = TextStyle(fontFamily = GeistMono, fontSize = 11.sp, fontWeight = FontWeight.Bold),
            )
        }
        Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(PeekBorder)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(PeekAccent),
            )
        }
        Text(text = message, color = PeekSecondary, style = TextStyle(fontFamily = Inter, fontSize = 12.sp))
    }
}

@Composable
private fun LoadingFooter(onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.loading_hint),
            color = PeekMuted,
            style = TextStyle(fontFamily = Inter, fontSize = 12.sp),
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .border(1.dp, PeekBorder, RoundedCornerShape(50))
                .clickable(role = Role.Button, onClick = onBack)
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(Icons.Rounded.Close, contentDescription = null, tint = PeekInk, modifier = Modifier.size(14.dp))
            Text(
                text = stringResource(R.string.cancel),
                color = PeekInk,
                style = TextStyle(fontFamily = Inter, fontSize = 14.sp, fontWeight = FontWeight.Bold),
            )
        }
    }
}

@Composable
private fun UnavailableViewer(url: String, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        UnsupportedHeader(onBack)
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 22.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier.size(76.dp).clip(CircleShape).background(PeekTile),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.LinkOff, contentDescription = null, tint = PeekAccent, modifier = Modifier.size(34.dp))
            }
            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.height(27.dp).clip(CircleShape).background(PeekChip).padding(horizontal = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(Icons.Rounded.LinkOff, contentDescription = null, tint = PeekSecondary, modifier = Modifier.size(13.dp))
                Text(
                    text = stringResource(R.string.unsupported_link_label),
                    color = PeekSecondary,
                    style = TextStyle(fontFamily = GeistMono, fontSize = 8.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp),
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.unsupported_link_title),
                modifier = Modifier.fillMaxWidth(),
                color = PeekInk,
                textAlign = TextAlign.Center,
                style = TextStyle(fontFamily = Geist, fontSize = 27.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-1.1).sp),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.unsupported_link_description),
                modifier = Modifier.fillMaxWidth(),
                color = PeekSecondary,
                textAlign = TextAlign.Center,
                style = TextStyle(fontFamily = Inter, fontSize = 14.sp, lineHeight = 20.sp),
            )
            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(PeekChip).padding(horizontal = 15.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier.size(34.dp).clip(CircleShape).background(PeekGround),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Link, contentDescription = null, tint = PeekAccent, modifier = Modifier.size(16.dp))
                }
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = stringResource(R.string.link_source),
                        color = PeekSecondary,
                        style = TextStyle(fontFamily = GeistMono, fontSize = 8.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.7.sp),
                    )
                    Text(
                        text = url,
                        color = PeekInk,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(fontFamily = Inter, fontSize = 11.sp, lineHeight = 15.sp),
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            Row(
                modifier = Modifier.clip(CircleShape).border(1.dp, PeekBorder, CircleShape).clickable(role = Role.Button, onClick = onBack).padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Rounded.Home, contentDescription = null, tint = PeekInk, modifier = Modifier.size(14.dp))
                Text(
                    text = stringResource(R.string.home),
                    color = PeekInk,
                    style = TextStyle(fontFamily = Inter, fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                )
            }
        }
    }
}

@Composable
private fun UnsupportedHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        LoadingHeaderPill(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back), tint = PeekInk, modifier = Modifier.size(18.dp))
        }
        PeekLockup()
        Box(
            modifier = Modifier.size(38.dp).clip(CircleShape).background(PeekChip),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.MoreHoriz, contentDescription = stringResource(R.string.more_options), tint = PeekSecondary, modifier = Modifier.size(20.dp))
        }
    }
}
