package com.mustafashakir.peek

import com.mustafashakir.peek.data.cache.LinkContentCacheDocument
import com.mustafashakir.peek.data.cache.LinkContentCacheEntry
import com.mustafashakir.peek.data.cache.LinkContentCacheStore
import com.mustafashakir.peek.data.instagram.Caption
import com.mustafashakir.peek.data.instagram.ImageCandidate
import com.mustafashakir.peek.data.instagram.ImageVersions
import com.mustafashakir.peek.data.instagram.InstagramLinkContentRepository
import com.mustafashakir.peek.data.instagram.InstagramPageLoader
import com.mustafashakir.peek.data.instagram.Owner
import com.mustafashakir.peek.data.instagram.PageLoadProgressElement
import com.mustafashakir.peek.data.instagram.ParsedInstagramComment
import com.mustafashakir.peek.data.instagram.ParsedInstagramCommentsPage
import com.mustafashakir.peek.data.instagram.ParsedInstagramMedia
import com.mustafashakir.peek.data.instagram.VideoVersion
import com.mustafashakir.peek.domain.model.InstagramMetadata
import com.mustafashakir.peek.domain.model.LoadProgress
import com.mustafashakir.peek.domain.model.LoadStage
import com.mustafashakir.peek.domain.model.MediaLocation
import com.mustafashakir.peek.domain.repository.LoadProgressListener
import java.io.IOException
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InstagramRepositoryTest {
    private fun newCacheStore() = LinkContentCacheStore(FakeCacheDataStore())

    @Test
    fun canonicalizesSupportedReelAndMapsAllMetadata() = runTest {
        val loader = FakePageLoader(media())
        val repository = InstagramLinkContentRepository(listOf(loader), newCacheStore())
        val requested = "https://instagram.com/reel/DapVyootsZw/?utm_source=copy_link"

        val content = repository.resolve(requested).getOrThrow()

        assertEquals("https://www.instagram.com/reel/DapVyootsZw/", loader.urls.single())
        assertEquals(requested, content.url)
        assertEquals("testuser", content.author.name)
        assertEquals(23, content.commentCount)
        assertEquals(1, content.comments.size)
        assertEquals("Looks great", content.comments.single().body)
        assertEquals(
            "https://example.com/thumb.jpg",
            (content.media.location as MediaLocation.Remote).url,
        )
        val metadata = content.sourceMetadata as InstagramMetadata
        assertEquals("123", metadata.postId)
        assertEquals("DapVyootsZw", metadata.shortcode)
        assertEquals("Test User", metadata.authorFullName)
        assertEquals("https://example.com/profile.jpg", metadata.authorProfilePictureUrl)
        assertTrue(metadata.authorIsVerified)
        assertEquals(1_700_000_000L, metadata.takenAtEpochSeconds)
        assertEquals(2, metadata.videoVariants.size)
        assertEquals(1, metadata.mediaItems.size)
    }

    @Test
    fun acceptsPostReelAndReelsPaths() = runTest {
        listOf("p", "reel", "reels").forEach { route ->
            val loader = FakePageLoader(media())
            val repository = InstagramLinkContentRepository(listOf(loader), newCacheStore())

            assertTrue(repository.resolve("https://www.instagram.com/$route/DapVyootsZw/").isSuccess)
            assertEquals(
                "https://www.instagram.com/$route/DapVyootsZw/",
                loader.urls.single(),
            )
        }
    }

    @Test
    fun mapsPhotoPostWithoutRequiringVideo() = runTest {
        val photo = media().copy(
            videoVersions = emptyList(),
            imageVersions = ImageVersions(
                listOf(ImageCandidate("https://example.com/photo.jpg", 1080, 1080)),
            ),
        )
        val repository = InstagramLinkContentRepository(listOf(FakePageLoader(photo)), newCacheStore())

        val content = repository.resolve("https://www.instagram.com/p/DapVyootsZw/").getOrThrow()

        assertEquals(com.mustafashakir.peek.domain.model.LinkKind.Post, content.kind)
        assertEquals("PHOTO", content.media.badge)
        assertEquals(
            "https://example.com/photo.jpg",
            (content.media.location as MediaLocation.Remote).url,
        )
        val metadata = content.sourceMetadata as InstagramMetadata
        assertTrue(metadata.mediaItems.single().videoVariants.isEmpty())
    }

    @Test
    fun mapsMixedCarouselInOriginalOrder() = runTest {
        val photo = media().copy(
            pk = "photo",
            videoVersions = emptyList(),
            imageVersions = ImageVersions(listOf(ImageCandidate("https://example.com/one.jpg", 1080, 1080))),
            carouselMedia = emptyList(),
            accessibilityCaption = "First photo",
        )
        val video = media().copy(
            pk = "video",
            videoVersions = listOf(VideoVersion(101, "https://example.com/two.mp4", 1080, 1920)),
            imageVersions = ImageVersions(listOf(ImageCandidate("https://example.com/two.jpg", 1080, 1920))),
            carouselMedia = emptyList(),
            accessibilityCaption = "Second video",
        )
        val carousel = media().copy(
            videoVersions = emptyList(),
            carouselMedia = listOf(photo, video),
        )
        val loader = FakePageLoader(carousel)
        val repository = InstagramLinkContentRepository(listOf(loader), newCacheStore())

        val content = repository.resolve(
            "https://www.instagram.com/p/DapVyootsZw/?img_index=2",
        ).getOrThrow()

        assertEquals(com.mustafashakir.peek.domain.model.LinkKind.Post, content.kind)
        assertEquals("CAROUSEL", content.media.badge)
        assertEquals("https://www.instagram.com/p/DapVyootsZw/", loader.urls.single())
        val items = (content.sourceMetadata as InstagramMetadata).mediaItems
        assertEquals(listOf("photo", "video"), items.map { it.id })
        assertTrue(items.first().videoVariants.isEmpty())
        assertEquals("https://example.com/two.mp4", items.last().videoVariants.single().url)
    }

    @Test
    fun rejectsUnsupportedAndInsecureUrlsWithoutLoading() = runTest {
        val loader = FakePageLoader(media())
        val repository = InstagramLinkContentRepository(listOf(loader), newCacheStore())

        assertTrue(repository.resolve("https://www.instagram.com/stories/DapVyootsZw/").isFailure)
        assertTrue(repository.resolve("http://www.instagram.com/reel/DapVyootsZw/").isFailure)
        assertTrue(repository.resolve("https://example.com/reel/DapVyootsZw/").isFailure)
        assertTrue(loader.urls.isEmpty())
    }

    @Test
    fun propagatesLoaderFailure() = runTest {
        val repository = InstagramLinkContentRepository(
            listOf(InstagramPageLoader { throw IOException("blocked") }),
            newCacheStore(),
        )

        val result = repository.resolve("https://www.instagram.com/reel/DapVyootsZw/")

        assertTrue(result.isFailure)
        assertEquals("blocked", result.exceptionOrNull()?.message)
    }

    @Test
    fun cachesSuccessAndCoalescesConcurrentRequests() = runTest {
        val gate = CompletableDeferred<Unit>()
        val loader = FakePageLoader(media(), gate)
        val repository = InstagramLinkContentRepository(listOf(loader), newCacheStore())
        val url = "https://www.instagram.com/reel/DapVyootsZw/"

        val first = async { repository.resolve(url) }
        val second = async { repository.resolve("$url?source=second") }
        gate.complete(Unit)

        assertTrue(first.await().isSuccess)
        assertTrue(second.await().isSuccess)
        assertEquals(1, loader.urls.size)
        assertFalse(loader.urls.single().contains("source="))
    }

    @Test
    fun threadsProgressFromCoroutineContextThroughResolve() = runTest {
        val repository = InstagramLinkContentRepository(listOf(ContextReadingPageLoader(media())), newCacheStore())

        val seen = mutableListOf<LoadProgress>()
        val result = repository.resolve(
            "https://www.instagram.com/reel/DapVyootsZw/",
            LoadProgressListener { seen += it },
        )

        assertTrue(result.isSuccess)
        assertEquals(ContextReadingPageLoader.EMITTED, seen)
    }

    @Test
    fun cachesLoadedCommentPagesAcrossViewerResolutions() = runTest {
        val initial = media().copy(commentsEndCursor = "cursor-one")
        val nextPage = ParsedInstagramCommentsPage(
            comments = listOf(
                ParsedInstagramComment(
                    id = "comment-2",
                    text = "A later comment",
                    createdAt = System.currentTimeMillis() / 1_000L - 60,
                    parentCommentId = null,
                    user = Owner(pk = "999", username = "later-reader"),
                ),
            ),
            endCursor = null,
        )
        val cacheStore = newCacheStore()
        val loader = FakePageLoader(initial, commentPages = mapOf("cursor-one" to nextPage))
        val repository = InstagramLinkContentRepository(listOf(loader), cacheStore)
        val url = "https://www.instagram.com/p/DapVyootsZw/"

        repository.resolve(url).getOrThrow()
        val paged = repository.loadMoreComments(url).getOrThrow()
        val repeated = repository.loadMoreComments(url).getOrThrow()
        val reopenedLoader = FakePageLoader(initial)
        val reopened = InstagramLinkContentRepository(listOf(reopenedLoader), cacheStore)
            .resolve("$url?img_index=2")
            .getOrThrow()

        assertEquals(listOf("cursor-one"), loader.commentCursors)
        assertEquals(listOf("Looks great", "A later comment"), paged.comments.map { it.body })
        assertEquals(paged.comments, repeated.comments)
        assertEquals(paged.comments, reopened.comments)
        assertEquals(null, (reopened.sourceMetadata as InstagramMetadata).commentsEndCursor)
        assertTrue(reopenedLoader.urls.isEmpty())
    }

    @Test
    fun fallsBackToSecondResolverWhenFirstFails() = runTest {
        val primary = FailingPageLoader("primary")
        val secondary = FakePageLoader(media(), resolverId = "secondary")
        val repository = InstagramLinkContentRepository(listOf(primary, secondary), newCacheStore())
        val url = "https://www.instagram.com/reel/DapVyootsZw/"

        val content = repository.resolve(url).getOrThrow()

        assertEquals(1, primary.attempts)
        assertEquals("https://www.instagram.com/reel/DapVyootsZw/", secondary.urls.single())
        assertEquals("testuser", content.author.name)
    }

    @Test
    fun neverInvokesSecondResolverWhenFirstSucceeds() = runTest {
        val primary = FakePageLoader(media(), resolverId = "primary")
        val secondary = FailingPageLoader("secondary")
        val repository = InstagramLinkContentRepository(listOf(primary, secondary), newCacheStore())

        val result = repository.resolve("https://www.instagram.com/reel/DapVyootsZw/")

        assertTrue(result.isSuccess)
        assertEquals(0, secondary.attempts)
    }

    @Test
    fun neverInvokesSecondResolverOnCacheHit() = runTest {
        val primary = FakePageLoader(media(), resolverId = "primary")
        val secondary = FailingPageLoader("secondary")
        val repository = InstagramLinkContentRepository(listOf(primary, secondary), newCacheStore())
        val url = "https://www.instagram.com/reel/DapVyootsZw/"

        repository.resolve(url).getOrThrow()
        repository.resolve(url).getOrThrow()

        assertEquals(1, primary.urls.size)
        assertEquals(0, secondary.attempts)
    }

    @Test
    fun loadMoreCommentsUsesResolverThatProducedTheCachedContentEvenWhenItIsNotFirst() = runTest {
        val nextPage = ParsedInstagramCommentsPage(
            comments = listOf(
                ParsedInstagramComment(
                    id = "comment-2",
                    text = "A later comment",
                    createdAt = System.currentTimeMillis() / 1_000L - 60,
                    parentCommentId = null,
                    user = Owner(pk = "999", username = "later-reader"),
                ),
            ),
            endCursor = null,
        )
        val initial = media().copy(commentsEndCursor = "cursor-one")
        val primary = FailingPageLoader("primary")
        val secondary = FakePageLoader(initial, resolverId = "secondary", commentPages = mapOf("cursor-one" to nextPage))
        val repository = InstagramLinkContentRepository(listOf(primary, secondary), newCacheStore())
        val url = "https://www.instagram.com/p/DapVyootsZw/"

        repository.resolve(url).getOrThrow()
        val paged = repository.loadMoreComments(url).getOrThrow()

        assertEquals(listOf("cursor-one"), secondary.commentCursors)
        assertEquals(0, primary.commentAttempts)
        assertEquals(listOf("Looks great", "A later comment"), paged.comments.map { it.body })
    }

    @Test
    fun loadMoreCommentsStaysBoundToPersistedResolverAcrossProcessRestart() = runTest {
        val nextPage = ParsedInstagramCommentsPage(
            comments = listOf(
                ParsedInstagramComment(
                    id = "comment-2",
                    text = "A later comment",
                    createdAt = System.currentTimeMillis() / 1_000L - 60,
                    parentCommentId = null,
                    user = Owner(pk = "999", username = "later-reader"),
                ),
            ),
            endCursor = null,
        )
        val initial = media().copy(commentsEndCursor = "cursor-one")
        val cacheStore = newCacheStore()
        val url = "https://www.instagram.com/p/DapVyootsZw/"
        val firstProcessSecondary = FakePageLoader(initial, resolverId = "secondary")
        InstagramLinkContentRepository(
            listOf(FailingPageLoader("primary"), firstProcessSecondary),
            cacheStore,
        ).resolve(url).getOrThrow()

        // A fresh repository instance simulating a process restart: resolve() is never called on
        // it, so any in-session resolver-affinity bookkeeping must come from the persisted cache.
        val restartedPrimary = FailingPageLoader("primary")
        val restartedSecondary =
            FakePageLoader(initial, resolverId = "secondary", commentPages = mapOf("cursor-one" to nextPage))
        val restarted = InstagramLinkContentRepository(
            listOf(restartedPrimary, restartedSecondary),
            cacheStore,
        )

        val paged = restarted.loadMoreComments(url).getOrThrow()

        assertEquals(listOf("cursor-one"), restartedSecondary.commentCursors)
        assertEquals(0, restartedPrimary.commentAttempts)
        assertEquals(listOf("Looks great", "A later comment"), paged.comments.map { it.body })
    }

    @Test
    fun loadMoreCommentsDefaultsToPrimaryResolverWhenPersistedAffinityIsUnrecognized() = runTest {
        val initial = media().copy(commentsEndCursor = "cursor-one")
        val nextPage = ParsedInstagramCommentsPage(comments = emptyList(), endCursor = null)
        val shortcode = "DapVyootsZw"
        val url = "https://www.instagram.com/p/$shortcode/"

        // Seed content mapped the normal way, then persist it with resolverId = null, simulating
        // a cache entry written before resolver identity was tracked.
        val seedCacheDataStore = FakeCacheDataStore()
        InstagramLinkContentRepository(listOf(FakePageLoader(initial)), LinkContentCacheStore(seedCacheDataStore))
            .resolve(url)
            .getOrThrow()
        val mappedContent = seedCacheDataStore.snapshot().entries.single().content
        val cacheDataStore = FakeCacheDataStore()
        cacheDataStore.seed(LinkContentCacheEntry(key = shortcode, content = mappedContent, resolverId = null))

        val primary = FakePageLoader(initial, resolverId = "primary", commentPages = mapOf("cursor-one" to nextPage))
        val secondary = FailingPageLoader("secondary")
        val repository = InstagramLinkContentRepository(
            listOf(primary, secondary),
            LinkContentCacheStore(cacheDataStore),
        )

        repository.loadMoreComments(url).getOrThrow()

        assertEquals(listOf("cursor-one"), primary.commentCursors)
        assertEquals(0, secondary.commentAttempts)
    }

    private class FakeCacheDataStore : androidx.datastore.core.DataStore<LinkContentCacheDocument> {
        private val state = kotlinx.coroutines.flow.MutableStateFlow(LinkContentCacheDocument())
        override val data = state
        override suspend fun updateData(
            transform: suspend (LinkContentCacheDocument) -> LinkContentCacheDocument,
        ): LinkContentCacheDocument {
            val updated = transform(state.value)
            state.value = updated
            return updated
        }

        fun seed(entry: LinkContentCacheEntry) {
            state.value = state.value.copy(entries = listOf(entry))
        }

        fun snapshot(): LinkContentCacheDocument = state.value
    }

    private class FakePageLoader(
        private val result: ParsedInstagramMedia,
        private val gate: CompletableDeferred<Unit>? = null,
        private val commentPages: Map<String, ParsedInstagramCommentsPage> = emptyMap(),
        override val resolverId: String = "fake",
    ) : InstagramPageLoader {
        val urls = mutableListOf<String>()
        val commentCursors = mutableListOf<String>()

        override suspend fun resolve(url: String): ParsedInstagramMedia {
            urls += url
            gate?.await()
            return result
        }

        override suspend fun loadComments(postId: String, cursor: String): ParsedInstagramCommentsPage {
            commentCursors += cursor
            return commentPages.getValue(cursor)
        }
    }

    /** A resolver that always fails, for asserting a fallback/affinity-bound resolver is never reached. */
    private class FailingPageLoader(override val resolverId: String) : InstagramPageLoader {
        var attempts = 0
        var commentAttempts = 0

        override suspend fun resolve(url: String): ParsedInstagramMedia {
            attempts += 1
            throw IOException("$resolverId failed")
        }

        override suspend fun loadComments(postId: String, cursor: String): ParsedInstagramCommentsPage {
            commentAttempts += 1
            throw IOException("$resolverId does not support comment pagination")
        }
    }

    private class ContextReadingPageLoader(private val result: ParsedInstagramMedia) : InstagramPageLoader {
        override suspend fun resolve(url: String): ParsedInstagramMedia {
            val listener = coroutineContext[PageLoadProgressElement]?.listener
            EMITTED.forEach { listener?.onProgress(it) }
            return result
        }

        companion object {
            val EMITTED = listOf(
                LoadProgress(0.1f, LoadStage.Connecting),
                LoadProgress(0.6f, LoadStage.FetchingPage),
            )
        }
    }

    private fun media() = ParsedInstagramMedia(
        pk = "123",
        code = "DapVyootsZw",
        videoVersions = listOf(
            VideoVersion(101, "https://example.com/video-hd.mp4", 1080, 1920),
            VideoVersion(102, "https://example.com/video-sd.mp4", 540, 960),
        ),
        imageVersions = ImageVersions(
            listOf(ImageCandidate("https://example.com/thumb.jpg", 640, 1136)),
        ),
        caption = Caption("Hello reel"),
        likeCount = 6_422,
        commentCount = 23,
        takenAt = 1_700_000_000L,
        owner = Owner(
            pk = "456",
            username = "testuser",
            full_name = "Test User",
            profile_pic_url = "https://example.com/profile.jpg",
            is_verified = true,
        ),
        comments = listOf(
            ParsedInstagramComment(
                id = "comment-1",
                text = "Looks great",
                createdAt = System.currentTimeMillis() / 1_000L - 120,
                parentCommentId = null,
                user = Owner(pk = "789", username = "commenter"),
            ),
        ),
    )
}
