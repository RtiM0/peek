package com.mustafashakir.peek.data.recent

import kotlinx.serialization.Serializable

@Serializable
data class RecentLinkRecord(
    val url: String,
    val openedAtEpochMillis: Long,
)

@Serializable
data class RecentLinksDocument(
    val links: List<RecentLinkRecord> = emptyList(),
)
