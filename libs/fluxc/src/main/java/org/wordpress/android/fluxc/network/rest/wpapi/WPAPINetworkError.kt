package org.wordpress.android.fluxc.network.rest.wpapi

import org.json.JSONObject
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError

class WPAPINetworkError(
    baseError: BaseNetworkError,
    val errorCode: String? = null,
    val errorData: JSONObject? = null
) : BaseNetworkError(baseError)

internal fun BaseNetworkError.toWPAPINetworkError(): WPAPINetworkError {
    return when (this) {
        is WPAPINetworkError -> this
        is WPComGsonNetworkError -> WPAPINetworkError(
            baseError = this,
            errorCode = this.apiError
        )

        else -> WPAPINetworkError(this)
    }
}
