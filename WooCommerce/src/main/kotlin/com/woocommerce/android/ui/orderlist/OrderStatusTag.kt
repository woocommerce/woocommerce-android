package com.woocommerce.android.ui.orderlist

import android.content.Context
import com.woocommerce.android.R
import com.woocommerce.android.widgets.tags.ITag
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderStatus

/**
 * Represents a single order status label.
 */
class OrderStatusTag(rawText: String,
                     bgColor: Int,
                     fgColor: Int) : ITag(rawText.trim(), bgColor, fgColor) {
    /**
     * Translates the raw status text received from the server into a friendly
     * localized string.
     */
    override fun getFormattedLabel(context: Context): String {
        return when (rawText.toLowerCase()) {
            OrderStatus.PROCESSING -> context.getString(R.string.orderstatus_processing)
            OrderStatus.PENDING -> context.getString(R.string.orderstatus_pending)
            OrderStatus.FAILED -> context.getString(R.string.orderstatus_failed)
            OrderStatus.COMPLETED -> context.getString(R.string.orderstatus_completed)
            OrderStatus.ON_HOLD -> context.getString(R.string.orderstatus_hold)
            OrderStatus.CANCELLED -> context.getString(R.string.orderstatus_cancelled)
            OrderStatus.REFUNDED -> context.getString(R.string.orderstatus_refunded)
            else -> rawText
        }
    }
}
