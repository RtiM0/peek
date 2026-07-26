package com.mustafashakir.peek.data.fixture

import com.mustafashakir.peek.domain.model.Author
import com.mustafashakir.peek.domain.model.BundledImageKey
import com.mustafashakir.peek.domain.model.Comment
import com.mustafashakir.peek.domain.model.LinkContent
import com.mustafashakir.peek.domain.model.LinkKind
import com.mustafashakir.peek.domain.model.LinkSource
import com.mustafashakir.peek.domain.model.Media
import com.mustafashakir.peek.domain.model.MediaLocation

object FixtureCatalog {
    const val KYOTO_URL = "https://www.instagram.com/reel/peek-kyoto/"
    const val MATERIAL_URL = "https://www.youtube.com/watch?v=peek-material"
    const val FIELD_NOTES_URL = "https://www.tiktok.com/@peek/video/field-notes-04"

    val urls: List<String> = listOf(KYOTO_URL, MATERIAL_URL, FIELD_NOTES_URL)

    val contents: Map<String, LinkContent> = listOf(
        LinkContent(
            url = KYOTO_URL,
            title = "A quiet morning in Kyoto",
            source = LinkSource.Instagram,
            kind = LinkKind.Post,
            thumbnail = MediaLocation.Bundled(BundledImageKey.Kyoto),
            media = Media(
                location = MediaLocation.Bundled(BundledImageKey.Coast),
                contentDescription = "Sunlit trees beside a quiet rocky coast",
            ),
            author = Author("Mara Chen", "AUTHOR  ·  2H"),
            commentCount = 46,
            comments = listOf(
                Comment(
                    id = "leila-post",
                    author = "leila",
                    initial = "L",
                    age = "18m",
                    body = "The morning light makes this feel almost cinematic.",
                    replies = listOf(
                        Comment(
                            id = "jonah-post",
                            author = "jonah",
                            initial = "J",
                            age = "12m",
                            body = "Exactly — especially the color along the coast.",
                        ),
                    ),
                ),
                Comment(
                    id = "noah-post",
                    author = "noah.r",
                    initial = "N",
                    age = "6m",
                    body = "Adding this place to my list.",
                ),
            ),
        ),
        LinkContent(
            url = MATERIAL_URL,
            title = "A24 — Material studies",
            source = LinkSource.YouTube,
            kind = LinkKind.Video,
            thumbnail = MediaLocation.Bundled(BundledImageKey.Material),
            media = Media(
                location = MediaLocation.Bundled(BundledImageKey.Coast),
                contentDescription = "Short film preview of a sunlit coastline",
                badge = "SHORT FILM",
                duration = "02:18",
            ),
            author = Author("Mara Chen", "AUTHOR  ·  MAY 24"),
            commentCount = 12,
            comments = videoComments(),
        ),
        LinkContent(
            url = FIELD_NOTES_URL,
            title = "Field notes, volume 04",
            source = LinkSource.TikTok,
            kind = LinkKind.Video,
            thumbnail = MediaLocation.Bundled(BundledImageKey.FieldNotes),
            media = Media(
                location = MediaLocation.Bundled(BundledImageKey.Coast),
                contentDescription = "Field notes video preview beside the coast",
                badge = "SHORT FILM",
                duration = "02:18",
            ),
            author = Author("Mara Chen", "AUTHOR  ·  MAY 24"),
            commentCount = 12,
            comments = videoComments(),
        ),
    ).associateBy(LinkContent::url)

    private fun videoComments(): List<Comment> = listOf(
        Comment(
            id = "lena-video",
            author = "lena.ortiz",
            initial = "L",
            age = "22m",
            body = "The pacing feels like taking a breath.",
            replies = listOf(
                Comment(
                    id = "mara-video",
                    author = "mara · creator",
                    initial = "M",
                    age = "14m",
                    body = "Exactly what I hoped it would hold.",
                    isCreator = true,
                ),
            ),
        ),
        Comment(
            id = "noah-video",
            author = "noah.r",
            initial = "N",
            age = "6m",
            body = "The final frame is beautiful.",
        ),
    )
}
