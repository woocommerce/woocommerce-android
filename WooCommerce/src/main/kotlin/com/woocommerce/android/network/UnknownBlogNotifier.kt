package com.woocommerce.android.network

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.wordpress.android.fluxc.network.rest.wpcom.wc.UnknownBlogListener
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bridges FluxC's [UnknownBlogListener] (invoked from the network layer when WPCom returns the
 * `unknown_blog` error code) to a [Flow] the app can observe to recover from a stale selected site.
 */
@Singleton
class UnknownBlogNotifier @Inject constructor() : UnknownBlogListener {
    // replay = 1 so the one-shot recovery signal isn't lost if it is emitted before the observer subscribes
    private val _unknownBlogEvents = MutableSharedFlow<Long>(replay = 1)
    val unknownBlogEvents: Flow<Long> = _unknownBlogEvents.asSharedFlow()

    override fun onUnknownBlog(siteId: Long) {
        _unknownBlogEvents.tryEmit(siteId)
    }
}
