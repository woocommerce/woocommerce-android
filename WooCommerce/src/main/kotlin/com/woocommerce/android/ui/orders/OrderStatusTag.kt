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
                config.bgColor = ContextCompat.getColor(context, R.color.tag_bg_processing)
            }
            CoreOrderStatus.PENDING.value -> {
                config.bgColor = ContextCompat.getColor(context, R.color.tag_bg_other)
            }
            CoreOrderStatus.FAILED.value -> {
                config.bgColor = ContextCompat.getColor(context, R.color.tag_bg_failed)
            }
            CoreOrderStatus.COMPLETED.value -> {
                config.bgColor = ContextCompat.getColor(context, R.color.tag_bg_other)
            }
            CoreOrderStatus.ON_HOLD.value -> {
                config.bgColor = ContextCompat.getColor(context, R.color.tag_bg_on_hold)
            }
            CoreOrderStatus.CANCELLED.value -> {
                config.bgColor = ContextCompat.getColor(context, R.color.tag_bg_other)
            }
            CoreOrderStatus.REFUNDED.value -> {
                config.bgColor = ContextCompat.getColor(context, R.color.tag_bg_other)
            }
            else -> {
                config.bgColor = ContextCompat.getColor(context, R.color.tagView_bg)
            }
        }
        return config
    }
}
