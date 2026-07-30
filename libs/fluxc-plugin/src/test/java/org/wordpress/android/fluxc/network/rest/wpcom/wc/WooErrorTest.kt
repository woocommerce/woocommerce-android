package org.wordpress.android.fluxc.network.rest.wpcom.wc

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetworkError

class WooErrorTest {

    @Test
    fun `given refund api error codes, when converting, then each maps to its dedicated type`() {
        val expectedMappings = mapOf(
            "order_not_refundable" to WooErrorType.ORDER_NOT_REFUNDABLE,
            "quantity_exceeds_refundable" to WooErrorType.REFUND_QUANTITY_EXCEEDS_REFUNDABLE,
            "line_item_already_refunded" to WooErrorType.REFUND_LINE_ITEM_ALREADY_REFUNDED,
            "preview_exceeds_max_refundable" to WooErrorType.REFUND_EXCEEDS_REMAINING,
            "refund_exceeds_remaining" to WooErrorType.REFUND_EXCEEDS_REMAINING,
            "refund_total_exceeds_line" to WooErrorType.REFUND_EXCEEDS_LINE_TOTAL,
            "invalid_refund_amount" to WooErrorType.INVALID_REFUND_AMOUNT,
        )

        expectedMappings.forEach { (code, expectedType) ->
            val error = wpApiError(code).toWooError()

            assertThat(error.type).describedAs("type for code %s", code).isEqualTo(expectedType)
            assertThat(error.apiErrorCode).describedAs("apiErrorCode for code %s", code).isEqualTo(code)
        }
    }

    @Test
    fun `given refund programming error codes, when converting, then they stay generic`() {
        val programmingErrorCodes = listOf(
            "invalid_line_item",
            "invalid_quantity",
            "invalid_refund_total",
            "missing_quantity_or_refund_total",
            "duplicate_line_item",
            "line_item_not_found",
            "missing_line_items",
        )

        programmingErrorCodes.forEach { code ->
            val error = wpApiError(code).toWooError()

            assertThat(error.type).describedAs("type for code %s", code).isEqualTo(WooErrorType.GENERIC_ERROR)
            assertThat(error.apiErrorCode).describedAs("apiErrorCode for code %s", code).isEqualTo(code)
        }
    }

    @Test
    fun `given unknown api error code, when converting, then falls back to generic error`() {
        val error = wpApiError("some_unknown_code").toWooError()

        assertThat(error.type).isEqualTo(WooErrorType.GENERIC_ERROR)
        assertThat(error.apiErrorCode).isEqualTo("some_unknown_code")
    }

    private fun wpApiError(code: String) = WPAPINetworkError(
        BaseNetworkError(GenericErrorType.UNKNOWN),
        code
    )
}
