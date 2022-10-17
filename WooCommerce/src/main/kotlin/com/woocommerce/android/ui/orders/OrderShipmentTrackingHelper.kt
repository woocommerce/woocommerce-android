package com.woocommerce.android.ui.orders

import android.content.Context
import android.view.View
import android.widget.PopupMenu
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.copyToClipboard
import com.woocommerce.android.widgets.AppRatingDialog
import org.wordpress.android.util.ToastUtils

object OrderShipmentTrackingHelper {
    private fun copyTrackingNumber(
        context: Context,
        trackingNumber: String
    ) {
        try {
            context.copyToClipboard(context.getString(R.string.order_shipment_tracking_number), trackingNumber)
            ToastUtils.showToast(context, R.string.order_shipment_tracking_number_clipboard)
        } catch (e: Exception) {
            WooLog.e(WooLog.T.UTILS, e)
            ToastUtils.showToast(context, R.string.error_copy_to_clipboard)
        }
    }

    fun trackShipment(
        context: Context,
        trackingLink: String
    ) {
        ChromeCustomTabUtils.launchUrl(context, trackingLink)
        AppRatingDialog.incrementInteractions()
    }

    fun showTrackingOrDeleteOptionPopup(
        anchor: View,
        context: Context,
        trackingLink: String,
        trackingNumber: String,
        onDeleteTrackingClicked: ((trackingNumber: String) -> Unit)? = null
    ) {
        val popup = PopupMenu(context, anchor)
        popup.menuInflater.inflate(R.menu.menu_order_detail_shipment_tracking_actions, popup.menu)

        with(popup.menu.findItem(R.id.menu_track_shipment)) {
            isVisible = trackingLink.isNotEmpty()
            setOnMenuItemClickListener {
                AnalyticsTracker.track(
                    AnalyticsEvent.SHIPMENT_TRACKING_MENU_ACTION,
                    mapOf(
                        AnalyticsTracker.KEY_FEEDBACK_ACTION to AnalyticsTracker.VALUE_SHIPMENT_TRACK
                    )
                )
                trackShipment(context, trackingLink)
                true
            }
        }

        popup.menu.findItem(R.id.menu_copy_tracking)?.setOnMenuItemClickListener {
            AnalyticsTracker.track(
                AnalyticsEvent.SHIPMENT_TRACKING_MENU_ACTION,
                mapOf(
                    AnalyticsTracker.KEY_FEEDBACK_ACTION to AnalyticsTracker.VALUE_SHIPMENT_COPY
                )
            )
            copyTrackingNumber(context, trackingNumber)
            true
        }

        popup.menu.findItem(R.id.menu_delete_shipment)?.isVisible = onDeleteTrackingClicked != null
        onDeleteTrackingClicked?.let {
            popup.menu.findItem(R.id.menu_delete_shipment)?.setOnMenuItemClickListener {
                onDeleteTrackingClicked(trackingNumber)
                AppRatingDialog.incrementInteractions()
                true
            }
        }

        popup.show()
    }
}
