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
    override fun getFormattedLabel(context: Context): String {
        return when (rawText.toLowerCase()) {
            "processing" -> context.getString(R.string.orderstatus_processing)
            "pending" -> context.getString(R.string.orderstatus_pending)
            "failed" -> context.getString(R.string.orderstatus_failed)
            "completed" -> context.getString(R.string.orderstatus_completed)
            "on-hold" -> context.getString(R.string.orderstatus_hold)
            "cancelled" -> context.getString(R.string.orderstatus_cancelled)
            "refunded" -> context.getString(R.string.orderstatus_refunded)
            else -> rawText
        }
    }
}
