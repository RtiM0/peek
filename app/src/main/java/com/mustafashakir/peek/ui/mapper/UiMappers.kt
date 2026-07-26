package com.mustafashakir.peek.ui.mapper

import com.mustafashakir.peek.R
import com.mustafashakir.peek.domain.model.BundledImageKey
import com.mustafashakir.peek.domain.model.Clock
import com.mustafashakir.peek.domain.model.Comment
import com.mustafashakir.peek.domain.model.InstagramMetadata
import com.mustafashakir.peek.domain.model.LinkContent
import com.mustafashakir.peek.domain.model.LinkKind
import com.mustafashakir.peek.domain.model.LinkSource
import com.mustafashakir.peek.domain.model.LoadStage
import com.mustafashakir.peek.domain.model.MediaLocation
import com.mustafashakir.peek.domain.model.RecentContent
import com.mustafashakir.peek.domain.model.SourceMetadata
import com.mustafashakir.peek.ui.model.CommentUiModel
import com.mustafashakir.peek.ui.model.HomeUiState
import com.mustafashakir.peek.ui.model.RecentLinkUiModel
import com.mustafashakir.peek.ui.model.UiImage
import com.mustafashakir.peek.ui.model.ViewerMediaItemUiModel
import com.mustafashakir.peek.ui.model.ViewerPostUiModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.TextStyle
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Locale
import kotlin.math.max

fun loadStageMessage(stage: LoadStage): String = when (stage) {
    LoadStage.Connecting -> "Connecting to the source"
    LoadStage.FetchingPage -> "Fetching the page"
    LoadStage.ExtractingContent -> "Finding the original source"
}

class UiImageMapper {
    fun map(location: MediaLocation): UiImage = when (location) {
        is MediaLocation.Remote -> UiImage.Url(location.url)
        is MediaLocation.Bundled -> UiImage.Resource(
            when (location.key) {
                BundledImageKey.Kyoto -> R.drawable.thumbnail_kyoto
                BundledImageKey.Material -> R.drawable.thumbnail_material
                BundledImageKey.FieldNotes -> R.drawable.thumbnail_field_notes
                BundledImageKey.Coast -> R.drawable.media_coast
            },
        )
    }
}

class HomeUiMapper(
    private val imageMapper: UiImageMapper,
    private val clock: Clock,
    private val zoneId: ZoneId = ZoneId.systemDefault(),
) {
    fun map(recents: List<RecentContent>): HomeUiState {
        if (recents.isEmpty()) return HomeUiState.Empty
        return HomeUiState.Content(
            recentLinks = recents.map { recent ->
                val content = recent.content
                if (content == null) {
                    RecentLinkUiModel(
                        url = recent.recentLink.url,
                        title = recent.recentLink.url,
                        sourceLabel = "",
                        ageLabel = ageLabel(recent.recentLink.openedAtEpochMillis),
                        thumbnail = null,
                        thumbnailDescription = recent.recentLink.url,
                        isCached = false,
                    )
                } else {
                    RecentLinkUiModel(
                        url = content.url,
                        title = content.title,
                        sourceLabel = sourceLabel(content),
                        ageLabel = ageLabel(recent.recentLink.openedAtEpochMillis),
                        thumbnail = imageMapper.map(content.thumbnail),
                        thumbnailDescription = content.title,
                        isCached = true,
                    )
                }
            },
        )
    }

    private fun sourceLabel(content: LinkContent): String = when (content.source) {
        LinkSource.Instagram -> if (content.kind == LinkKind.Video) "INSTAGRAM · REEL" else "INSTAGRAM · POST"
        LinkSource.YouTube -> "YOUTUBE · VIDEO"
        LinkSource.TikTok -> "TIKTOK · CLIP"
    }

    private fun ageLabel(openedAtEpochMillis: Long): String {
        val ageMillis = max(0L, clock.nowEpochMillis() - openedAtEpochMillis)
        val minutes = ageMillis / 60_000
        return when {
            minutes < 60 -> "${max(1L, minutes)}m"
            minutes < 1_440 -> "${minutes / 60}h"
            else -> Instant.ofEpochMilli(openedAtEpochMillis)
                .atZone(zoneId)
                .dayOfWeek
                .getDisplayName(TextStyle.SHORT, Locale.US)
                .uppercase(Locale.US)
        }
    }
}

class ViewerUiMapper(private val imageMapper: UiImageMapper) {
    fun map(content: LinkContent): ViewerPostUiModel {
        val sourceMetadata = content.sourceMetadata
        val mediaItems = if (sourceMetadata is InstagramMetadata && sourceMetadata.mediaItems.isNotEmpty()) {
            sourceMetadata.mediaItems.map { item ->
                ViewerMediaItemUiModel(
                    id = item.id,
                    image = imageMapper.map(MediaLocation.Remote(item.imageUrl)),
                    contentDescription = item.contentDescription,
                    videoUrl = bestVideoUrl(item.videoVariants),
                )
            }
        } else {
            listOf(
                ViewerMediaItemUiModel(
                    id = content.url,
                    image = imageMapper.map(content.media.location),
                    contentDescription = content.media.contentDescription,
                    videoUrl = bestVideoUrl(sourceMetadata),
                ),
            )
        }
        return ViewerPostUiModel(
            title = content.title,
            isVideo = content.kind == LinkKind.Video,
            media = mediaItems.first().image,
            mediaDescription = mediaItems.first().contentDescription,
            mediaBadge = content.media.badge,
            duration = content.media.duration,
            videoUrl = mediaItems.first().videoUrl,
            authorName = content.author.name,
            authorMetadata = content.author.metadata,
            commentCount = content.commentCount,
            comments = content.comments.map(::mapComment),
            canLoadMoreComments = (sourceMetadata as? InstagramMetadata)?.commentsEndCursor != null,
            mediaItems = mediaItems,
            initialMediaIndex = requestedMediaIndex(content.url, mediaItems.size),
        )
    }

    private fun requestedMediaIndex(url: String, itemCount: Int): Int {
        if (itemCount <= 1) return 0
        val rawQuery = runCatching { URI(url).rawQuery }.getOrNull() ?: return 0
        val oneBasedIndex = rawQuery
            .split("&")
            .firstOrNull { it.substringBefore("=") == "img_index" }
            ?.substringAfter("=", "")
            ?.let { URLDecoder.decode(it, StandardCharsets.UTF_8.name()) }
            ?.toIntOrNull()
            ?: return 0
        return (oneBasedIndex - 1).coerceIn(0, itemCount - 1)
    }

    private fun bestVideoUrl(sourceMetadata: SourceMetadata?): String? =
        when (sourceMetadata) {
            is InstagramMetadata -> bestVideoUrl(sourceMetadata.videoVariants)
            null -> null
        }

    private fun bestVideoUrl(
        variants: List<com.mustafashakir.peek.domain.model.InstagramVideoVariant>,
    ): String? = variants.maxByOrNull { (it.width ?: 0) * (it.height ?: 0) }?.url

    private fun mapComment(comment: Comment): CommentUiModel = CommentUiModel(
        id = comment.id,
        author = comment.author,
        initial = comment.initial,
        age = comment.age,
        body = comment.body,
        isCreator = comment.isCreator,
        replies = comment.replies.map(::mapComment),
    )
}
