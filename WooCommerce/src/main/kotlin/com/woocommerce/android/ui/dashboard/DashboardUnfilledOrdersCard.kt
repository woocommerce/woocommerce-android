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
     *
     * NOTE: The title text is not using the [StringUtils.getQuantityString] method because it
     * temporarily requires a way of displaying a "+" sign if [canLoadMore] is true. Once we
     * have an API endpoint that can deliver a total for all orders this can be removed.
     */
    fun updateOrdersCount(count: Int, canLoadMore: Boolean) {
        alertAction_title.text = when {
            count == 1 -> resources.getString(R.string.dashboard_fulfill_order_title)
            canLoadMore -> resources.getString(R.string.dashboard_fulfill_orders_title, count, "+")
            else -> resources.getString(R.string.dashboard_fulfill_orders_title, count, "")
        }

        alertAction_action.text = StringUtils.getQuantityString(
                context, count, R.string.dashboard_action_view_orders, one = R.string.dashboard_action_view_order)
    }
}
