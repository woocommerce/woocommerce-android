package com.woocommerce.android.ui.woopos.orders.details.refund

import androidx.annotation.StringRes
import com.woocommerce.android.R

/**
 * Refund API errors returned by the wc/v3 refund preview/create endpoints, mapped to
 * cashier-facing messages. These errors usually mean the order changed since the screen was
 * loaded, for example another register refunded part of the order in the meantime.
 *
 * Matched on the raw API error code carried by `WooError.apiErrorCode` rather than on a
 * `WooErrorType`: the codes are unprefixed and refund-specific, so typing them in FluxC's shared
 * error enum would give them app-wide meaning and force every exhaustive `when` over
 * `WooErrorType` to enumerate refund cases.
 *
 * Programming-error codes (invalid line item ids, malformed payloads, etc.) intentionally have
 * no entry here: they indicate a client bug and keep the generic error message.
 */
enum class WooPosRefundApiError(@StringRes val messageRes: Int) {
    QuantityExceedsRefundable(R.string.woopos_refund_error_quantity_exceeds_refundable),
    LineItemAlreadyRefunded(R.string.woopos_refund_error_item_already_refunded),
    OrderNotRefundable(R.string.woopos_refund_error_order_not_refundable),
    AmountExceedsOrderRemaining(R.string.woopos_refund_error_amount_exceeds_order_remaining),
    AmountExceedsItemRemaining(R.string.woopos_refund_error_amount_exceeds_item_remaining),
    InvalidAmount(R.string.woopos_refund_error_invalid_amount);

    companion object {
        fun fromCode(code: String?): WooPosRefundApiError? = when (code) {
            "quantity_exceeds_refundable" -> QuantityExceedsRefundable
            "line_item_already_refunded" -> LineItemAlreadyRefunded
            "order_not_refundable" -> OrderNotRefundable
            // The preview and the create report the same condition under different codes.
            "preview_exceeds_max_refundable", "refund_exceeds_remaining" -> AmountExceedsOrderRemaining
            "refund_total_exceeds_line" -> AmountExceedsItemRemaining
            "invalid_refund_amount" -> InvalidAmount
            else -> null
        }
    }
}
