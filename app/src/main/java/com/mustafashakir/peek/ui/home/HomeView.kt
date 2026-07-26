package com.mustafashakir.peek.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mustafashakir.peek.R
import com.mustafashakir.peek.ui.components.PeekImage
import com.mustafashakir.peek.ui.components.PeekLockup
import com.mustafashakir.peek.ui.model.HomeUiState
import com.mustafashakir.peek.ui.model.RecentLinkUiModel
import com.mustafashakir.peek.ui.theme.Geist
import com.mustafashakir.peek.ui.theme.GeistMono
import com.mustafashakir.peek.ui.theme.Inter
import com.mustafashakir.peek.ui.theme.PeekBorder
import com.mustafashakir.peek.ui.theme.PeekAccent
import com.mustafashakir.peek.ui.theme.PeekGround
import com.mustafashakir.peek.ui.theme.PeekInk
import com.mustafashakir.peek.ui.theme.PeekMuted
import com.mustafashakir.peek.ui.theme.PeekSecondary
import com.mustafashakir.peek.ui.theme.PeekTile

@Composable
fun HomeView(
    uiState: HomeUiState,
    onPasteClick: () -> Unit,
    onRecentLink: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize().background(PeekGround), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = 390.dp)
                .fillMaxWidth()
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 8.dp),
        ) {
            HomeHeader()
            Spacer(Modifier.height(20.dp))
            ClipboardEntry(onPasteClick)
            Spacer(Modifier.height(20.dp))
            when (uiState) {
                HomeUiState.Loading -> LoadingRecents()
                HomeUiState.Empty -> EmptyRecents()
                is HomeUiState.Content -> RecentLinks(uiState.recentLinks, onRecentLink)
            }
        }
    }
}

@Composable
private fun HomeHeader() {
    val settingsDescription = stringResource(R.string.settings)
    Row(
        modifier = Modifier.fillMaxWidth().height(34.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        PeekLockup()
        Box(
            modifier = Modifier.requiredSize(48.dp).semantics { contentDescription = settingsDescription },
            contentAlignment = Alignment.CenterEnd,
        ) {
            Icon(Icons.Rounded.Tune, contentDescription = stringResource(R.string.settings), tint = PeekSecondary, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun ClipboardEntry(onPasteClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().height(222.dp)) {
        Text(
            text = stringResource(R.string.home_statement),
            modifier = Modifier.width(210.dp).offset(y = 6.dp),
            color = PeekInk,
            style = TextStyle(fontFamily = Geist, fontSize = 42.sp, fontWeight = FontWeight.Bold, letterSpacing = (-2).sp, lineHeight = 38.6.sp),
        )
        Box(
            modifier = Modifier.align(Alignment.TopEnd).offset(x = (-8).dp, y = 16.dp).size(width = 100.dp, height = 96.dp).rotate(-5f).clip(RoundedCornerShape(22.dp)).background(PeekBorder),
        )
        Box(
            modifier = Modifier.align(Alignment.TopEnd).offset(y = 4.dp).size(width = 100.dp, height = 96.dp).clip(RoundedCornerShape(22.dp)).background(PeekTile),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Visibility, contentDescription = null, tint = PeekInk, modifier = Modifier.size(38.dp))
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(66.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(PeekInk)
                .clickable(role = Role.Button, onClick = onPasteClick)
                .padding(start = 16.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.ContentPaste, contentDescription = null, tint = Color.White, modifier = Modifier.size(19.dp))
            }
            Text(
                text = stringResource(R.string.paste_from_clipboard),
                modifier = Modifier.weight(1f),
                color = Color.White,
                style = TextStyle(fontFamily = Inter, fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
            )
            Box(Modifier.size(38.dp).clip(RoundedCornerShape(50)).background(PeekTile), contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = PeekInk, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun RecentLinks(links: List<RecentLinkUiModel>, onRecentLink: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth().height(26.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.recent_links), color = PeekInk, style = TextStyle(fontFamily = Geist, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp))
            Text(stringResource(R.string.recent_count, links.size), color = PeekMuted, style = TextStyle(fontFamily = GeistMono, fontSize = 8.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.sp))
        }
        links.forEach { link -> RecentLinkRow(link, onRecentLink) }
    }
}

@Composable
private fun RecentLinkRow(link: RecentLinkUiModel, onRecentLink: (String) -> Unit) {
    val openLinkDescription = stringResource(R.string.open_link, link.title)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable(role = Role.Button) { onRecentLink(link.url) }
            .semantics { contentDescription = openLinkDescription },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (link.thumbnail != null) {
            PeekImage(
                image = link.thumbnail,
                contentDescription = link.thumbnailDescription,
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(PeekBorder),
            )
        } else {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(PeekBorder))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(link.title, maxLines = 1, color = PeekInk, style = TextStyle(fontFamily = Inter, fontSize = 13.sp, fontWeight = FontWeight.SemiBold))
            if (link.isCached) {
                Text(link.sourceLabel, color = PeekMuted, style = TextStyle(fontFamily = GeistMono, fontSize = 8.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.8.sp))
            } else {
                Text(stringResource(R.string.tap_to_load), color = PeekMuted, style = TextStyle(fontFamily = GeistMono, fontSize = 8.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.8.sp))
            }
        }
        Text(link.ageLabel, color = PeekSecondary, style = TextStyle(fontFamily = GeistMono, fontSize = 8.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp))
    }
}

@Composable
private fun LoadingRecents() {
    Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = PeekAccent, modifier = Modifier.size(28.dp))
    }
}

@Composable
private fun EmptyRecents() {
    Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
        Text(stringResource(R.string.no_recent_links), color = PeekMuted, fontFamily = Inter, fontSize = 13.sp)
    }
}
