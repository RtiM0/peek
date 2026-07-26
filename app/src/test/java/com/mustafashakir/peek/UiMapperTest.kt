package com.mustafashakir.peek

import com.mustafashakir.peek.data.fixture.FixtureCatalog
import com.mustafashakir.peek.domain.model.Clock
import com.mustafashakir.peek.domain.model.InstagramMediaItem
import com.mustafashakir.peek.domain.model.InstagramMetadata
import com.mustafashakir.peek.domain.model.RecentContent
import com.mustafashakir.peek.domain.model.RecentLink
import com.mustafashakir.peek.ui.mapper.HomeUiMapper
import com.mustafashakir.peek.ui.mapper.UiImageMapper
import com.mustafashakir.peek.ui.mapper.ViewerUiMapper
import com.mustafashakir.peek.ui.model.HomeUiState
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UiMapperTest {
    private val now = 1_800_000L

    @Test
    fun homeMapperProducesCompleteRenderingModels() {
        val content = FixtureCatalog.contents.getValue(FixtureCatalog.KYOTO_URL)
        val mapper = HomeUiMapper(UiImageMapper(), Clock { now }, ZoneId.of("UTC"))

        val result = mapper.map(listOf(RecentContent(RecentLink(content.url, now - 120_000), content)))

        assertTrue(result is HomeUiState.Content)
        result as HomeUiState.Content
        assertEquals("A quiet morning in Kyoto", result.recentLinks.single().title)
        assertEquals("2m", result.recentLinks.single().ageLabel)
        assertEquals("INSTAGRAM · POST", result.recentLinks.single().sourceLabel)
    }

    @Test
    fun viewerMapperRetainsThreadStructure() {
        val content = FixtureCatalog.contents.getValue(FixtureCatalog.MATERIAL_URL)

        val result = ViewerUiMapper(UiImageMapper()).map(content)

        assertTrue(result.isVideo)
        assertEquals(2, result.comments.size)
        assertEquals(1, result.comments.first().replies.size)
    }

    @Test
    fun viewerMapperHonorsRequestedCarouselIndex() {
        val base = FixtureCatalog.contents.getValue(FixtureCatalog.KYOTO_URL)
        val content = base.copy(
            url = "https://www.instagram.com/p/example/?img_index=2",
            sourceMetadata = InstagramMetadata(
                postId = "post",
                shortcode = "example",
                code = "example",
                takenAtEpochSeconds = 0,
                likeCount = 0,
                commentCount = 0,
                authorId = "author",
                authorUsername = "author",
                authorFullName = null,
                authorProfilePictureUrl = null,
                authorIsVerified = false,
                videoVariants = emptyList(),
                mediaItems = listOf(
                    InstagramMediaItem("one", "https://example.com/one.jpg", "One"),
                    InstagramMediaItem("two", "https://example.com/two.jpg", "Two"),
                ),
            ),
        )

        val result = ViewerUiMapper(UiImageMapper()).map(content)

        assertEquals(2, result.mediaItems.size)
        assertEquals(1, result.initialMediaIndex)
    }
}
