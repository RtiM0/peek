package com.mustafashakir.peek.data.resolver

/** Resolves a URL to a source-specific payload. Implementations may only support a subset of URLs. */
fun interface UrlResolver<T> {
    suspend fun resolve(url: String): T

    /** Stable identity for this resolver, persisted so callers can recall which resolver produced a result. */
    val resolverId: String
        get() = this::class.simpleName ?: "unknown"

    /** Whether this resolver is eligible to attempt [url]. */
    fun supports(url: String): Boolean = true
}
