package com.mustafashakir.peek.data.instagram

import com.mustafashakir.peek.domain.repository.LoadProgressListener
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

/**
 * Threads a [LoadProgressListener] through a coroutine's context so [InstagramPageLoader]
 * implementations can report progress without changing the loader's SAM signature.
 */
class PageLoadProgressElement(val listener: LoadProgressListener) :
    AbstractCoroutineContextElement(PageLoadProgressElement) {
    companion object Key : CoroutineContext.Key<PageLoadProgressElement>
}
