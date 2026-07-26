package com.mustafashakir.peek.data.resolver

import kotlinx.coroutines.CancellationException

/** The payload produced by [resolverId], the resolver that won a [PrioritizedUrlResolver] attempt. */
data class Resolved<T>(val value: T, val resolverId: String)

/**
 * Tries [resolvers] for a URL in priority order, skipping any resolver that doesn't
 * [UrlResolver.supports] it, and falling through to the next eligible resolver on any
 * non-cancellation failure. Surfaces the last failure with earlier failures attached as
 * [Throwable.addSuppressed] diagnostics.
 */
class PrioritizedUrlResolver<T>(private val resolvers: List<UrlResolver<T>>) : UrlResolver<T> {
    init {
        require(resolvers.isNotEmpty()) { "PrioritizedUrlResolver requires at least one resolver" }
    }

    override suspend fun resolve(url: String): T = resolveWithSource(url).value

    suspend fun resolveWithSource(url: String): Resolved<T> {
        val eligible = resolvers.filter { it.supports(url) }
        require(eligible.isNotEmpty()) { "No resolver supports url: $url" }

        val failures = mutableListOf<Throwable>()
        for (resolver in eligible) {
            try {
                return Resolved(resolver.resolve(url), resolver.resolverId)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                failures += error
            }
        }
        val final = failures.last()
        failures.dropLast(1).forEach(final::addSuppressed)
        throw final
    }
}
