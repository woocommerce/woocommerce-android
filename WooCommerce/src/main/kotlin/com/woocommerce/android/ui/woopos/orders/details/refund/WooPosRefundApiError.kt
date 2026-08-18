package com.woocommerce.android.ui.woopos.orders.details.refund

import androidx.annotation.StringRes
import com.woocommerce.android.R

/**
 * Refund API errors returned by the wc/v3 refund preview/create endpoints, mapped to
 * cashier-facing messages. These errors usually mean the order changed since the screen was
 * loaded, for example another register refunded part of the order in the meantime.
 *
 * Matched on the raw API error code carried by `WooError.apiErrorCode` rather than on a
 * `WooErrorType`: the codes are refund-specific, so typing them in FluxC's shared error enum
 * would give them app-wide meaning and force every exhaustive `when` over `WooErrorType` to
 * enumerate refund cases.
 *
 * Programming-error codes (invalid line item ids, malformed payloads, etc.) intentionally have
 * no entry here: they indicate a client bug and keep the generic error message.
 */
enum class WooPosRefundApiError(
    @StringRes val messageRes: Int,
    val recovery: WooPosRefundState.Recovery,
) {
    QuantityExceedsRefundable(
        R.string.woopos_refund_error_quantity_exceeds_refundable,
        WooPosRefundState.Recovery.RefreshItems,
    ),
    LineItemAlreadyRefunded(
        R.string.woopos_refund_error_item_already_refunded,
        WooPosRefundState.Recovery.RefreshItems,
    ),

    /** Nothing is left to refund. Both paths route this to [WooPosRefundState.NoRefundableItems]. */
    OrderNotRefundable(
        R.string.woopos_refund_error_order_not_refundable,
        WooPosRefundState.Recovery.None,
    ),
    AmountExceedsOrderRemaining(
        R.string.woopos_refund_error_amount_exceeds_order_remaining,
        WooPosRefundState.Recovery.RefreshItems,
    ),
    AmountExceedsItemRemaining(
        R.string.woopos_refund_error_amount_exceeds_item_remaining,
        WooPosRefundState.Recovery.RefreshItems,
    ),

    /** The amount is malformed, not too large, so a fresh item list does not help. */
    InvalidAmount(
        R.string.woopos_refund_error_invalid_amount,
        WooPosRefundState.Recovery.None,
    );

    companion object {
        fun fromCode(code: String?): WooPosRefundApiError? = when (code) {
            "woocommerce_rest_quantity_exceeds_refundable" -> QuantityExceedsRefundable
            "woocommerce_rest_line_item_already_refunded" -> LineItemAlreadyRefunded
            "woocommerce_rest_order_not_refundable" -> OrderNotRefundable
            // The preview and the create report the same condition under different codes.
            "woocommerce_rest_preview_exceeds_max_refundable",
            "woocommerce_rest_refund_exceeds_remaining" -> AmountExceedsOrderRemaining
            "woocommerce_rest_refund_total_exceeds_line" -> AmountExceedsItemRemaining
            "woocommerce_rest_invalid_refund_amount" -> InvalidAmount
            else -> null
        }
    }
}
