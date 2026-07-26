package com.mustafashakir.peek

import com.mustafashakir.peek.data.fixture.FixtureCatalog
import com.mustafashakir.peek.data.fixture.FixtureLinkContentRepository
import com.mustafashakir.peek.domain.model.RecentLink
import com.mustafashakir.peek.domain.repository.RecentLinksRepository
import com.mustafashakir.peek.domain.usecase.OpenLinkUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class OpenLinkUseCaseTest {
    @Test
    fun successfulResolutionRecordsTheOpenedUrl() = runTest {
        val recents = RecordingRecentLinksRepository()
        val useCase = OpenLinkUseCase(FixtureLinkContentRepository(), recents)

        useCase(FixtureCatalog.KYOTO_URL).getOrThrow()

        assertEquals(FixtureCatalog.KYOTO_URL, recents.lastOpened)
    }
}

private class RecordingRecentLinksRepository : RecentLinksRepository {
    private val items = MutableStateFlow<List<RecentLink>>(emptyList())
    var lastOpened: String? = null
    override fun observeRecents(): Flow<List<RecentLink>> = items
    override suspend fun markOpened(url: String) { lastOpened = url }
}
