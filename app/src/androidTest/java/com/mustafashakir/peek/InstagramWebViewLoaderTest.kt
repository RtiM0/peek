package com.mustafashakir.peek

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mustafashakir.peek.data.instagram.AndroidInstagramPageLoader
import com.mustafashakir.peek.data.instagram.PageLoadProgressElement
import com.mustafashakir.peek.domain.model.LoadProgress
import com.mustafashakir.peek.domain.repository.LoadProgressListener
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InstagramWebViewLoaderTest {
    @Test
    fun hiddenWebViewUsesDesktopUserAgentAndExtractsDynamicJson() = runBlocking {
        val userAgent = AndroidInstagramPageLoader.DESKTOP_USER_AGENT
        assertTrue(userAgent.contains("Macintosh"))
        assertTrue(userAgent.contains("Chrome/"))
        assertFalse(userAgent.contains("Mobile"))
        assertFalse(userAgent.contains("Android"))

        val loader = AndroidInstagramPageLoader(
            context = ApplicationProvider.getApplicationContext(),
            timeoutMillis = 5_000,
            pollIntervalMillis = 50,
        )
        val media = loader.loadHtmlForTesting(
            """
            <!doctype html><html><body><script>
              const desktop = navigator.userAgent.includes('Macintosh') &&
                navigator.userAgent.includes('Chrome/') &&
                !navigator.userAgent.includes('Mobile') &&
                !navigator.userAgent.includes('Android');
              if (desktop) {
                const payload = {require: [{nested: {items: [{
                  pk: 'ua-post', code: 'test', taken_at: 123,
                  video_versions: [{type: 101, url: 'https://example.com/ua-video.mp4'}],
                  caption: {text: 'Desktop UA accepted'},
                  like_count: 4, comment_count: 2,
                  owner: {pk: 'ua-author', username: 'desktop_user', is_verified: false}
                }]}}]};
                const node = document.createElement('script');
                node.type = 'application/json';
                node.textContent = JSON.stringify(payload);
                document.body.appendChild(node);
              }
            </script></body></html>
            """.trimIndent(),
        )

        assertEquals("desktop_user", media.owner.username)
        assertEquals("https://example.com/ua-video.mp4", media.videoVersions.single().url)
    }

    @Test
    fun reportsProgressWhileLoadingAndExtracting() = runBlocking {
        val loader = AndroidInstagramPageLoader(
            context = ApplicationProvider.getApplicationContext(),
            timeoutMillis = 5_000,
            pollIntervalMillis = 50,
        )
        val seen = mutableListOf<LoadProgress>()

        withContext(PageLoadProgressElement(LoadProgressListener { seen += it })) {
            loader.loadHtmlForTesting(
                """
                <!doctype html><html><body><script>
                  const payload = {require: [{nested: {items: [{
                    pk: 'progress-post', code: 'test', taken_at: 123,
                    video_versions: [{type: 101, url: 'https://example.com/progress-video.mp4'}],
                    caption: {text: 'Progress'}, like_count: 1, comment_count: 0,
                    owner: {pk: 'progress-author', username: 'progress_user', is_verified: false}
                  }]}}]};
                  const node = document.createElement('script');
                  node.type = 'application/json';
                  node.textContent = JSON.stringify(payload);
                  document.body.appendChild(node);
                </script></body></html>
                """.trimIndent(),
            )
        }

        assertTrue(seen.isNotEmpty())
        assertEquals(1f, seen.last().fraction)
        assertTrue(seen.all { it.fraction in 0f..1f })
    }
}
