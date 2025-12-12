package com.woocommerce.android.util

import org.json.JSONObject
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType

object WooErrorTestUtils {
    fun createInvalidCouponError(
        message: String? = "Invalid coupon",
        apiErrorCode: String? = "woocommerce_rest_invalid_coupon",
        errorData: JSONObject? = null
    ): WooError = WooError(
        type = WooErrorType.INVALID_COUPON,
        original = BaseRequest.GenericErrorType.UNKNOWN,
        message = message,
        apiErrorCode = apiErrorCode,
        errorData = errorData
    )

    fun createInvalidVariationIdError(
        variationId: Long? = null,
    ): WooError = WooError(
        type = WooErrorType.INVALID_VARIATION_ID,
        original = BaseRequest.GenericErrorType.UNKNOWN,
        message = "",
        apiErrorCode = "",
        errorData = variationId?.let {
            mock<JSONObject>().apply {
                whenever(has("variation_id")).thenReturn(true)
                whenever(optLong("variation_id", 0L)).thenReturn(it)
            }
        }
    )
}
