package com.woocommerce.android.ui.orders

import android.content.Context
import androidx.core.content.ContextCompat
import com.woocommerce.android.R
import com.woocommerce.android.widgets.tags.ITag
import com.woocommerce.android.widgets.tags.TagConfig
import org.wordpress.android.fluxc.model.WCOrderStatusModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus

/**
 * Represents a single order status label.
 */
class OrderStatusTag(private val orderStatus: WCOrderStatusModel) : ITag(orderStatus.statusKey.trim()) {
    override fun getTagConfiguration(context: Context): TagConfig {
        val config = TagConfig(context).apply { tagText = orderStatus.label }

        when (rawText.toLowerCase()) {
            CoreOrderStatus.PROCESSING.value -> {
                config.fgColor = ContextCompat.getColor(context, R.color.orderStatus_processing_text)
                config.bgColor = ContextCompat.getColor(context, R.color.orderStatus_processing_bg)
                config.borderColor = ContextCompat.getColor(context, R.color.orderStatus_processing_border)
            }
            CoreOrderStatus.PENDING.value -> {
                config.fgColor = ContextCompat.getColor(context, R.color.orderStatus_pending_text)
                config.bgColor = ContextCompat.getColor(context, R.color.orderStatus_pending_bg)
                config.borderColor = ContextCompat.getColor(context, R.color.orderStatus_pending_border)
            }
            CoreOrderStatus.FAILED.value -> {
                config.fgColor = ContextCompat.getColor(context, R.color.orderStatus_failed_text)
                config.bgColor = ContextCompat.getColor(context, R.color.orderStatus_failed_bg)
                config.borderColor = ContextCompat.getColor(context, R.color.orderStatus_failed_border)
            }
            CoreOrderStatus.COMPLETED.value -> {
                config.fgColor = ContextCompat.getColor(context, R.color.orderStatus_completed_text)
                config.bgColor = ContextCompat.getColor(context, R.color.orderStatus_completed_bg)
                config.borderColor = ContextCompat.getColor(context, R.color.orderStatus_completed_border)
            }
            CoreOrderStatus.ON_HOLD.value -> {
                config.fgColor = ContextCompat.getColor(context, R.color.orderStatus_hold_text)
                config.bgColor = ContextCompat.getColor(context, R.color.orderStatus_hold_bg)
                config.borderColor = ContextCompat.getColor(context, R.color.orderStatus_hold_border)
            }
            CoreOrderStatus.CANCELLED.value -> {
                config.fgColor = ContextCompat.getColor(context, R.color.orderStatus_cancelled_text)
                config.bgColor = ContextCompat.getColor(context, R.color.orderStatus_cancelled_bg)
                config.borderColor = ContextCompat.getColor(context, R.color.orderStatus_cancelled_border)
            }
            CoreOrderStatus.REFUNDED.value -> {
                config.fgColor = ContextCompat.getColor(context, R.color.orderStatus_refunded_text)
                config.bgColor = ContextCompat.getColor(context, R.color.orderStatus_refunded_bg)
                config.borderColor = ContextCompat.getColor(context, R.color.orderStatus_refunded_border)
            }
            else -> {
                config.fgColor = ContextCompat.getColor(context, R.color.tagView_text)
                config.bgColor = ContextCompat.getColor(context, R.color.tagView_bg)
                config.borderColor = ContextCompat.getColor(context, R.color.tagView_border_bg)
            }
        }
        return config
    }
}
