package com.mustafashakir.peek.domain.repository

import com.mustafashakir.peek.domain.model.LinkContent
import com.mustafashakir.peek.domain.model.LoadProgress
import com.mustafashakir.peek.domain.model.RecentLink
import kotlinx.coroutines.flow.Flow

fun interface LoadProgressListener {
    fun onProgress(progress: LoadProgress)
}

interface LinkContentRepository {
    suspend fun resolve(url: String): Result<LinkContent>

    /** Same as [resolve], additionally reporting progress if the underlying source can. */
    suspend fun resolve(url: String, onProgress: LoadProgressListener): Result<LinkContent> = resolve(url)

    /** Returns cached content for [url] without triggering a network load, or null if not cached. */
    suspend fun peekCached(url: String): LinkContent?

    /** Loads the next cached comments page, if the source has one. */
    suspend fun loadMoreComments(url: String): Result<LinkContent> = Result.failure(
        UnsupportedOperationException("Comment pagination is not supported by this source"),
    )

    /** Discards any cached content for [url] and resolves it again from the network. */
    suspend fun refresh(url: String): Result<LinkContent>

    /** Same as [refresh], additionally reporting progress if the underlying source can. */
    suspend fun refresh(url: String, onProgress: LoadProgressListener): Result<LinkContent> = refresh(url)
}

interface RecentLinksRepository {
    fun observeRecents(): Flow<List<RecentLink>>
    suspend fun markOpened(url: String)
}
