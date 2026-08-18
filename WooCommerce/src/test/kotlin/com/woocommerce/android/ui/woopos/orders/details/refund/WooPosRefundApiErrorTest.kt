package com.woocommerce.android.ui.woopos.orders.details.refund

import com.woocommerce.android.R
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WooPosRefundApiErrorTest {

    @Test
    fun `given refund api error codes, when mapped, then each resolves to its cashier-facing message`() {
        val expectedMappings = mapOf(
            "woocommerce_rest_order_not_refundable" to WooPosRefundApiError.OrderNotRefundable,
            "woocommerce_rest_quantity_exceeds_refundable" to WooPosRefundApiError.QuantityExceedsRefundable,
            "woocommerce_rest_line_item_already_refunded" to WooPosRefundApiError.LineItemAlreadyRefunded,
            "woocommerce_rest_preview_exceeds_max_refundable" to WooPosRefundApiError.AmountExceedsOrderRemaining,
            "woocommerce_rest_refund_exceeds_remaining" to WooPosRefundApiError.AmountExceedsOrderRemaining,
            "woocommerce_rest_refund_total_exceeds_line" to WooPosRefundApiError.AmountExceedsItemRemaining,
            "woocommerce_rest_invalid_refund_amount" to WooPosRefundApiError.InvalidAmount,
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
            "woocommerce_rest_invalid_line_item",
            "woocommerce_rest_invalid_quantity",
            "woocommerce_rest_invalid_refund_total",
            "woocommerce_rest_missing_quantity_or_refund_total",
            "woocommerce_rest_duplicate_line_item",
            "woocommerce_rest_line_item_not_found",
            "woocommerce_rest_missing_line_items",
        )

        programmingErrorCodes.forEach { code ->
            assertThat(WooPosRefundApiError.fromCode(code))
                .describedAs("mapping for code %s", code)
                .isNull()
        }
    }

    @Test
    fun `given refund api errors, when recovering, then only the ones a reload can fix offer it`() {
        val expectedRecoveries = mapOf(
            WooPosRefundApiError.QuantityExceedsRefundable to WooPosRefundState.Recovery.RefreshItems,
            WooPosRefundApiError.LineItemAlreadyRefunded to WooPosRefundState.Recovery.RefreshItems,
            WooPosRefundApiError.AmountExceedsOrderRemaining to WooPosRefundState.Recovery.RefreshItems,
            WooPosRefundApiError.AmountExceedsItemRemaining to WooPosRefundState.Recovery.RefreshItems,
            WooPosRefundApiError.OrderNotRefundable to WooPosRefundState.Recovery.None,
            WooPosRefundApiError.InvalidAmount to WooPosRefundState.Recovery.None,
        )

        WooPosRefundApiError.entries.forEach { apiError ->
            assertThat(apiError.recovery)
                .describedAs("recovery for %s", apiError)
                .isEqualTo(expectedRecoveries.getValue(apiError))
        }
    }

    @Test
    fun `given refund api errors, when recovering, then none of them offer a retry`() {
        // Every mapped code is a deterministic validation rejection: the same request always gets
        // the same answer, so a retry cannot be the way out.
        assertThat(WooPosRefundApiError.entries.map { it.recovery })
            .doesNotContain(WooPosRefundState.Recovery.Retry)
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
        assertThat(WooPosRefundApiError.fromCode("woocommerce_rest_preview_exceeds_max_refundable")?.messageRes)
            .isEqualTo(R.string.woopos_refund_error_amount_exceeds_order_remaining)
        assertThat(WooPosRefundApiError.fromCode("woocommerce_rest_refund_exceeds_remaining")?.messageRes)
            .isEqualTo(R.string.woopos_refund_error_amount_exceeds_order_remaining)
    }
}
