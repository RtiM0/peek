package com.mustafashakir.peek

import com.mustafashakir.peek.data.fixture.FixtureCatalog
import com.mustafashakir.peek.data.fixture.FixtureLinkContentRepository
import com.mustafashakir.peek.domain.model.LinkKind
import com.mustafashakir.peek.domain.repository.LoadProgressListener
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FixtureRepositoryTest {
    private val repository = FixtureLinkContentRepository()

    @Test
    fun resolvesEverySeededLink() = runTest {
        val resolved = FixtureCatalog.urls.map { repository.resolve(it).getOrThrow() }

        assertEquals(3, resolved.size)
        assertEquals(LinkKind.Post, resolved.first().kind)
        assertEquals(LinkKind.Video, resolved[1].kind)
    }

    @Test
    fun rejectsUnknownLinks() = runTest {
        assertTrue(repository.resolve("https://invalid.example/post").isFailure)
    }

    @Test
    fun defaultProgressOverloadNeverInvokesListener() = runTest {
        var invoked = false
        val result = repository.resolve(FixtureCatalog.urls.first(), LoadProgressListener { invoked = true })

        assertTrue(result.isSuccess)
        assertEquals(false, invoked)
    }
}
