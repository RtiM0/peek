package com.mustafashakir.peek.domain.model

enum class RemoteMediaKind {
    Image,
    Video,
}

data class RemoteMedia(
    val id: String,
    val url: String,
    val kind: RemoteMediaKind,
)

/**
 * A media file prepared by the data layer for a short-lived platform action.
 *
 * The path is intentionally platform-neutral; the UI layer decides how to expose it to another app.
 */
data class PreparedMedia(
    val path: String,
    val mimeType: String,
)
