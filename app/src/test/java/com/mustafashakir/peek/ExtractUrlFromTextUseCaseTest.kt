package com.mustafashakir.peek

import com.mustafashakir.peek.domain.usecase.ExtractUrlFromTextUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExtractUrlFromTextUseCaseTest {
    private val extractUrl = ExtractUrlFromTextUseCase()

    @Test
    fun extractsUrlFromSharedTextAndRemovesSentencePunctuation() {
        assertEquals(
            "https://www.instagram.com/reel/DapVyootsZw/",
            extractUrl("Watch this: https://www.instagram.com/reel/DapVyootsZw/)."),
        )
    }

    @Test
    fun acceptsHttpAndHttpsUrlsWithHosts() {
        assertEquals("http://example.com/path", extractUrl("http://example.com/path"))
        assertEquals("https://example.com", extractUrl("https://example.com"))
    }

    @Test
    fun rejectsMissingInvalidAndNonWebUrls() {
        assertNull(extractUrl(null))
        assertNull(extractUrl("just some text"))
        assertNull(extractUrl("mailto:hello@example.com"))
        assertNull(extractUrl("https://"))
    }
}
