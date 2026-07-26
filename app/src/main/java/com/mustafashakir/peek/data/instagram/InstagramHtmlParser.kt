package com.mustafashakir.peek.data.instagram

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class VideoVersion(
    val type: Int = 0,
    val url: String,
    val width: Int? = null,
    val height: Int? = null,
)

@Serializable
data class ImageCandidate(val url: String, val width: Int = 0, val height: Int = 0)

@Serializable
data class ImageVersions(val candidates: List<ImageCandidate>)

@Serializable
data class Caption(val text: String)

@Serializable
data class Owner(
    val pk: String = "",
    val username: String = "unknown",
    val full_name: String? = null,
    val profile_pic_url: String? = null,
    val is_verified: Boolean = false,
)

data class ParsedInstagramComment(
    val id: String,
    val text: String,
    val createdAt: Long,
    val parentCommentId: String?,
    val user: Owner,
)

data class ParsedInstagramCommentsPage(
    val comments: List<ParsedInstagramComment>,
    val endCursor: String?,
)

data class ParsedInstagramMedia(
    val pk: String,
    val code: String,
    val videoVersions: List<VideoVersion>,
    val imageVersions: ImageVersions?,
    val caption: Caption?,
    val likeCount: Int,
    val commentCount: Int,
    val takenAt: Long,
    val owner: Owner,
    val comments: List<ParsedInstagramComment>,
    val commentsEndCursor: String? = null,
    val carouselMedia: List<ParsedInstagramMedia> = emptyList(),
    val accessibilityCaption: String? = null,
)

class InstagramHtmlParser {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun parse(html: String): ParsedInstagramMedia? {
        val media = extractFromJsonDocument(html)
            ?: extractFromEmbeddedJson(html)
            ?: extractFromHtmlKeys(html)
            ?: return null
        if (media.comments.isNotEmpty() || media.commentsEndCursor != null) return media
        val commentsPage = extractCommentsPageFromJsonDocument(html)
            ?: extractCommentsPageFromHtml(html)
        return commentsPage?.takeIf { it.comments.isNotEmpty() || it.endCursor != null }
            ?.let { media.copy(comments = it.comments, commentsEndCursor = it.endCursor) }
            ?: media
    }

    fun parseCommentsPage(content: String): ParsedInstagramCommentsPage? {
        val root = runCatching { json.parseToJsonElement(content) }.getOrNull() ?: return null
        return findFirstValue(root, "comments_connection")?.let(::parseCommentsPage)
    }

    private fun extractFromJsonDocument(content: String): ParsedInstagramMedia? {
        val root = runCatching { json.parseToJsonElement(content) }.getOrNull() ?: return null
        return bestMedia(root)
    }

    private fun extractCommentsPageFromJsonDocument(content: String): ParsedInstagramCommentsPage? {
        val root = runCatching { json.parseToJsonElement(content) }.getOrNull() ?: return null
        return findFirstValue(root, "comments_connection")?.let(::parseCommentsPage)
    }

    private fun extractCommentsPageFromHtml(html: String): ParsedInstagramCommentsPage? =
        (extractJsonValue(html, "comments_connection") ?: extractJsonValue(html, "comments"))
            ?.let { runCatching { json.parseToJsonElement(it) }.getOrNull() }
            ?.let(::parseCommentsPage)

    private fun extractFromEmbeddedJson(html: String): ParsedInstagramMedia? {
        var best: ParsedInstagramMedia? = null
        SCRIPT_JSON.findAll(html).forEach { match ->
            val root = runCatching { json.parseToJsonElement(match.groupValues[1]) }.getOrNull()
                ?: return@forEach
            val media = bestMedia(root) ?: return@forEach
            val currentBest = best
            if (currentBest == null || media.richnessScore() > currentBest.richnessScore()) best = media
        }
        return best
    }

    private fun bestMedia(root: JsonElement): ParsedInstagramMedia? =
        findMediaObjects(root)
            .mapNotNull(::parseMediaObject)
            .maxByOrNull { it.richnessScore() }

    private fun findMediaObjects(element: JsonElement): List<JsonObject> {
        val queue = ArrayDeque<JsonElement>()
        val matches = mutableListOf<JsonObject>()
        queue.add(element)
        var visited = 0
        while (queue.isNotEmpty() && visited++ < MAX_JSON_NODES) {
            when (val current = queue.removeFirst()) {
                is JsonObject -> {
                    val videos = current["video_versions"] as? JsonArray
                    val carousel = current["carousel_media"] as? JsonArray
                    val images = ((current["image_versions2"] ?: current["image_versions"]) as? JsonObject)
                        ?.get("candidates") as? JsonArray
                    val hasIdentity = current.containsKey("pk") ||
                        current.containsKey("id") ||
                        current.containsKey("media_id") ||
                        current.containsKey("code")
                    val hasMedia = !videos.isNullOrEmpty() ||
                        !carousel.isNullOrEmpty() ||
                        !images.isNullOrEmpty() ||
                        current.string("display_uri") != null
                    if (hasIdentity && hasMedia) matches += current
                    current.values.forEach(queue::addLast)
                }
                is JsonArray -> current.forEach(queue::addLast)
                else -> Unit
            }
        }
        return matches
    }

    private fun ParsedInstagramMedia.richnessScore(): Int =
        carouselMedia.size * 10_000 +
            comments.size * 1_000 +
            (if (imageVersions?.candidates?.isNotEmpty() == true) 100 else 0) +
            (if (caption != null) 10 else 0) +
            (if (owner.username != "unknown") 1 else 0)

    private fun parseMediaObject(obj: JsonObject): ParsedInstagramMedia? {
        val videos = obj["video_versions"]?.let(::parseVideoVersions).orEmpty()
        val carousel = (obj["carousel_media"] as? JsonArray).orEmpty()
            .mapNotNull { (it as? JsonObject)?.let(::parseMediaObject) }
        val imageVersions = (obj["image_versions2"] ?: obj["image_versions"])
            ?.let(::parseImageVersions)
            ?: obj.string("display_uri")?.let { ImageVersions(listOf(ImageCandidate(it))) }
        if (videos.isEmpty() && carousel.isEmpty() && imageVersions == null) return null
        val ownerElement = obj["owner"] ?: obj["user"]
        return ParsedInstagramMedia(
            pk = obj.string("media_id") ?: obj.string("pk") ?: obj.string("id").orEmpty(),
            code = obj.string("code").orEmpty(),
            videoVersions = videos,
            imageVersions = imageVersions,
            caption = obj["caption"]?.let(::parseCaption),
            likeCount = obj.int("like_count"),
            commentCount = obj.int("comment_count"),
            takenAt = obj.long("taken_at"),
            owner = ownerElement?.let(::parseOwner) ?: Owner(),
            comments = (obj["comments_connection"] ?: obj["comments"])
                ?.let(::parseComments)
                .orEmpty(),
            commentsEndCursor = (obj["comments_connection"] ?: obj["comments"])
                ?.let(::parseCommentsPage)
                ?.endCursor,
            carouselMedia = carousel,
            accessibilityCaption = obj.string("accessibility_caption"),
        )
    }

    private fun extractFromHtmlKeys(html: String): ParsedInstagramMedia? {
        val videos = extractJsonValue(html, "video_versions")
            ?.let { runCatching { json.parseToJsonElement(it) }.getOrNull() }
            ?.let(::parseVideoVersions)
            .orEmpty()

        val imageVersions = (extractJsonValue(html, "image_versions2")
            ?: extractJsonValue(html, "image_versions"))
            ?.let { runCatching { json.parseToJsonElement(it) }.getOrNull() }
            ?.let(::parseImageVersions)
            ?: extractString(html, "display_uri")?.let {
                ImageVersions(listOf(ImageCandidate(it)))
            }
        val carouselMedia = extractJsonValue(html, "carousel_media")
            ?.let { runCatching { json.parseToJsonElement(it) }.getOrNull() as? JsonArray }
            .orEmpty()
            .mapNotNull { (it as? JsonObject)?.let(::parseMediaObject) }
        if (videos.isEmpty() && imageVersions == null && carouselMedia.isEmpty()) return null
        val caption = extractJsonValue(html, "caption")
            ?.let { runCatching { json.parseToJsonElement(it) }.getOrNull() }
            ?.let(::parseCaption)
        val owner = (extractJsonValue(html, "owner") ?: extractJsonValue(html, "user"))
            ?.let { runCatching { json.parseToJsonElement(it) }.getOrNull() }
            ?.let(::parseOwner)
            ?: Owner(username = extractString(html, "username") ?: "unknown")
        val commentsElement = (extractJsonValue(html, "comments_connection")
            ?: extractJsonValue(html, "comments"))
            ?.let { runCatching { json.parseToJsonElement(it) }.getOrNull() }
        val commentsPage = commentsElement?.let(::parseCommentsPage)

        return ParsedInstagramMedia(
            pk = extractString(html, "media_id") ?: extractString(html, "pk").orEmpty(),
            code = extractString(html, "code").orEmpty(),
            videoVersions = videos,
            imageVersions = imageVersions,
            caption = caption,
            likeCount = extractNumber(html, "like_count")?.toIntOrNull() ?: 0,
            commentCount = extractNumber(html, "comment_count")?.toIntOrNull() ?: 0,
            takenAt = extractNumber(html, "taken_at")?.toLongOrNull() ?: 0L,
            owner = owner,
            comments = commentsPage?.comments.orEmpty(),
            commentsEndCursor = commentsPage?.endCursor,
            carouselMedia = carouselMedia,
            accessibilityCaption = extractString(html, "accessibility_caption"),
        )
    }

    private fun findFirstValue(root: JsonElement, key: String): JsonElement? {
        val queue = ArrayDeque<JsonElement>()
        queue.add(root)
        var visited = 0
        while (queue.isNotEmpty() && visited++ < MAX_JSON_NODES) {
            when (val current = queue.removeFirst()) {
                is JsonObject -> {
                    current[key]?.let { return it }
                    current.values.forEach(queue::addLast)
                }
                is JsonArray -> current.forEach(queue::addLast)
                else -> Unit
            }
        }
        return null
    }

    private fun parseComments(element: JsonElement): List<ParsedInstagramComment> {
        val items = when (element) {
            is JsonArray -> element
            is JsonObject -> {
                val edges = element["edges"] as? JsonArray
                val nodes = element["nodes"] as? JsonArray
                edges ?: nodes ?: JsonArray(emptyList())
            }
            else -> JsonArray(emptyList())
        }

        return items.flatMap { item ->
            val wrapper = item as? JsonObject ?: return@flatMap emptyList()
            val node = (wrapper["node"] as? JsonObject) ?: wrapper
            val comment = parseCommentNode(node) ?: return@flatMap emptyList()
            val nested = listOfNotNull(
                node["preview_child_comments"],
                node["child_comments"],
                node["edge_threaded_comments"],
            ).flatMap(::parseComments)
            listOf(comment) + nested
        }
    }

    private fun parseCommentsPage(element: JsonElement): ParsedInstagramCommentsPage {
        val connection = element as? JsonObject
        val pageInfo = connection?.get("page_info") as? JsonObject
        val endCursor = pageInfo?.string("end_cursor")
            ?.takeIf { pageInfo.boolean("has_next_page") }
        return ParsedInstagramCommentsPage(parseComments(element), endCursor)
    }

    private fun parseCommentNode(node: JsonObject): ParsedInstagramComment? {
        val text = node.string("text")?.takeIf(String::isNotBlank) ?: return null
        val user = node["user"]?.let(::parseOwner)
            ?: node["owner"]?.let(::parseOwner)
            ?: return null
        return ParsedInstagramComment(
            id = node.string("id") ?: node.string("pk") ?: return null,
            text = text,
            createdAt = node.long("created_at"),
            parentCommentId = node.string("parent_comment_id"),
            user = user,
        )
    }

    private fun parseVideoVersions(element: JsonElement): List<VideoVersion> =
        (element as? JsonArray).orEmpty().mapNotNull { item ->
            val obj = item as? JsonObject ?: return@mapNotNull null
            val url = obj.string("url") ?: return@mapNotNull null
            VideoVersion(
                type = obj.int("type"),
                url = url,
                width = obj.intOrNull("width"),
                height = obj.intOrNull("height"),
            )
        }

    private fun parseImageVersions(element: JsonElement): ImageVersions? {
        val candidates = (element as? JsonObject)?.get("candidates") as? JsonArray ?: return null
        val parsed = candidates.mapNotNull { item ->
            val obj = item as? JsonObject ?: return@mapNotNull null
            ImageCandidate(
                url = obj.string("url") ?: return@mapNotNull null,
                width = obj.int("width"),
                height = obj.int("height"),
            )
        }
        return parsed.takeIf { it.isNotEmpty() }?.let(::ImageVersions)
    }

    private fun parseCaption(element: JsonElement): Caption? {
        if (element.toString() == "null") return null
        val text = (element as? JsonObject)?.string("text") ?: return null
        return Caption(text)
    }

    private fun parseOwner(element: JsonElement): Owner? {
        val obj = element as? JsonObject ?: return null
        return Owner(
            pk = obj.string("pk").orEmpty(),
            username = obj.string("username") ?: "unknown",
            full_name = obj.string("full_name"),
            profile_pic_url = obj.string("profile_pic_url"),
            is_verified = obj.boolean("is_verified"),
        )
    }

    private fun extractJsonValue(html: String, key: String): String? {
        var searchFrom = 0
        val needle = "\"$key\""
        while (true) {
            val keyIndex = html.indexOf(needle, searchFrom)
            if (keyIndex < 0) return null
            var index = keyIndex + needle.length
            while (index < html.length && html[index].isWhitespace()) index++
            if (index >= html.length || html[index] != ':') {
                searchFrom = index
                continue
            }
            index++
            while (index < html.length && html[index].isWhitespace()) index++
            if (index >= html.length) return null
            return when (html[index]) {
                '{', '[' -> balancedJson(html, index)
                '"' -> quotedJson(html, index)
                else -> html.substring(index, html.indexOfAny(charArrayOf(',', '}'), index).let {
                    if (it < 0) html.length else it
                }).trim()
            }
        }
    }

    private fun balancedJson(text: String, start: Int): String? {
        val open = text[start]
        val close = if (open == '{') '}' else ']'
        var depth = 0
        var inString = false
        var escaped = false
        for (index in start until text.length) {
            val char = text[index]
            if (inString) {
                when {
                    escaped -> escaped = false
                    char == '\\' -> escaped = true
                    char == '"' -> inString = false
                }
            } else {
                when (char) {
                    '"' -> inString = true
                    open -> depth++
                    close -> if (--depth == 0) return text.substring(start, index + 1)
                }
            }
        }
        return null
    }

    private fun quotedJson(text: String, start: Int): String? {
        var escaped = false
        for (index in start + 1 until text.length) {
            when {
                escaped -> escaped = false
                text[index] == '\\' -> escaped = true
                text[index] == '"' -> return text.substring(start, index + 1)
            }
        }
        return null
    }

    private fun extractString(html: String, key: String): String? =
        extractJsonValue(html, key)?.let { value ->
            runCatching { json.parseToJsonElement(value).jsonPrimitive.contentOrNull }.getOrNull()
        }

    private fun extractNumber(html: String, key: String): String? =
        extractJsonValue(html, key)?.takeWhile { it.isDigit() || it == '-' }

    private fun JsonObject.string(key: String): String? =
        get(key)?.let { runCatching { it.jsonPrimitive.contentOrNull }.getOrNull() }

    private fun JsonObject.int(key: String): Int = intOrNull(key) ?: 0
    private fun JsonObject.intOrNull(key: String): Int? = string(key)?.toIntOrNull()
    private fun JsonObject.long(key: String): Long = string(key)?.toLongOrNull() ?: 0L
    private fun JsonObject.boolean(key: String): Boolean = string(key)?.toBooleanStrictOrNull() ?: false

    private companion object {
        const val MAX_JSON_NODES = 50_000
        val SCRIPT_JSON = Regex(
            """<script[^>]*type\s*=\s*[\"']application/json[\"'][^>]*>([\s\S]*?)</script>""",
            RegexOption.IGNORE_CASE,
        )
    }
}
