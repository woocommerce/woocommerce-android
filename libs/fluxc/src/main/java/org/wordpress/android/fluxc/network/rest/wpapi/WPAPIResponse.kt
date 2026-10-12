package org.wordpress.android.fluxc.network.rest.wpapi

import org.wordpress.android.fluxc.network.rest.Header

sealed class WPAPIResponse<T> {
    data class Success<T>(
        val data: T?,
        val headers: List<Header>,
        val networkingMode: WPAPINetworkingMode? = null,
        /**
         * The store's HTTP status, or null when it isn't known, like through the Jetpack tunnel.
         */
        val statusCode: Int? = null,
    ) : WPAPIResponse<T>()

    data class Error<T>(
        val error: WPAPINetworkError,
        val networkingMode: WPAPINetworkingMode? = null,
    ) : WPAPIResponse<T>()
}

/**
 * The networking mode that was used to make the request.
 */
sealed interface WPAPINetworkingMode {
    data object ApplicationPasswords : WPAPINetworkingMode
    data object ApplicationPasswordsWithJetpack : WPAPINetworkingMode
    data class JetpackTunnel(
        val isFallback: Boolean = false,
        val applicationPasswordsError: WPAPINetworkError? = null,
    ) : WPAPINetworkingMode
}
