package com.mustafashakir.peek

import com.mustafashakir.peek.data.fixture.FixtureCatalog
import com.mustafashakir.peek.data.fixture.FixtureLinkContentRepository
import com.mustafashakir.peek.domain.model.Clock
import com.mustafashakir.peek.domain.model.LinkContent
import com.mustafashakir.peek.domain.model.LoadProgress
import com.mustafashakir.peek.domain.model.LoadStage
import com.mustafashakir.peek.domain.model.RecentLink
import com.mustafashakir.peek.domain.repository.LinkContentRepository
import com.mustafashakir.peek.domain.repository.LoadProgressListener
import com.mustafashakir.peek.domain.repository.RecentLinksRepository
import com.mustafashakir.peek.domain.usecase.ObserveRecentContentUseCase
import com.mustafashakir.peek.domain.usecase.OpenLinkUseCase
import com.mustafashakir.peek.domain.usecase.RefreshLinkUseCase
import com.mustafashakir.peek.domain.usecase.LoadMoreCommentsUseCase
import com.mustafashakir.peek.ui.home.HomeViewModel
import com.mustafashakir.peek.ui.mapper.HomeUiMapper
import com.mustafashakir.peek.ui.mapper.UiImageMapper
import com.mustafashakir.peek.ui.mapper.ViewerUiMapper
import com.mustafashakir.peek.ui.model.HomeUiState
import com.mustafashakir.peek.ui.model.ViewerUiState
import com.mustafashakir.peek.ui.viewer.ViewerViewModel
import java.time.ZoneOffset
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun homeViewModelPublishesMappedRepositoryState() = runTest(mainDispatcherRule.dispatcher) {
        val recents = FakeRecentLinksRepository(
            listOf(RecentLink(FixtureCatalog.KYOTO_URL, 1_000L)),
        )
        val viewModel = HomeViewModel(
            observeRecentContent = ObserveRecentContentUseCase(FixtureLinkContentRepository(), recents),
            mapper = HomeUiMapper(UiImageMapper(), Clock { 61_000L }, ZoneOffset.UTC),
        )
        val collection = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }

        advanceUntilIdle()

        val content = viewModel.uiState.value as HomeUiState.Content
        assertEquals("A quiet morning in Kyoto", content.recentLinks.single().title)
        collection.cancel()
    }

    @Test
    fun viewerViewModelMapsResolvedContentAndMarksItOpened() = runTest(mainDispatcherRule.dispatcher) {
        val recents = FakeRecentLinksRepository(emptyList())
        val viewModel = ViewerViewModel(
            url = FixtureCatalog.MATERIAL_URL,
            openLink = OpenLinkUseCase(FixtureLinkContentRepository(), recents),
            refreshLink = RefreshLinkUseCase(FixtureLinkContentRepository(), recents),
            loadMoreComments = LoadMoreCommentsUseCase(FixtureLinkContentRepository()),
            mapper = ViewerUiMapper(UiImageMapper()),
        )

        advanceUntilIdle()

        val content = viewModel.uiState.value as ViewerUiState.Content
        assertEquals("A24 — Material studies", content.post.title)
        assertEquals(listOf(FixtureCatalog.MATERIAL_URL), recents.openedUrls)
    }

    @Test
    fun viewerViewModelPublishesIntermediateProgressBeforeContent() = runTest(mainDispatcherRule.dispatcher) {
        val recents = FakeRecentLinksRepository(emptyList())
        val content = FixtureCatalog.contents.getValue(FixtureCatalog.MATERIAL_URL)
        val repository = ProgressReportingLinkContentRepository(
            content,
            listOf(
                LoadProgress(0.2f, LoadStage.Connecting),
                LoadProgress(0.7f, LoadStage.FetchingPage),
            ),
        )
        val viewModel = ViewerViewModel(
            url = FixtureCatalog.MATERIAL_URL,
            openLink = OpenLinkUseCase(repository, recents),
            refreshLink = RefreshLinkUseCase(repository, recents),
            loadMoreComments = LoadMoreCommentsUseCase(repository),
            mapper = ViewerUiMapper(UiImageMapper()),
        )
        val seen = mutableListOf<ViewerUiState>()
        val collection = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { seen += it }
        }

        advanceUntilIdle()

        val loadingStates = seen.filterIsInstance<ViewerUiState.Loading>()
        assertEquals(0.2f, loadingStates[1].progress)
        assertEquals(0.7f, loadingStates[2].progress)
        assertEquals(true, seen.last() is ViewerUiState.Content)
        collection.cancel()
    }

    @Test
    fun viewerViewModelPublishesUnavailableForUnknownUrl() = runTest(mainDispatcherRule.dispatcher) {
        val fakeRecents = FakeRecentLinksRepository(emptyList())
        val viewModel = ViewerViewModel(
            url = "unknown",
            openLink = OpenLinkUseCase(FixtureLinkContentRepository(), fakeRecents),
            refreshLink = RefreshLinkUseCase(FixtureLinkContentRepository(), fakeRecents),
            loadMoreComments = LoadMoreCommentsUseCase(FixtureLinkContentRepository()),
            mapper = ViewerUiMapper(UiImageMapper()),
        )

        advanceUntilIdle()

        assertEquals(ViewerUiState.Unavailable("unknown"), viewModel.uiState.value)
    }
}

private class FakeRecentLinksRepository(initial: List<RecentLink>) : RecentLinksRepository {
    private val state = MutableStateFlow(initial)
    val openedUrls = mutableListOf<String>()

    override fun observeRecents(): Flow<List<RecentLink>> = state

    override suspend fun markOpened(url: String) {
        openedUrls += url
    }
}

private class ProgressReportingLinkContentRepository(
    private val content: LinkContent,
    private val progress: List<LoadProgress>,
) : LinkContentRepository {
    override suspend fun resolve(url: String): Result<LinkContent> = Result.success(content)

    override suspend fun resolve(url: String, onProgress: LoadProgressListener): Result<LinkContent> {
        progress.forEach(onProgress::onProgress)
        return Result.success(content)
    }

    override suspend fun peekCached(url: String): LinkContent? = content

    override suspend fun refresh(url: String): Result<LinkContent> = resolve(url)

    override suspend fun refresh(url: String, onProgress: LoadProgressListener): Result<LinkContent> =
        resolve(url, onProgress)
}
