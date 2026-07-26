package com.mustafashakir.peek.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
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
import com.mustafashakir.peek.ui.theme.PeekBorder
import com.mustafashakir.peek.ui.theme.PeekInk
import com.mustafashakir.peek.ui.theme.PeekMuted
import com.mustafashakir.peek.ui.theme.PeekSecondary
import com.mustafashakir.peek.ui.theme.PeekTile
import androidx.compose.ui.res.stringResource

@Composable
fun AuthorByline(post: ViewerPostUiModel) {
    Row(
        modifier = Modifier.fillMaxWidth().height(54.dp).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.size(34.dp).clip(CircleShape).background(PeekTile), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Person, contentDescription = null, tint = PeekSecondary, modifier = Modifier.size(17.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(post.authorName, color = PeekInk, style = TextStyle(fontFamily = Inter, fontSize = 12.sp, fontWeight = FontWeight.SemiBold))
            Text(post.authorMetadata, color = PeekMuted, style = TextStyle(fontFamily = GeistMono, fontSize = 8.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.7.sp))
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
