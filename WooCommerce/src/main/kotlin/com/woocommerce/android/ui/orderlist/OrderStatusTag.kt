package com.woocommerce.android.ui.orderlist

import android.content.Context
import com.woocommerce.android.R
import com.woocommerce.android.widgets.tags.ITag

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
            STATUS_PROCESSING -> context.getString(R.string.orderstatus_processing)
            STATUS_PENDING -> context.getString(R.string.orderstatus_pending)
            STATUS_FAILED -> context.getString(R.string.orderstatus_failed)
            STATUS_COMPLETED -> context.getString(R.string.orderstatus_completed)
            STATUS_HOLD -> context.getString(R.string.orderstatus_hold)
            STATUS_CANCELLED -> context.getString(R.string.orderstatus_cancelled)
            STATUS_REFUNDED -> context.getString(R.string.orderstatus_refunded)
            else -> rawText
        }
    }
}
