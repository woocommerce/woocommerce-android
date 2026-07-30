package com.woocommerce.android.ui.woopos.orders.details.refund

import androidx.annotation.StringRes
import com.woocommerce.android.R
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType

/**
 * Refund API errors returned by the wc/v3 refund preview/create endpoints, mapped to
 * cashier-facing messages. These errors usually mean the order changed since the screen was
 * loaded, for example another register refunded part of the order in the meantime.
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
        fun fromWooErrorType(type: WooErrorType?): WooPosRefundApiError? = when (type) {
            WooErrorType.REFUND_QUANTITY_EXCEEDS_REFUNDABLE -> QuantityExceedsRefundable
            WooErrorType.REFUND_LINE_ITEM_ALREADY_REFUNDED -> LineItemAlreadyRefunded
            WooErrorType.ORDER_NOT_REFUNDABLE -> OrderNotRefundable
            WooErrorType.REFUND_EXCEEDS_REMAINING -> AmountExceedsOrderRemaining
            WooErrorType.REFUND_EXCEEDS_LINE_TOTAL -> AmountExceedsItemRemaining
            WooErrorType.INVALID_REFUND_AMOUNT -> InvalidAmount
            else -> null
        }
    }
}
