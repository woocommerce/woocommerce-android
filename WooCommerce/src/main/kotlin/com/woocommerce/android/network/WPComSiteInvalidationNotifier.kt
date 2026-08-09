package com.woocommerce.android.network

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WPComSiteInvalidationEvent
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WPComSiteInvalidationListener
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bridges FluxC's [WPComSiteInvalidationListener] to a [Flow] the app can observe to recover from
 * an invalid site.
 */
@Singleton
class WPComSiteInvalidationNotifier @Inject constructor() : WPComSiteInvalidationListener {
    // replay = 1 so the one-shot recovery signal isn't lost if it is emitted before the observer subscribes
    private val _siteInvalidationEvents = MutableSharedFlow<WPComSiteInvalidationEvent>(replay = 1)
    val siteInvalidationEvents: Flow<WPComSiteInvalidationEvent> = _siteInvalidationEvents.asSharedFlow()

    override fun onSiteInvalidated(event: WPComSiteInvalidationEvent) {
        _siteInvalidationEvents.tryEmit(event)
    }
}
