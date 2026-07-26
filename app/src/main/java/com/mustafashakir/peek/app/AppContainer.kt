package com.mustafashakir.peek.app

import android.content.Context
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import com.mustafashakir.peek.data.cache.LinkContentCacheDocument
import com.mustafashakir.peek.data.cache.LinkContentCacheSerializer
import com.mustafashakir.peek.data.cache.LinkContentCacheStore
import com.mustafashakir.peek.data.instagram.AndroidInstagramPageLoader
import com.mustafashakir.peek.data.instagram.InstagramDirectPageLoader
import com.mustafashakir.peek.data.instagram.InstagramLinkContentRepository
import com.mustafashakir.peek.data.recent.DataStoreRecentLinksRepository
import com.mustafashakir.peek.data.recent.RecentLinksDocument
import com.mustafashakir.peek.data.recent.RecentLinksSerializer
import com.mustafashakir.peek.domain.model.Clock
import com.mustafashakir.peek.domain.model.SystemClock
import com.mustafashakir.peek.domain.repository.LinkContentRepository
import com.mustafashakir.peek.domain.usecase.ObserveRecentContentUseCase
import com.mustafashakir.peek.domain.usecase.OpenLinkUseCase
import com.mustafashakir.peek.domain.usecase.RefreshLinkUseCase
import com.mustafashakir.peek.domain.usecase.LoadMoreCommentsUseCase
import com.mustafashakir.peek.ui.mapper.HomeUiMapper
import com.mustafashakir.peek.ui.mapper.UiImageMapper
import com.mustafashakir.peek.ui.mapper.ViewerUiMapper
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

interface AppContainer {
    val observeRecentContent: ObserveRecentContentUseCase
    val openLink: OpenLinkUseCase
    val refreshLink: RefreshLinkUseCase
    val loadMoreComments: LoadMoreCommentsUseCase
    val homeUiMapper: HomeUiMapper
    val viewerUiMapper: ViewerUiMapper
}

class DefaultAppContainer(
    context: Context,
    clock: Clock = SystemClock,
) : AppContainer {
    private val seedCacheDocument = LinkContentCacheDocument(entries = emptyList())
    private val linkContentCacheDataStore = DataStoreFactory.create(
        serializer = LinkContentCacheSerializer(seedCacheDocument),
        corruptionHandler = ReplaceFileCorruptionHandler { seedCacheDocument },
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
        produceFile = { File(context.filesDir, "link_content_cache.json") },
    )
    private val linkContentCacheStore = LinkContentCacheStore(linkContentCacheDataStore)
    private val contentRepository: LinkContentRepository = InstagramLinkContentRepository(
        pageLoaders = listOf(
            InstagramDirectPageLoader(),
            AndroidInstagramPageLoader(context),
        ),
        cacheStore = linkContentCacheStore,
    )
    private val seedDocument = RecentLinksDocument(links = emptyList())
    private val recentLinksDataStore = DataStoreFactory.create(
        serializer = RecentLinksSerializer(seedDocument),
        corruptionHandler = ReplaceFileCorruptionHandler { seedDocument },
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
        produceFile = { File(context.filesDir, "recent_links.json") },
    )
    private val recentLinksRepository = DataStoreRecentLinksRepository(recentLinksDataStore, clock)
    private val imageMapper = UiImageMapper()

    override val observeRecentContent = ObserveRecentContentUseCase(
        contentRepository = contentRepository,
        recentLinksRepository = recentLinksRepository,
    )
    override val openLink = OpenLinkUseCase(
        contentRepository = contentRepository,
        recentLinksRepository = recentLinksRepository,
    )
    override val refreshLink = RefreshLinkUseCase(
        contentRepository = contentRepository,
        recentLinksRepository = recentLinksRepository,
    )
    override val loadMoreComments = LoadMoreCommentsUseCase(contentRepository)
    override val homeUiMapper = HomeUiMapper(imageMapper, clock)
    override val viewerUiMapper = ViewerUiMapper(imageMapper)

}
