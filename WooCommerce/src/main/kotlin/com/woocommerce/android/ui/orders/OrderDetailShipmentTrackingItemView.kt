package com.woocommerce.android.ui.orders

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import android.view.View
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.util.WooLog
import kotlinx.android.synthetic.main.order_detail_shipment_tracking_item.view.*
import org.wordpress.android.fluxc.model.WCOrderShipmentTrackingModel

class OrderDetailShipmentTrackingItemView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null
) : ConstraintLayout(ctx, attrs) {
    init {
        View.inflate(context, R.layout.order_detail_shipment_tracking_item, this)
    }

    fun initView(item: WCOrderShipmentTrackingModel, uiMessageResolver: UIMessageResolver) {
        tracking_type.text = item.trackingProvider
        tracking_number.text = item.trackingNumber

        tracking_copyNumber.setOnClickListener {
            try {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.primaryClip = ClipData.newPlainText(
                        context.getString(R.string.order_shipment_tracking_number),
                        context.getString(R.string.order_shipment_tracking_number_clipboard))
                uiMessageResolver.showSnack(R.string.order_shipment_tracking_number_clipboard)
            } catch (e: Exception) {
                WooLog.e(WooLog.T.UTILS, e)
                uiMessageResolver.showSnack(R.string.error_copy_to_clipboard)
            }
        }

        if (item.trackingLink.isNotEmpty()) {
            tracking_btnTrack.setOnClickListener {
                AnalyticsTracker.track(Stat.ORDER_DETAIL_TRACK_PACKAGE_BUTTON_TAPPED)
                ChromeCustomTabUtils.launchUrl(context, item.trackingLink)
            }
        }
    }
}
