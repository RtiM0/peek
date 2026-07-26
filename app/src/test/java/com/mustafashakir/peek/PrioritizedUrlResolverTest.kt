package com.mustafashakir.peek

import com.mustafashakir.peek.data.resolver.PrioritizedUrlResolver
import com.mustafashakir.peek.data.resolver.UrlResolver
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PrioritizedUrlResolverTest {
    @Test
    fun firstResolverWinsWithoutInvokingLaterOnes() = runTest {
        val second = RecordingResolver("second") { "second-result" }
        val chain = PrioritizedUrlResolver(listOf(RecordingResolver("first") { "first-result" }, second))

        val result = chain.resolve("https://example.com/a")

        assertEquals("first-result", result)
        assertTrue(second.urls.isEmpty())
    }

    @Test
    fun fallsThroughToNextResolverOnFailure() = runTest {
        val chain = PrioritizedUrlResolver(
            listOf(
                RecordingResolver("first") { throw IOException("first failed") },
                RecordingResolver("second") { "second-result" },
            ),
        )

        val result = chain.resolveWithSource("https://example.com/a")

        assertEquals("second-result", result.value)
        assertEquals("second", result.resolverId)
    }

    @Test
    fun exhaustsAllResolversAndSurfacesLastFailureWithEarlierOnesSuppressed() = runTest {
        val chain = PrioritizedUrlResolver(
            listOf(
                RecordingResolver("first") { throw IOException("first failed") },
                RecordingResolver("second") { throw IOException("second failed") },
            ),
        )

        val result = runCatching { chain.resolve("https://example.com/a") }

        val error = result.exceptionOrNull()
        assertEquals("second failed", error?.message)
        assertEquals(1, error?.suppressed?.size)
        assertEquals("first failed", error?.suppressed?.single()?.message)
    }

    @Test
    fun rethrowsCancellationImmediatelyWithoutFallback() = runTest {
        val second = RecordingResolver("second") { "second-result" }
        val chain = PrioritizedUrlResolver(
            listOf(RecordingResolver("first") { throw CancellationException("cancelled") }, second),
        )

        var caught: CancellationException? = null
        try {
            chain.resolve("https://example.com/a")
        } catch (cancellation: CancellationException) {
            caught = cancellation
        }

        assertEquals("cancelled", caught?.message)
        assertTrue(second.urls.isEmpty())
    }

    @Test
    fun skipsResolversThatDoNotSupportTheUrl() = runTest {
        val unsupported = RecordingResolver("unsupported", supports = { false }) { "unsupported-result" }
        val supported = RecordingResolver("supported") { "supported-result" }
        val chain = PrioritizedUrlResolver(listOf(unsupported, supported))

        val result = chain.resolveWithSource("https://example.com/a")

        assertEquals("supported-result", result.value)
        assertTrue(unsupported.urls.isEmpty())
    }

    @Test
    fun requiresAtLeastOneResolver() {
        assertThrows(IllegalArgumentException::class.java) {
            PrioritizedUrlResolver<String>(emptyList())
        }
    }

    @Test
    fun failsWhenNoResolverSupportsTheUrl() = runTest {
        val chain = PrioritizedUrlResolver(listOf(RecordingResolver("first", supports = { false }) { "unused" }))

        val result = runCatching { chain.resolve("https://example.com/a") }

        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    private class RecordingResolver(
        override val resolverId: String,
        private val supports: (String) -> Boolean = { true },
        private val onResolve: suspend (String) -> String,
    ) : UrlResolver<String> {
        val urls = mutableListOf<String>()

        override fun supports(url: String): Boolean = supports.invoke(url)

        override suspend fun resolve(url: String): String {
            urls += url
            return onResolve(url)
        }
    }
}
