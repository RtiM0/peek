package com.mustafashakir.peek.domain.usecase

import com.mustafashakir.peek.domain.model.LinkContent
import com.mustafashakir.peek.domain.repository.LinkContentRepository

/** Retrieves exactly one next page from a post's existing comments connection. */
class LoadMoreCommentsUseCase(
    private val contentRepository: LinkContentRepository,
) {
    suspend operator fun invoke(url: String): Result<LinkContent> =
        contentRepository.loadMoreComments(url)
}
