package org.wordpress.android.fluxc.network.rest.wpapi

sealed class WPAPIResponse<T> {
    data class Success<T>(
        val data: T?,
        val networkingMode: WPAPINetworkingMode? = null,
    ) : WPAPIResponse<T>()

    data class Error<T>(val error: WPAPINetworkError) : WPAPIResponse<T>()
}

/**
 * The networking mode that was used to make the request.
 *
 * This is used for tracking purposes, and used only with the Woo experimental networking mode.
 */
sealed interface WPAPINetworkingMode {
    data object ApplicationPasswords : WPAPINetworkingMode
    data object ApplicationPasswordsWithJetpack : WPAPINetworkingMode
    data class JetpackTunnel(
        val isFallback: Boolean = false,
        val applicationPasswordsError: WPAPINetworkError? = null,
    ) : WPAPINetworkingMode
}
