package com.mustafashakir.peek.domain.usecase

import com.mustafashakir.peek.domain.model.LinkContent
import com.mustafashakir.peek.domain.repository.LinkContentRepository
import com.mustafashakir.peek.domain.repository.LoadProgressListener
import com.mustafashakir.peek.domain.repository.RecentLinksRepository

class RefreshLinkUseCase(
    private val contentRepository: LinkContentRepository,
    private val recentLinksRepository: RecentLinksRepository,
) {
    suspend operator fun invoke(
        url: String,
        onProgress: LoadProgressListener = LoadProgressListener {},
    ): Result<LinkContent> =
        contentRepository.refresh(url, onProgress).onSuccess { recentLinksRepository.markOpened(url) }
}
