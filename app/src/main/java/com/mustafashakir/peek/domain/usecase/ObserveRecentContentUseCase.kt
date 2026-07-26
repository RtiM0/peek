package com.mustafashakir.peek.domain.usecase

import com.mustafashakir.peek.domain.model.RecentContent
import com.mustafashakir.peek.domain.repository.LinkContentRepository
import com.mustafashakir.peek.domain.repository.RecentLinksRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObserveRecentContentUseCase(
    private val contentRepository: LinkContentRepository,
    private val recentLinksRepository: RecentLinksRepository,
) {
    operator fun invoke(): Flow<List<RecentContent>> =
        recentLinksRepository.observeRecents().map { links ->
            links.map { recent -> RecentContent(recent, contentRepository.peekCached(recent.url)) }
        }
}
