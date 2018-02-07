package com.woocommerce.android.ui.orderlist

import android.content.Context
import com.woocommerce.android.R
import com.woocommerce.android.widgets.tags.ITag

/**
 * Represents a single order status label.
 */
class OrderStatusTag(rawText: String,
                     bgColor: Int,
                     fgColor: Int) : ITag(rawText, bgColor, fgColor) {
    override fun getFormattedLabel(context: Context): String {
        return when (rawText) {
            "pending", "processing" -> context.getString(R.string.orderstatus_new)
            "on-hold" -> context.getString(R.string.orderstatus_hold)
            "completed" -> context.getString(R.string.orderstatus_complete)
            else -> rawText
        }
    }
}
