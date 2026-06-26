package org.wordpress.android.fluxc.network.rest.wpcom.wc

import org.wordpress.android.fluxc.model.SiteModel

/**
 * Listener for detecting the [WooError.REST_INVALID_SIGNATURE_CODE] error centrally at the network layer.
 *
 * Jetpack returns this error when signature verification fails on the merchant's site, which is a server-side
 * problem the app cannot fix. The implementation can surface this to the merchant and stop silent retries.
 */
interface InvalidSignatureListener {
    /**
     * Called when a request for the given [siteModel] fails with the invalid signature error.
     */
    fun onInvalidSignatureDetected(siteModel: SiteModel)

    /**
     * Called when a request for the given [siteModel] succeeds, allowing the implementation to clear any
     * previously recorded invalid signature state (self-healing once the server-side issue is resolved).
     */
    fun onSuccessfulConnection(siteModel: SiteModel) {}
}
