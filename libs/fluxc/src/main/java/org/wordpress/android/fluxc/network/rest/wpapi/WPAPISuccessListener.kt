package org.wordpress.android.fluxc.network.rest.wpapi

import org.wordpress.android.fluxc.network.rest.GsonRequest
import org.wordpress.android.fluxc.network.rest.Header

/**
 * A listener that turns a WP API response into a [WPAPIResponse.Success] with its status code.
 */
internal fun <T> wpApiSuccessListener(onSuccess: (WPAPIResponse.Success<T>) -> Unit) =
    object : GsonRequest.ResponseListener<T> {
        override fun onResponse(response: T?, headers: List<Header>) = onResponse(response, headers, null)

        override fun onResponse(response: T?, headers: List<Header>, statusCode: Int?) =
            onSuccess(WPAPIResponse.Success(response, headers, statusCode = statusCode))
    }
