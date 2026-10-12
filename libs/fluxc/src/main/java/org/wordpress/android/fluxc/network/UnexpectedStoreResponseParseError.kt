package org.wordpress.android.fluxc.network

import com.android.volley.ParseError

/**
 * A parse error for a 2xx store response that was not JSON. Volley drops the response once parsing fails, so the
 * details are kept here until the error is delivered.
 */
class UnexpectedStoreResponseParseError(
    cause: Throwable?,
    val unexpectedStoreResponse: UnexpectedStoreResponse
) : ParseError(cause)
