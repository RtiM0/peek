package com.mustafashakir.peek.data.instagram

import com.mustafashakir.peek.data.cache.CachedLinkContent
import com.mustafashakir.peek.data.cache.LinkContentCacheStore
import com.mustafashakir.peek.data.resolver.PrioritizedUrlResolver
import com.mustafashakir.peek.domain.model.Author
import com.mustafashakir.peek.domain.model.Comment
import com.mustafashakir.peek.domain.model.InstagramMediaItem
import com.mustafashakir.peek.domain.model.InstagramMetadata
import com.mustafashakir.peek.domain.model.InstagramVideoVariant
import com.mustafashakir.peek.domain.model.LinkContent
import com.mustafashakir.peek.domain.model.LinkKind
import com.mustafashakir.peek.domain.model.LinkSource
import com.mustafashakir.peek.domain.model.Media
import com.mustafashakir.peek.domain.model.MediaLocation
import com.mustafashakir.peek.domain.repository.LinkContentRepository
import com.mustafashakir.peek.domain.repository.LoadProgressListener
import java.net.URI
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class InstagramLinkContentRepository(
    private val pageLoaders: List<InstagramPageLoader>,
    private val cacheStore: LinkContentCacheStore,
) : LinkContentRepository {
    private val resolverChain = PrioritizedUrlResolver(pageLoaders)
    private val resolversById = pageLoaders.associateBy { it.resolverId }
    private val successfulCache = ConcurrentHashMap<String, LinkContent>()

    /**
     * Which resolver produced the currently-cached content for a shortcode, so [loadMoreComments]
     * always paginates through the same resolver rather than falling back to another one.
     */
    private val resolverForShortcode = ConcurrentHashMap<String, InstagramPageLoader>()
    private val loadMutex = Mutex()

    override suspend fun resolve(url: String): Result<LinkContent> =
        resolve(url, LoadProgressListener {})

    override suspend fun resolve(url: String, onProgress: LoadProgressListener): Result<LinkContent> {
        val post = canonicalize(url) ?: return Result.failure(
            IllegalArgumentException("Unsupported Instagram post URL: $url"),
        )
        successfulCache[post.shortcode]?.let { return Result.success(it.withUrl(url)) }
        cacheStore.get(post.shortcode)?.let {
            rememberCacheHit(post.shortcode, it)
            return Result.success(it.content.withUrl(url))
        }

        return loadAndCache(url, post, onProgress)
    }

    override suspend fun peekCached(url: String): LinkContent? {
        val post = canonicalize(url) ?: return null
        successfulCache[post.shortcode]?.let { return it.withUrl(url) }
        return cacheStore.get(post.shortcode)
            ?.also { rememberCacheHit(post.shortcode, it) }
            ?.content
            ?.withUrl(url)
    }

    override suspend fun loadMoreComments(url: String): Result<LinkContent> = try {
        val post = canonicalize(url) ?: return Result.failure(
            IllegalArgumentException("Unsupported Instagram post URL: $url"),
        )
        loadMutex.withLock {
            val current = successfulCache[post.shortcode]
                ?: cacheStore.get(post.shortcode)?.also { rememberCacheHit(post.shortcode, it) }?.content
                ?: return@withLock Result.failure(
                    IllegalStateException("Load the Instagram post before loading more comments"),
                )
            val metadata = current.sourceMetadata as? InstagramMetadata
                ?: return@withLock Result.failure(
                    IllegalStateException("This post does not support comment pagination"),
                )
            val cursor = metadata.commentsEndCursor
                ?: return@withLock Result.success(current.withUrl(url))
            val resolver = resolverForShortcode[post.shortcode] ?: pageLoaders.first()
            val page = resolver.loadComments(metadata.postId, cursor)
            val updated = current.copy(
                comments = mergeComments(current.comments, mapComments(page.comments, metadata.authorId)),
                sourceMetadata = metadata.copy(commentsEndCursor = page.endCursor),
            )
            successfulCache[post.shortcode] = updated
            cacheStore.put(post.shortcode, updated, resolver.resolverId)
            Result.success(updated.withUrl(url))
        }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Exception) {
        Result.failure(error)
    }

    override suspend fun refresh(url: String): Result<LinkContent> =
        refresh(url, LoadProgressListener {})

    override suspend fun refresh(url: String, onProgress: LoadProgressListener): Result<LinkContent> {
        val post = canonicalize(url) ?: return Result.failure(
            IllegalArgumentException("Unsupported Instagram post URL: $url"),
        )
        successfulCache.remove(post.shortcode)
        resolverForShortcode.remove(post.shortcode)
        cacheStore.remove(post.shortcode)
        return loadAndCache(url, post, onProgress)
    }

    private suspend fun loadAndCache(
        url: String,
        post: CanonicalPost,
        onProgress: LoadProgressListener,
    ): Result<LinkContent> = try {
        loadMutex.withLock {
            successfulCache[post.shortcode]?.let { return@withLock Result.success(it.withUrl(url)) }
            val resolved = withContext(PageLoadProgressElement(onProgress)) {
                resolverChain.resolveWithSource(post.canonicalUrl)
            }
            val content = mapToLinkContent(url, post.shortcode, resolved.value)
            successfulCache[post.shortcode] = content
            resolverForShortcode[post.shortcode] = resolversById.getValue(resolved.resolverId)
            cacheStore.put(post.shortcode, content, resolved.resolverId)
            Result.success(content)
        }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Exception) {
        Result.failure(error)
    }

    /**
     * Recalls which resolver produced [cached] so a later [loadMoreComments] call stays bound to
     * it. Falls back to the highest-priority resolver only when the persisted id is missing or no
     * longer recognized (e.g. a cache entry written before this field existed) — every such entry
     * predates any resolver but the highest-priority one, so this is a historically correct
     * recovery, not a guess.
     */
    private fun rememberCacheHit(shortcode: String, cached: CachedLinkContent) {
        successfulCache[shortcode] = cached.content
        resolverForShortcode[shortcode] = resolversById[cached.resolverId] ?: pageLoaders.first()
    }

    private fun canonicalize(url: String): CanonicalPost? {
        val uri = runCatching { URI(url) }.getOrNull() ?: return null
        if (!uri.scheme.equals("https", ignoreCase = true)) return null
        if (uri.host?.lowercase() !in INSTAGRAM_HOSTS) return null
        val match = POST_PATH.matchEntire(uri.path.orEmpty().trimEnd('/')) ?: return null
        val route = match.groupValues[1]
        val shortcode = match.groupValues[2]
        return CanonicalPost(shortcode, "https://www.instagram.com/$route/$shortcode/")
    }

    private fun mapToLinkContent(
        requestedUrl: String,
        shortcode: String,
        media: ParsedInstagramMedia,
    ): LinkContent {
        val caption = media.caption?.text?.takeIf(String::isNotBlank)
        val parsedItems = media.carouselMedia.ifEmpty { listOf(media) }
        val mediaItems = parsedItems.mapIndexed { index, item ->
            val variants = item.videoVersions.map {
                InstagramVideoVariant(it.type, it.url, it.width, it.height)
            }
            val imageUrl = item.imageVersions?.candidates
                ?.maxByOrNull { it.width * it.height }
                ?.url
                ?: variants.firstOrNull()?.url
                ?: throw IllegalStateException("Instagram media item ${index + 1} has no source")
            InstagramMediaItem(
                id = item.pk.ifBlank { "$shortcode-$index" },
                imageUrl = imageUrl,
                contentDescription = item.accessibilityCaption
                    ?: caption?.take(200)
                    ?: "Instagram media ${index + 1} by ${media.owner.username}",
                videoVariants = variants,
            )
        }
        val firstItem = mediaItems.firstOrNull()
            ?: throw IllegalStateException("Instagram post has no media")
        val isSingleVideo = mediaItems.size == 1 && firstItem.videoVariants.isNotEmpty()
        return LinkContent(
            url = requestedUrl,
            title = caption ?: "Instagram $shortcode",
            source = LinkSource.Instagram,
            kind = if (isSingleVideo) LinkKind.Video else LinkKind.Post,
            thumbnail = MediaLocation.Remote(firstItem.imageUrl),
            media = Media(
                location = MediaLocation.Remote(firstItem.imageUrl),
                contentDescription = firstItem.contentDescription,
                badge = when {
                    mediaItems.size > 1 -> "CAROUSEL"
                    isSingleVideo -> "REEL"
                    else -> "PHOTO"
                },
            ),
            author = Author(
                name = media.owner.username,
                metadata = buildString {
                    append("INSTAGRAM")
                    if (media.owner.is_verified) append(" · VERIFIED")
                    append(" · ${formatCount(media.likeCount)} LIKES")
                },
            ),
            commentCount = media.commentCount,
            comments = mapComments(media),
            sourceMetadata = InstagramMetadata(
                postId = media.pk,
                shortcode = shortcode,
                code = media.code.ifBlank { shortcode },
                takenAtEpochSeconds = media.takenAt,
                likeCount = media.likeCount,
                commentCount = media.commentCount,
                authorId = media.owner.pk,
                authorUsername = media.owner.username,
                authorFullName = media.owner.full_name,
                authorProfilePictureUrl = media.owner.profile_pic_url,
                authorIsVerified = media.owner.is_verified,
                videoVariants = firstItem.videoVariants,
                mediaItems = mediaItems,
                commentsEndCursor = media.commentsEndCursor,
            ),
        )
    }

    private fun mapComments(media: ParsedInstagramMedia): List<Comment> {
        return mapComments(media.comments, media.owner.pk)
    }

    private fun mapComments(
        comments: List<ParsedInstagramComment>,
        creatorId: String,
    ): List<Comment> {
        val byParent = comments.groupBy(ParsedInstagramComment::parentCommentId)

        fun map(comment: ParsedInstagramComment, visited: Set<String>): Comment {
            val nextVisited = visited + comment.id
            return Comment(
                id = comment.id,
                author = comment.user.username,
                initial = comment.user.username.firstOrNull()?.uppercase() ?: "?",
                age = formatAge(comment.createdAt),
                body = comment.text,
                isCreator = comment.user.pk.isNotBlank() && comment.user.pk == creatorId,
                replies = byParent[comment.id].orEmpty()
                    .filterNot { it.id in nextVisited }
                    .map { map(it, nextVisited) },
            )
        }

        return byParent[null].orEmpty().map { map(it, emptySet()) }
    }

    private fun mergeComments(existing: List<Comment>, next: List<Comment>): List<Comment> {
        val knownIds = existing.flatMap(::flattenComments).mapTo(mutableSetOf()) { it.id }
        return existing + next.filter { comment -> flattenComments(comment).none { it.id in knownIds } }
    }

    private fun flattenComments(comment: Comment): Sequence<Comment> = sequence {
        yield(comment)
        comment.replies.forEach { yieldAll(flattenComments(it)) }
    }

    private fun formatAge(createdAtEpochSeconds: Long): String {
        if (createdAtEpochSeconds <= 0L) return ""
        val ageSeconds = max(0L, System.currentTimeMillis() / 1_000L - createdAtEpochSeconds)
        return when {
            ageSeconds < 60 -> "now"
            ageSeconds < 3_600 -> "${ageSeconds / 60}m"
            ageSeconds < 86_400 -> "${ageSeconds / 3_600}h"
            else -> "${ageSeconds / 86_400}d"
        }
    }

    private fun LinkContent.withUrl(url: String): LinkContent =
        if (this.url == url) this else copy(url = url)

    private fun formatCount(count: Int): String = when {
        count >= 1_000_000 -> "${count / 1_000_000}M"
        count >= 10_000 -> "${count / 1_000}K"
        count >= 1_000 -> String.format(Locale.US, "%.1fK", count / 1_000.0)
        else -> count.toString()
    }

    private data class CanonicalPost(val shortcode: String, val canonicalUrl: String)

    private companion object {
        val POST_PATH = Regex("/(p|reel|reels)/([A-Za-z0-9_-]+)")
        val INSTAGRAM_HOSTS = setOf("instagram.com", "www.instagram.com")
    }
}
