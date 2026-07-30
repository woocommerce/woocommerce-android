package org.wordpress.android.fluxc.network.rest.wpcom.wc

import org.json.JSONObject
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.AUTHORIZATION_REQUIRED
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.CENSORED
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.HTTP_AUTH_ERROR
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.INVALID_RESPONSE
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.INVALID_SSL_CERTIFICATE
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NETWORK_ERROR
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NOT_AUTHENTICATED
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NOT_FOUND
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NO_CONNECTION
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.PARSE_ERROR
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.SERVER_ERROR
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.TIMEOUT
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.store.Store.OnChangedError

data class WooError(
    val type: WooErrorType,
    val original: GenericErrorType,
    val message: String? = null,
    val apiErrorCode: String? = null,
    val errorData: JSONObject? = null
) : OnChangedError {
    companion object {
        /**
         * Returned by Jetpack (HTTP 400) when signature verification fails on the merchant's site.
         * This is a server-side problem the app cannot fix (broken Jetpack connection, security plugin
         * conflict, outdated Jetpack, etc.).
         */
        const val REST_INVALID_SIGNATURE_CODE = "rest_invalid_signature"
    }
}

enum class WooErrorType {
    TIMEOUT,
    NO_CONNECTION,
    API_ERROR,
    INVALID_ID,
    GENERIC_ERROR,
    INVALID_RESPONSE,
    AUTHORIZATION_REQUIRED,
    INVALID_PARAM,
    API_NOT_FOUND,
    EMPTY_RESPONSE,
    INVALID_COUPON,
    INVALID_VARIATION_ID,
    RESOURCE_ALREADY_EXISTS,
    REST_INVALID_SIGNATURE,

    // Errors returned by the wc/v3 order refund endpoints (preview and create with computed
    // totals). They usually mean the order changed since it was loaded, for example another
    // client refunded part of the order in the meantime.
    ORDER_NOT_REFUNDABLE,
    REFUND_QUANTITY_EXCEEDS_REFUNDABLE,
    REFUND_LINE_ITEM_ALREADY_REFUNDED,
    REFUND_EXCEEDS_REMAINING,
    REFUND_EXCEEDS_LINE_TOTAL,
    INVALID_REFUND_AMOUNT
}

fun WPComGsonNetworkError.toWooError() = WooError(
    type = type.getWooErrorType(apiError),
    original = type,
    message = message,
    apiErrorCode = apiError,
    errorData = errorData,
)

fun WPAPINetworkError.toWooError() = WooError(
    type = type.getWooErrorType(errorCode),
    original = type,
    message = message,
    apiErrorCode = errorCode,
    errorData = errorData,
)

@Suppress("CyclomaticComplexMethod")
private fun GenericErrorType?.getWooErrorType(apiError: String?) = when (this) {
    TIMEOUT -> WooErrorType.TIMEOUT
    NO_CONNECTION -> WooErrorType.NO_CONNECTION
    SERVER_ERROR,
    INVALID_SSL_CERTIFICATE,
    NETWORK_ERROR -> WooErrorType.API_ERROR

    PARSE_ERROR,
    CENSORED,
    INVALID_RESPONSE -> WooErrorType.INVALID_RESPONSE

    HTTP_AUTH_ERROR,
    AUTHORIZATION_REQUIRED,
    NOT_AUTHENTICATED -> WooErrorType.AUTHORIZATION_REQUIRED

    NOT_FOUND -> {
        when (apiError) {
            "rest_no_route" -> WooErrorType.API_NOT_FOUND
            else -> WooErrorType.INVALID_ID
        }
    }

    UNKNOWN, null -> {
        when (apiError) {
            "rest_invalid_param" -> WooErrorType.INVALID_PARAM
            "rest_no_route" -> WooErrorType.API_NOT_FOUND
            "woocommerce_rest_invalid_coupon" -> WooErrorType.INVALID_COUPON
            "order_item_product_invalid_variation_id" -> WooErrorType.INVALID_VARIATION_ID
            WooError.REST_INVALID_SIGNATURE_CODE -> WooErrorType.REST_INVALID_SIGNATURE
            // wc/v3 order refund endpoints (preview and create with computed totals).
            "order_not_refundable" -> WooErrorType.ORDER_NOT_REFUNDABLE
            "quantity_exceeds_refundable" -> WooErrorType.REFUND_QUANTITY_EXCEEDS_REFUNDABLE
            "line_item_already_refunded" -> WooErrorType.REFUND_LINE_ITEM_ALREADY_REFUNDED
            "preview_exceeds_max_refundable",
            "refund_exceeds_remaining" -> WooErrorType.REFUND_EXCEEDS_REMAINING
            "refund_total_exceeds_line" -> WooErrorType.REFUND_EXCEEDS_LINE_TOTAL
            "invalid_refund_amount" -> WooErrorType.INVALID_REFUND_AMOUNT
            else -> WooErrorType.GENERIC_ERROR
        }
    }
}
