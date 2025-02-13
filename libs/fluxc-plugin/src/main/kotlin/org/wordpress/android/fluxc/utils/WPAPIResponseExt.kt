package org.wordpress.android.fluxc.utils

import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.toWooError

inline fun <reified T> WPAPIResponse<T>.toWooPayload(): WooPayload<T> =
    this.toWooPayload { it }

/**
 * Converts a [WPAPIResponse] to [WooPayload] by using the provided [mapper] for the conversion.
 *
 * @param mapper a mapper function to map between the input and return types
 */
inline fun <reified T, reified R> WPAPIResponse<T>.toWooPayload(
    mapper: (T) -> R
): WooPayload<R> = when (this) {
    is WPAPIResponse.Success -> {
        val result = data
        if (result == null) {
            if (null !is R) {
                WooPayload(
                    WooError(
                        type = WooErrorType.EMPTY_RESPONSE,
                        original = BaseRequest.GenericErrorType.UNKNOWN,
                        message = "Success response with empty data"
                    )
                )
            } else {
                WooPayload(null)
            }
        } else {
            WooPayload(mapper(result))
        }
    }
    is WPAPIResponse.Error -> WooPayload(error.toWooError())
}
