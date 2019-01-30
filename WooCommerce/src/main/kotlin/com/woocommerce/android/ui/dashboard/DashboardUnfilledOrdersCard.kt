package com.woocommerce.android.ui.dashboard

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.util.StringUtils
import com.woocommerce.android.widgets.SkeletonView
import kotlinx.android.synthetic.main.dashboard_unfilled_orders.view.*

/**
 * Dashboard card that displays the total number of orders awaiting fulfillment and a button to display
 * those orders.
 */
class DashboardUnfilledOrdersCard @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null)
    : FrameLayout(ctx, attrs) {
    private val skeletonView = SkeletonView()

    init {
        View.inflate(context, R.layout.dashboard_unfilled_orders, this)
    }

    interface Listener {
        fun onViewOrdersClicked()
    }

    fun initView(listener: Listener) {
        alertAction_action.setOnClickListener {
            // Track user click event
            AnalyticsTracker.track(Stat.DASHBOARD_UNFULFILLED_ORDERS_BUTTON_TAPPED)

            listener.onViewOrdersClicked()
        }
    }

    fun showSkeleton(show: Boolean) {
        if (show) {
            skeletonView.show(unfilled_card_content, R.layout.skeleton_dashboard_unfilled_orders, delayed = true)
        } else {
            skeletonView.hide()
        }
    }

    /**
     * Updates the title of the unfilled orders dashboard card using strings that match the
     * quantity of the order count.
     */
    fun updateOrdersCount(count: Int) {
        alertAction_title.text = StringUtils.getQuantityString(context, count,
                default = R.string.dashboard_fulfill_order_title_multiple,
                one = R.string.dashboard_fulfill_order_title_single)

        alertAction_action.text = StringUtils.getQuantityString(context, count,
                default = R.string.dashboard_action_view_orders,
                one = R.string.dashboard_action_view_order)
    }
}
