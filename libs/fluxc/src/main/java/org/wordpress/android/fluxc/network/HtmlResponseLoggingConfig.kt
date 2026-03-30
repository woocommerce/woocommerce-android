package org.wordpress.android.fluxc.network

/**
 * Bridge interface for the [HtmlResponseLoggingInterceptor].
 * The WooCommerce app module provides the implementation that reads from preferences
 * and delegates logging, keeping FluxC decoupled from app-level concerns.
 */
interface HtmlResponseLoggingConfig {
    val isEnabled: Boolean

    fun onHtmlResponseDetected(
        endpoint: String,
        statusCode: Int,
        contentType: String?,
        bodyPreview: String,
        redirectTarget: String?
    )
}
