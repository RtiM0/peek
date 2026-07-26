package com.mustafashakir.peek.ui.model

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable

@Immutable
sealed interface UiImage {
    @Immutable
    data class Resource(@param:DrawableRes val id: Int) : UiImage

    @Immutable
    data class Url(val value: String) : UiImage
}

@Immutable
sealed interface HomeUiState {
    data object Loading : HomeUiState

    @Immutable
    data class Content(
        val recentLinks: List<RecentLinkUiModel>,
    ) : HomeUiState

    data object Empty : HomeUiState
}

@Immutable
data class RecentLinkUiModel(
    val url: String,
    val title: String,
    val sourceLabel: String,
    val ageLabel: String,
    val thumbnail: UiImage?,
    val thumbnailDescription: String,
    val isCached: Boolean,
)

@Immutable
sealed interface ViewerUiState {
    @Immutable
    data class Loading(val progress: Float = 0f, val message: String = "") : ViewerUiState

    data class Unavailable(val url: String) : ViewerUiState

    @Immutable
    data class Content(
        val post: ViewerPostUiModel,
        val isLoadingMoreComments: Boolean = false,
    ) : ViewerUiState
}

@Immutable
data class ViewerPostUiModel(
    val title: String,
    val isVideo: Boolean,
    val media: UiImage,
    val mediaDescription: String,
    val mediaBadge: String?,
    val duration: String?,
    val videoUrl: String?,
    val authorName: String,
    val authorMetadata: String,
    val commentCount: Int,
    val comments: List<CommentUiModel>,
    val canLoadMoreComments: Boolean = false,
    val mediaItems: List<ViewerMediaItemUiModel> = emptyList(),
    val initialMediaIndex: Int = 0,
)

@Immutable
data class ViewerMediaItemUiModel(
    val id: String,
    val image: UiImage,
    val contentDescription: String,
    val videoUrl: String?,
)

@Immutable
data class CommentUiModel(
    val id: String,
    val author: String,
    val initial: String,
    val age: String,
    val body: String,
    val isCreator: Boolean,
    val replies: List<CommentUiModel>,
)
