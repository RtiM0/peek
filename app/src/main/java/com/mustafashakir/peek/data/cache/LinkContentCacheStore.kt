package com.mustafashakir.peek.data.cache

import androidx.datastore.core.DataStore
import com.mustafashakir.peek.domain.model.LinkContent
import kotlinx.coroutines.flow.first

/** A cached [content], tagged with the id of the resolver that produced it, if known. */
data class CachedLinkContent(val content: LinkContent, val resolverId: String?)

/** Persists resolved [LinkContent] across process restarts, keyed by a source-specific canonical key. */
class LinkContentCacheStore(
    private val dataStore: DataStore<LinkContentCacheDocument>,
) {
    suspend fun get(key: String): CachedLinkContent? =
        dataStore.data.first().entries.firstOrNull { it.key == key }
            ?.let { CachedLinkContent(it.content, it.resolverId) }

    suspend fun put(key: String, content: LinkContent, resolverId: String) {
        dataStore.updateData { current ->
            current.copy(
                entries = listOf(LinkContentCacheEntry(key, content, resolverId)) +
                    current.entries.filterNot { it.key == key },
            )
        }
    }

    suspend fun remove(key: String) {
        dataStore.updateData { current ->
            current.copy(entries = current.entries.filterNot { it.key == key })
        }
    }
}
