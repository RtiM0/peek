package com.mustafashakir.peek.ui.preview

import com.mustafashakir.peek.R
import com.mustafashakir.peek.ui.model.CommentUiModel
import com.mustafashakir.peek.ui.model.HomeUiState
import com.mustafashakir.peek.ui.model.RecentLinkUiModel
import com.mustafashakir.peek.ui.model.UiImage
import com.mustafashakir.peek.ui.model.ViewerMediaItemUiModel
import com.mustafashakir.peek.ui.model.ViewerPostUiModel
import com.mustafashakir.peek.ui.model.ViewerUiState

object PeekPreviewFixtures {
    val home = HomeUiState.Content(
        recentLinks = listOf(
            RecentLinkUiModel("preview://kyoto", "A quiet morning in Kyoto", "INSTAGRAM · REEL", "2m", UiImage.Resource(R.drawable.thumbnail_kyoto), "Kyoto", isCached = true),
            RecentLinkUiModel("preview://material", "A24 — Material studies", "YOUTUBE · VIDEO", "1h", UiImage.Resource(R.drawable.thumbnail_material), "Material studies", isCached = true),
            RecentLinkUiModel("preview://field-notes", "Field notes, volume 04", "TIKTOK · CLIP", "SUN", UiImage.Resource(R.drawable.thumbnail_field_notes), "Field notes", isCached = false),
        ),
    )

    val post = ViewerUiState.Content(viewerPost(isVideo = false))
    val video = ViewerUiState.Content(viewerPost(isVideo = true))
    val carousel = ViewerUiState.Content(
        viewerPost(isVideo = false).copy(
            mediaBadge = "CAROUSEL",
            mediaItems = listOf(
                ViewerMediaItemUiModel(
                    id = "carousel-one",
                    image = UiImage.Resource(R.drawable.media_coast),
                    contentDescription = "Coast photo",
                    videoUrl = null,
                ),
                ViewerMediaItemUiModel(
                    id = "carousel-two",
                    image = UiImage.Resource(R.drawable.thumbnail_kyoto),
                    contentDescription = "Kyoto photo",
                    videoUrl = null,
                ),
                ViewerMediaItemUiModel(
                    id = "carousel-three",
                    image = UiImage.Resource(R.drawable.thumbnail_material),
                    contentDescription = "Material video",
                    videoUrl = "https://127.0.0.1/preview.mp4",
                ),
            ),
            initialMediaIndex = 1,
        ),
    )
    val loading = ViewerUiState.Loading(0.55f, "Finding the original source")
    val unavailable = ViewerUiState.Unavailable("https://members.example.com/private/article")

    private fun viewerPost(isVideo: Boolean) = ViewerPostUiModel(
        title = if (isVideo) "A24 — Material studies" else "A quiet morning in Kyoto",
        isVideo = isVideo,
        media = UiImage.Resource(R.drawable.media_coast),
        mediaDescription = "Sunlit coastline",
        mediaBadge = if (isVideo) "SHORT FILM" else null,
        duration = if (isVideo) "02:18" else null,
        videoUrl = if (isVideo) "https://example.com/preview.mp4" else null,
        authorName = "Mara Chen",
        authorMetadata = if (isVideo) "AUTHOR  ·  MAY 24" else "AUTHOR  ·  2H",
        sourceUrl = "https://www.instagram.com/p/preview-fixture/",
        commentCount = if (isVideo) 12 else 46,
        comments = listOf(
            CommentUiModel(
                id = "preview-one",
                author = if (isVideo) "lena.ortiz" else "leila",
                initial = "L",
                age = if (isVideo) "22m" else "18m",
                body = if (isVideo) "The pacing feels like taking a breath." else "The morning light makes this feel almost cinematic.",
                isCreator = false,
                replies = listOf(
                    CommentUiModel(
                        id = "preview-reply",
                        author = if (isVideo) "mara · creator" else "jonah",
                        initial = if (isVideo) "M" else "J",
                        age = if (isVideo) "14m" else "12m",
                        body = if (isVideo) "Exactly what I hoped it would hold." else "Exactly — especially the color along the coast.",
                        isCreator = isVideo,
                        replies = emptyList(),
                    ),
                ),
            ),
            CommentUiModel(
                id = "preview-two",
                author = "noah.r",
                initial = "N",
                age = "6m",
                body = if (isVideo) "The final frame is beautiful." else "Adding this place to my list.",
                isCreator = false,
                replies = emptyList(),
            ),
        ),
    )
}
