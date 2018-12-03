package com.woocommerce.android.ui.orders

import android.content.Context
import android.support.v4.content.ContextCompat
import com.woocommerce.android.R
import com.woocommerce.android.widgets.tags.ITag
import com.woocommerce.android.widgets.tags.TagConfig
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus

/**
 * Represents a single order status label.
 */
class OrderStatusTag(rawText: String) : ITag(rawText.trim()) {
    override fun getTagConfiguration(context: Context): TagConfig {
        val config = TagConfig(context)

        when (rawText.toLowerCase()) {
            CoreOrderStatus.PROCESSING.value -> {
                config.tagText = context.getString(R.string.orderstatus_processing)
                config.fgColor = ContextCompat.getColor(context, R.color.orderStatus_processing_text)
                config.bgColor = ContextCompat.getColor(context, R.color.orderStatus_processing_bg)
                config.borderColor = ContextCompat.getColor(context, R.color.orderStatus_processing_border)
            }
            CoreOrderStatus.PENDING.value -> {
                config.tagText = context.getString(R.string.orderstatus_pending)
                config.fgColor = ContextCompat.getColor(context, R.color.orderStatus_pending_text)
                config.bgColor = ContextCompat.getColor(context, R.color.orderStatus_pending_bg)
                config.borderColor = ContextCompat.getColor(context, R.color.orderStatus_pending_border)
            }
            CoreOrderStatus.FAILED.value -> {
                config.tagText = context.getString(R.string.orderstatus_failed)
                config.fgColor = ContextCompat.getColor(context, R.color.orderStatus_failed_text)
                config.bgColor = ContextCompat.getColor(context, R.color.orderStatus_failed_bg)
                config.borderColor = ContextCompat.getColor(context, R.color.orderStatus_failed_border)
            }
            CoreOrderStatus.COMPLETED.value -> {
                config.tagText = context.getString(R.string.orderstatus_completed)
                config.fgColor = ContextCompat.getColor(context, R.color.orderStatus_completed_text)
                config.bgColor = ContextCompat.getColor(context, R.color.orderStatus_completed_bg)
                config.borderColor = ContextCompat.getColor(context, R.color.orderStatus_completed_border)
            }
            CoreOrderStatus.ON_HOLD.value -> {
                config.tagText = context.getString(R.string.orderstatus_hold)
                config.fgColor = ContextCompat.getColor(context, R.color.orderStatus_hold_text)
                config.bgColor = ContextCompat.getColor(context, R.color.orderStatus_hold_bg)
                config.borderColor = ContextCompat.getColor(context, R.color.orderStatus_hold_border)
            }
            CoreOrderStatus.CANCELLED.value -> {
                config.tagText = context.getString(R.string.orderstatus_cancelled)
                config.fgColor = ContextCompat.getColor(context, R.color.orderStatus_cancelled_text)
                config.bgColor = ContextCompat.getColor(context, R.color.orderStatus_cancelled_bg)
                config.borderColor = ContextCompat.getColor(context, R.color.orderStatus_cancelled_border)
            }
            CoreOrderStatus.REFUNDED.value -> {
                config.tagText = context.getString(R.string.orderstatus_refunded)
                config.fgColor = ContextCompat.getColor(context, R.color.orderStatus_refunded_text)
                config.bgColor = ContextCompat.getColor(context, R.color.orderStatus_refunded_bg)
                config.borderColor = ContextCompat.getColor(context, R.color.orderStatus_refunded_border)
            }
            else -> {
                config.tagText = rawText
                config.fgColor = ContextCompat.getColor(context, R.color.tagView_text)
                config.bgColor = ContextCompat.getColor(context, R.color.tagView_bg)
                config.borderColor = ContextCompat.getColor(context, R.color.tagView_border_bg)
            }
        }
        return config
    }
}
