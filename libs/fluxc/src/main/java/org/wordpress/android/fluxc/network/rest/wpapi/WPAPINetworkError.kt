package org.wordpress.android.fluxc.network.rest.wpapi

import org.json.JSONObject
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError

class WPAPINetworkError(
    baseError: BaseNetworkError,
    val errorCode: String? = null,
    val errorData: JSONObject? = null
) : BaseNetworkError(baseError)
