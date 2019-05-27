package com.woocommerce.android.ui.orders

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import android.view.View
import android.widget.PopupMenu
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.widgets.AppRatingDialog
import kotlinx.android.synthetic.main.order_detail_shipment_tracking_item.view.*
import org.wordpress.android.fluxc.model.WCOrderShipmentTrackingModel

class OrderDetailShipmentTrackingItemView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null
) : ConstraintLayout(ctx, attrs) {
    init {
        View.inflate(context, R.layout.order_detail_shipment_tracking_item, this)
    }

    private var listener: OrderShipmentTrackingActionListener? = null

    fun initView(
        item: WCOrderShipmentTrackingModel,
        uiMessageResolver: UIMessageResolver,
        allowAddTrackingOption: Boolean,
        shipmentTrackingActionListener: OrderShipmentTrackingActionListener?
    ) {
        this.listener = shipmentTrackingActionListener
        tracking_type.text = item.trackingProvider
        tracking_number.text = item.trackingNumber
        tracking_dateShipped.text = context.getString(
                R.string.order_shipment_tracking_shipped_date,
                DateUtils.getLocalizedLongDateString(context, item.dateShipped))

        tracking_copyNumber.setOnClickListener {
            try {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.primaryClip = ClipData.newPlainText(
                        context.getString(R.string.order_shipment_tracking_number),
                        item.trackingNumber)
                uiMessageResolver.showSnack(R.string.order_shipment_tracking_number_clipboard)
            } catch (e: Exception) {
                WooLog.e(WooLog.T.UTILS, e)
                uiMessageResolver.showSnack(R.string.error_copy_to_clipboard)
            }
        }

        if (!allowAddTrackingOption) {
            tracking_btnTrack.visibility = View.VISIBLE
            tracking_btnTrack.setOnClickListener {
                showTrackingOrDeleteOptionPopup(item)
            }
        } else {
            tracking_btnTrack.visibility = View.GONE
            tracking_btnDelete.visibility = View.VISIBLE
            tracking_btnDelete.setOnClickListener {
                AnalyticsTracker.track(Stat.ORDER_TRACKING_DELETE, mapOf(
                        AnalyticsTracker.KEY_SOURCE to AnalyticsTracker.VALUE_ORDER_FULFILL
                ))
                AppRatingDialog.incrementInteractions()
                listener?.deleteOrderShipmentTracking(item)
            }
        }
    }

    private fun showTrackingOrDeleteOptionPopup(item: WCOrderShipmentTrackingModel) {
        val popup = PopupMenu(context, tracking_btnTrack)
        popup.menuInflater.inflate(R.menu.menu_order_detail_shipment_tracking_actions, popup.menu)

        /**
         * Track shipment menu option is only displayed if the tracking link
         * is not empty
         */
        if (item.trackingLink.isNotEmpty()) {
            popup.menu.findItem(R.id.menu_track_shipment)?.isVisible = true
            popup.menu.findItem(R.id.menu_track_shipment)?.setOnMenuItemClickListener {
                AnalyticsTracker.track(Stat.ORDER_DETAIL_TRACK_PACKAGE_BUTTON_TAPPED)
                ChromeCustomTabUtils.launchUrl(context, item.trackingLink)
                AppRatingDialog.incrementInteractions()
                true
            }
        }

        popup.menu.findItem(R.id.menu_delete_shipment)?.setOnMenuItemClickListener {
            AnalyticsTracker.track(Stat.ORDER_TRACKING_DELETE, mapOf(
                    AnalyticsTracker.KEY_SOURCE to AnalyticsTracker.VALUE_ORDER_DETAIL
            ))
            listener?.deleteOrderShipmentTracking(item)
            AppRatingDialog.incrementInteractions()
            true
        }

        popup.show()
    }
}
