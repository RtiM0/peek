package com.mustafashakir.peek.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mustafashakir.peek.R
import com.mustafashakir.peek.ui.model.CommentUiModel
import com.mustafashakir.peek.ui.model.ViewerPostUiModel
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
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AuthorByline(
    post: ViewerPostUiModel,
    onCopyLink: suspend () -> Unit,
    onCopyMedia: suspend () -> Unit,
    onDownload: suspend () -> Unit,
    onShare: suspend () -> Unit,
    modifier: Modifier = Modifier,
    canCopyMedia: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(54.dp).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(Modifier.size(34.dp).clip(CircleShape).background(PeekTile), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Person, contentDescription = null, tint = PeekSecondary, modifier = Modifier.size(17.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(post.authorName, color = PeekInk, style = TextStyle(fontFamily = Inter, fontSize = 12.sp, fontWeight = FontWeight.SemiBold))
                Text(post.authorMetadata, color = PeekMuted, style = TextStyle(fontFamily = GeistMono, fontSize = 8.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.7.sp))
            }
            EllipsisToggleButton(expanded = expanded, onToggle = { expanded = !expanded })
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(animationSpec = tween(220)) + fadeIn(animationSpec = tween(180)),
            exit = shrinkVertically(animationSpec = tween(180)) + fadeOut(animationSpec = tween(120)),
        ) {
            UtilityActionsRow(
                onCopyLink = onCopyLink,
                onCopyMedia = onCopyMedia,
                onDownload = onDownload,
                onShare = onShare,
                canCopyMedia = canCopyMedia,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun EllipsisToggleButton(expanded: Boolean, onToggle: () -> Unit) {
    val backgroundColor by animateColorAsState(if (expanded) PeekAccent else PeekGround, label = "ellipsisBackground")
    val iconTint by animateColorAsState(if (expanded) PeekGround else PeekAccent, label = "ellipsisTint")
    val borderAlpha by animateFloatAsState(if (expanded) 0f else 1f, label = "ellipsisBorderAlpha")
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(1.dp, PeekBorder.copy(alpha = borderAlpha), RoundedCornerShape(8.dp))
            .clickable(role = Role.Button, onClickLabel = stringResource(R.string.media_options), onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Rounded.MoreHoriz, contentDescription = stringResource(R.string.media_options), tint = iconTint, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun UtilityActionsRow(
    onCopyLink: suspend () -> Unit,
    onCopyMedia: suspend () -> Unit,
    onDownload: suspend () -> Unit,
    onShare: suspend () -> Unit,
    canCopyMedia: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(62.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(PeekChip)
            .border(1.dp, PeekBorder, RoundedCornerShape(12.dp))
            .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        UtilityActionButton(Icons.Rounded.Link, stringResource(R.string.copy_link), onCopyLink, Modifier.weight(1f))
        if (canCopyMedia) {
            UtilityActionButton(Icons.Rounded.ContentCopy, stringResource(R.string.copy_media), onCopyMedia, Modifier.weight(1f))
        }
        UtilityActionButton(Icons.Rounded.Download, stringResource(R.string.download), onDownload, Modifier.weight(1f))
        UtilityActionButton(Icons.AutoMirrored.Rounded.Send, stringResource(R.string.share), onShare, Modifier.weight(1f))
    }
}

@Composable
private fun UtilityActionButton(
    icon: ImageVector,
    label: String,
    onClick: suspend () -> Unit,
    modifier: Modifier = Modifier,
) {
    var activated by remember { mutableStateOf(false) }
    var processing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val backgroundColor by animateColorAsState(if (activated) PeekAccent else PeekGround, label = "actionBackground")
    val contentColor by animateColorAsState(if (activated) PeekGround else PeekSecondary, label = "actionContent")
    val scale by animateFloatAsState(if (activated) 0.96f else 1f, label = "actionScale")
    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable(enabled = !processing, role = Role.Button, onClick = {
                activated = true
                processing = true
                scope.launch {
                    try {
                        onClick()
                    } finally {
                        processing = false
                        delay(120)
                        activated = false
                    }
                }
            }),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(17.dp))
        Text(label, color = contentColor, style = TextStyle(fontFamily = GeistMono, fontSize = 8.sp, fontWeight = FontWeight.SemiBold))
        AnimatedVisibility(
            visible = processing,
            enter = fadeIn(animationSpec = tween(120)),
            exit = fadeOut(animationSpec = tween(120)),
        ) {
            LinearProgressIndicator(
                modifier = Modifier.padding(top = 5.dp).width(24.dp).height(2.dp).clip(RoundedCornerShape(999.dp)),
                color = contentColor,
                trackColor = contentColor.copy(alpha = 0.22f),
                strokeCap = StrokeCap.Round,
            )
        }
    }
}

@Composable
fun CaptionText(post: ViewerPostUiModel) {
    Text(
        text = post.title,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
        color = PeekInk,
        style = TextStyle(fontFamily = Inter, fontSize = 12.sp, lineHeight = 16.sp),
    )
}

@Composable
fun CommentsSection(
    post: ViewerPostUiModel,
    isLoadingMore: Boolean,
    scrollOffset: Int,
    onLoadMore: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.thread), color = PeekInk, style = TextStyle(fontFamily = Geist, fontSize = 16.sp, fontWeight = FontWeight.SemiBold))
            Text(stringResource(R.string.comments_count, post.commentCount), color = PeekMuted, style = TextStyle(fontFamily = GeistMono, fontSize = 8.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.7.sp))
        }
        post.comments.forEachIndexed { index, comment ->
            CommentThread(comment, accentLine = index == 0)
        }
        if (post.canLoadMoreComments) {
            CommentPaginationSentinel(
                isLoading = isLoadingMore,
                scrollOffset = scrollOffset,
                onLoadMore = onLoadMore,
            )
        }
    }
}

@Composable
private fun CommentPaginationSentinel(
    isLoading: Boolean,
    scrollOffset: Int,
    onLoadMore: () -> Unit,
) {
    val rootView = LocalView.current
    var lastRequestedScrollOffset by remember { mutableIntStateOf(-1) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .onGloballyPositioned { coordinates ->
                val visible = coordinates.boundsInWindow().top < rootView.height
                if (scrollOffset > lastRequestedScrollOffset && visible && !isLoading) {
                    lastRequestedScrollOffset = scrollOffset
                    onLoadMore()
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (isLoading) "Loading more comments…" else "Scroll for more comments",
            color = PeekMuted,
            style = TextStyle(fontFamily = GeistMono, fontSize = 8.sp, fontWeight = FontWeight.SemiBold),
        )
    }
}

@Composable
fun CommentThread(comment: CommentUiModel, accentLine: Boolean) {
    val lineColor = if (accentLine) PeekMuted else PeekBorder
    Column(
        modifier = Modifier.fillMaxWidth().drawBehind {
            drawLine(lineColor, start = Offset(0f, 0f), end = Offset(0f, size.height), strokeWidth = 2.dp.toPx())
        }.padding(start = 10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        CommentRow(comment, isReply = false)
        comment.replies.forEach { CommentRow(it, isReply = true) }
    }
}

@Composable
fun CommentRow(comment: CommentUiModel, isReply: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(start = if (isReply) 30.dp else 0.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(comment.author, color = PeekInk, style = TextStyle(fontFamily = Inter, fontSize = 10.sp, fontWeight = FontWeight.SemiBold))
            Text(comment.age, color = PeekMuted, style = TextStyle(fontFamily = GeistMono, fontSize = 8.sp))
        }
        Text(comment.body, modifier = Modifier.fillMaxWidth(), color = PeekInk, style = TextStyle(fontFamily = Inter, fontSize = 13.sp, lineHeight = 18.sp))
    }
}
