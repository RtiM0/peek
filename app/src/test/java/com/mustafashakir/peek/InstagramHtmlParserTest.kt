package com.mustafashakir.peek

import com.mustafashakir.peek.data.instagram.InstagramHtmlParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InstagramHtmlParserTest {
    private val parser = InstagramHtmlParser()

    @Test
    fun recursivelyFindsMediaInEmbeddedJsonAndDecodesText() {
        val html = """
            <html><script type="application/json">
            {"require":[["RelayPrefetchedStreamCache",null,null,{"__bbox":{"result":{"data":{"items":[{
              "pk":"123","code":"DapVyootsZw","taken_at":1700000000,
              "video_versions":[{"type":101,"url":"https://example.com/video.mp4","width":1080,"height":1920}],
              "image_versions2":{"candidates":[{"url":"https://example.com/thumb.jpg","width":640,"height":1136}]},
              "caption":{"text":"Hello\nreel ❤"},"like_count":6422,"comment_count":23,
              "owner":{"pk":"456","username":"testuser","full_name":"Test User","profile_pic_url":"https://example.com/profile.jpg","is_verified":true},
              "comments_connection":{"edges":[{"node":{"id":"comment-1","text":"First comment","created_at":1699999900,"parent_comment_id":null,"user":{"pk":"789","username":"commenter"}}}]}
            }]}}}}]]}
            </script></html>
        """.trimIndent()

        val result = parser.parse(html)

        assertNotNull(result)
        assertEquals("https://example.com/video.mp4", result!!.videoVersions.single().url)
        assertEquals("Hello\nreel ❤", result.caption?.text)
        assertEquals("testuser", result.owner.username)
        assertEquals("Test User", result.owner.full_name)
        assertEquals(1_700_000_000L, result.takenAt)
        assertEquals("First comment", result.comments.single().text)
        assertEquals("commenter", result.comments.single().user.username)
    }

    @Test
    fun fallsBackToSpecificHtmlKeysWithEscapedQuotes() {
        val html = """
            <html><body>
            {"video_versions":[{"type":101,"url":"https://example.com/video.mp4"}],
             "caption":{"text":"She said \"hello\"\nand left"},
             "like_count":10,"comment_count":2,"taken_at":99,
             "media_id":"789","code":"fallbackCode",
             "user":{"pk":"7","username":"fallback_user","is_verified":false}}
            </body></html>
        """.trimIndent()

        val result = parser.parse(html)

        assertEquals("She said \"hello\"\nand left", result?.caption?.text)
        assertEquals("fallback_user", result?.owner?.username)
        assertEquals("789", result?.pk)
        assertEquals(10, result?.likeCount)
        assertTrue(result?.comments.isNullOrEmpty())
    }

    @Test
    fun toleratesMissingOptionalFields() {
        val result = parser.parse(
            """{"video_versions":[{"type":101,"url":"https://example.com/video.mp4"}]}""",
        )

        assertNotNull(result)
        assertEquals("unknown", result?.owner?.username)
        assertTrue(result?.imageVersions == null)
        assertEquals(0, result?.commentCount)
    }

    @Test
    fun mergesCommentsConnectionFromSeparatePayload() {
        val html = """
            <script type="application/json">
              {"media":{"pk":"1","video_versions":[{"type":101,"url":"https://example.com/video.mp4"}],"owner":{"pk":"2","username":"creator"}}}
            </script>
            <script type="application/json">
              {"response":{"comments_connection":{"edges":[{"node":{"id":"separate-comment","text":"Loaded separately","created_at":123,"parent_comment_id":null,"user":{"pk":"3","username":"reader"}}}]}}}
            </script>
        """.trimIndent()

        val result = parser.parse(html)

        assertEquals("Loaded separately", result?.comments?.single()?.text)
        assertEquals("reader", result?.comments?.single()?.user?.username)
    }

    @Test
    fun parsesRawGraphQlPhotoWithInitialComments() {
        val response = """
            {
              "data": {
                "xig_polaris_media": {
                  "if_not_gated_logged_out": {
                    "pk": "photo-post",
                    "code": "PhotoCode",
                    "taken_at": 1700000000,
                    "image_versions2": {
                      "candidates": [
                        {"url": "https://example.com/photo.jpg", "width": 1080, "height": 1350}
                      ]
                    },
                    "accessibility_caption": "A cat sitting by a sunny window",
                    "caption": {"text": "Window light"},
                    "like_count": 10,
                    "comment_count": 1,
                    "user": {"pk": "creator", "username": "photo_user"}
                  },
                  "comments_connection": {
                    "edges": [
                      {"node": {
                        "id": "first-page-comment",
                        "text": "Lovely",
                        "created_at": 1699999990,
                        "user": {"pk": "reader", "username": "reader_user"}
                      }}
                    ],
                    "page_info": {"has_next_page": true, "end_cursor": "ignored-for-now"}
                  }
                }
              }
            }
        """.trimIndent()

        val result = parser.parse(response)

        assertNotNull(result)
        assertTrue(result!!.videoVersions.isEmpty())
        assertEquals("https://example.com/photo.jpg", result.imageVersions?.candidates?.single()?.url)
        assertEquals("A cat sitting by a sunny window", result.accessibilityCaption)
        assertEquals("Lovely", result.comments.single().text)
        assertEquals("ignored-for-now", result.commentsEndCursor)
    }

    @Test
    fun parsesRawGraphQlMixedCarouselInOrder() {
        val response = """
            {
              "data": {
                "xig_polaris_media": {
                  "if_not_gated_logged_out": {
                    "pk": "carousel-post",
                    "code": "CarouselCode",
                    "user": {"pk": "creator", "username": "carousel_user"},
                    "carousel_media": [
                      {
                        "pk": "photo-slide",
                        "accessibility_caption": "Photo slide",
                        "image_versions2": {
                          "candidates": [{"url": "https://example.com/photo.jpg", "width": 1080, "height": 1080}]
                        }
                      },
                      {
                        "pk": "video-slide",
                        "accessibility_caption": "Video slide",
                        "video_versions": [
                          {"type": 101, "url": "https://example.com/video.mp4", "width": 1080, "height": 1920}
                        ],
                        "image_versions2": {
                          "candidates": [{"url": "https://example.com/poster.jpg", "width": 1080, "height": 1920}]
                        }
                      }
                    ]
                  },
                  "comments_connection": {"edges": []}
                }
              }
            }
        """.trimIndent()

        val result = parser.parse(response)

        assertNotNull(result)
        assertEquals(listOf("photo-slide", "video-slide"), result!!.carouselMedia.map { it.pk })
        assertTrue(result.carouselMedia.first().videoVersions.isEmpty())
        assertEquals(
            "https://example.com/video.mp4",
            result.carouselMedia.last().videoVersions.single().url,
        )
    }

    @Test
    fun returnsNullForMalformedOrNonMediaHtml() {
        assertNull(parser.parse("<html>no data here</html>"))
        assertNull(parser.parse("""{"video_versions":not-json}"""))
        assertNull(parser.parse("""{"image_versions2":{"candidates":[]}}"""))
    }
}
