package com.mustafashakir.peek.data.cache

import com.mustafashakir.peek.domain.model.LinkContent
import kotlinx.serialization.Serializable

@Serializable
data class LinkContentCacheEntry(
    val key: String,
    val content: LinkContent,
    val resolverId: String? = null,
)

@Serializable
data class LinkContentCacheDocument(
    val entries: List<LinkContentCacheEntry> = emptyList(),
)
