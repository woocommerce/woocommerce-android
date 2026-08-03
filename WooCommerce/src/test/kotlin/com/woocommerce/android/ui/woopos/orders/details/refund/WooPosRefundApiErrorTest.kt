package com.woocommerce.android.ui.woopos.orders.details.refund

import com.woocommerce.android.R
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WooPosRefundApiErrorTest {

    @Test
    fun `given refund api error codes, when mapped, then each resolves to its cashier-facing message`() {
        val expectedMappings = mapOf(
            "order_not_refundable" to WooPosRefundApiError.OrderNotRefundable,
            "quantity_exceeds_refundable" to WooPosRefundApiError.QuantityExceedsRefundable,
            "line_item_already_refunded" to WooPosRefundApiError.LineItemAlreadyRefunded,
            "preview_exceeds_max_refundable" to WooPosRefundApiError.AmountExceedsOrderRemaining,
            "refund_exceeds_remaining" to WooPosRefundApiError.AmountExceedsOrderRemaining,
            "refund_total_exceeds_line" to WooPosRefundApiError.AmountExceedsItemRemaining,
            "invalid_refund_amount" to WooPosRefundApiError.InvalidAmount,
        )

        expectedMappings.forEach { (code, expected) ->
            assertThat(WooPosRefundApiError.fromCode(code))
                .describedAs("mapping for code %s", code)
                .isEqualTo(expected)
        }
    }

    @Test
    fun `given refund programming error codes, when mapped, then they stay unmapped for the generic message`() {
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
            assertThat(WooPosRefundApiError.fromCode(code))
                .describedAs("mapping for code %s", code)
                .isNull()
        }
    }

    @Test
    fun `given an unknown or missing code, when mapped, then nothing is resolved`() {
        assertThat(WooPosRefundApiError.fromCode("some_unknown_code")).isNull()
        assertThat(WooPosRefundApiError.fromCode(null)).isNull()
    }

    @Test
    fun `given the two remaining-amount codes, when mapped, then they share one message`() {
        // The preview and the create report the same condition under different codes, so the
        // cashier sees identical copy either way.
        assertThat(WooPosRefundApiError.fromCode("preview_exceeds_max_refundable")?.messageRes)
            .isEqualTo(R.string.woopos_refund_error_amount_exceeds_order_remaining)
        assertThat(WooPosRefundApiError.fromCode("refund_exceeds_remaining")?.messageRes)
            .isEqualTo(R.string.woopos_refund_error_amount_exceeds_order_remaining)
    }
}
