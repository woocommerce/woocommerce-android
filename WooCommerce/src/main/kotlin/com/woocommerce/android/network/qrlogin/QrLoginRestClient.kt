package com.woocommerce.android.network.qrlogin

import com.woocommerce.android.ui.login.qrlogin.Secret

data class QrLoginCredentials(
    val userLogin: String,
    val applicationPassword: Secret,
    val uuid: String?
)

sealed class QrLoginExchangeException(message: String) : Exception(message) {
    data object TokenRejected : QrLoginExchangeException("Token was rejected by the site")
    data object EndpointMissing : QrLoginExchangeException("Exchange endpoint not available on the site")
    data object RateLimited : QrLoginExchangeException("Rate limit hit on the exchange endpoint")
    data object Network : QrLoginExchangeException("Network failure during exchange")
    data object MalformedResponse : QrLoginExchangeException("Exchange response was malformed")
    data class HttpError(val code: Int) : QrLoginExchangeException("HTTP $code from exchange endpoint")
    data class Unknown(val original: Throwable) :
        QrLoginExchangeException("Unknown exchange failure: ${original.javaClass.simpleName}")
}
