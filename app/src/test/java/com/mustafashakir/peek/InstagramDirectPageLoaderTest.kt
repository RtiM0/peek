package com.mustafashakir.peek

import com.mustafashakir.peek.data.instagram.InstagramDirectPageLoader
import com.mustafashakir.peek.data.instagram.PageLoadProgressElement
import com.mustafashakir.peek.domain.model.LoadProgress
import com.mustafashakir.peek.domain.repository.LoadProgressListener
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InstagramDirectPageLoaderTest {
    @Test
    fun postsAnonymousMediaQueryAndParsesInitialCommentPage() = runTest {
        val connection = FakeHttpConnection(GRAPHQL_RESPONSE)
        val loader = InstagramDirectPageLoader(connectionFactory = { connection })
        val progress = mutableListOf<LoadProgress>()

        val media = kotlinx.coroutines.withContext(
            PageLoadProgressElement(LoadProgressListener { progress += it }),
        ) {
            loader.resolve("https://www.instagram.com/p/BAAAAFc6p0s/")
        }

        val form = decodeForm(connection.requestBody.toString(StandardCharsets.UTF_8.name()))
        assertEquals("27130156389949648", form.getValue("doc_id"))
        assertEquals("""{"media_id":"1152921510460693804"}""", form.getValue("variables"))
        assertEquals("direct-photo", media.pk)
        assertEquals("First page", media.comments.single().text)
        assertEquals(1f, progress.last().fraction)
        assertFalse(connection.instanceRedirectsEnabled)
        assertEquals("936619743392459", connection.getRequestProperty("X-IG-App-ID"))
        assertEquals("same-origin", connection.getRequestProperty("Sec-Fetch-Site"))
    }

    @Test
    fun rejectsUnsupportedPathBeforeOpeningConnection() = runTest {
        var connectionOpened = false
        val loader = InstagramDirectPageLoader(connectionFactory = {
            connectionOpened = true
            FakeHttpConnection(GRAPHQL_RESPONSE)
        })

        val result = runCatching {
            loader.resolve("https://www.instagram.com/stories/example/")
        }

        assertTrue(result.isFailure)
        assertFalse(connectionOpened)
    }

    @Test
    fun postsCursorToAnonymousCommentsQueryAndParsesNextPage() = runTest {
        val connection = FakeHttpConnection(COMMENTS_PAGE_RESPONSE)
        val loader = InstagramDirectPageLoader(connectionFactory = { connection })

        val page = loader.loadComments("direct-photo", "cursor-one")

        val form = decodeForm(connection.requestBody.toString(StandardCharsets.UTF_8.name()))
        assertEquals("27261273046856309", form.getValue("doc_id"))
        assertEquals(
            """{"after":"cursor-one","first":10,"media_id":"direct-photo"}""",
            form.getValue("variables"),
        )
        assertEquals("Second page", page.comments.single().text)
        assertEquals("cursor-two", page.endCursor)
    }

    private fun decodeForm(body: String): Map<String, String> =
        body.split("&").associate { entry ->
            val (key, value) = entry.split("=", limit = 2)
            URLDecoder.decode(key, StandardCharsets.UTF_8.name()) to
                URLDecoder.decode(value, StandardCharsets.UTF_8.name())
        }

    private class FakeHttpConnection(private val response: String) :
        HttpURLConnection(URL("https://www.instagram.com/api/graphql")) {
        val requestBody = ByteArrayOutputStream()
        var instanceRedirectsEnabled: Boolean = true

        override fun setInstanceFollowRedirects(followRedirects: Boolean) {
            super.setInstanceFollowRedirects(followRedirects)
            this.instanceRedirectsEnabled = followRedirects
        }

        override fun getOutputStream() = requestBody
        override fun getResponseCode(): Int = HTTP_OK
        override fun getInputStream() =
            ByteArrayInputStream(response.toByteArray(StandardCharsets.UTF_8))

        override fun connect() = Unit
        override fun disconnect() = Unit
        override fun usingProxy(): Boolean = false
    }

    private companion object {
        val GRAPHQL_RESPONSE = """
            {
              "data": {
                "xig_polaris_media": {
                  "if_not_gated_logged_out": {
                    "pk": "direct-photo",
                    "code": "BAAAAFc6p0s",
                    "image_versions2": {
                      "candidates": [{"url": "https://example.com/photo.jpg", "width": 1080, "height": 1080}]
                    },
                    "user": {"pk": "creator", "username": "creator"}
                  },
                  "comments_connection": {
                    "edges": [{"node": {
                      "id": "comment",
                      "text": "First page",
                      "created_at": 1,
                      "user": {"pk": "reader", "username": "reader"}
                    }}]
                  }
                }
              }
            }
        """.trimIndent()

        val COMMENTS_PAGE_RESPONSE = """
            {
              "data": {
                "xig_polaris_media": {
                  "comments_connection": {
                    "edges": [{"node": {
                      "id": "comment-two",
                      "text": "Second page",
                      "created_at": 2,
                      "user": {"pk": "reader-two", "username": "reader-two"}
                    }}],
                    "page_info": {"has_next_page": true, "end_cursor": "cursor-two"}
                  }
                }
              }
            }
        """.trimIndent()
    }
}
