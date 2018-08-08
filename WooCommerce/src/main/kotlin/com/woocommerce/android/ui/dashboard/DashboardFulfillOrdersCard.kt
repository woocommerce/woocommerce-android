package com.woocommerce.android.ui.dashboard

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import android.view.View
import com.woocommerce.android.R
import com.woocommerce.android.util.StringUtils
import kotlinx.android.synthetic.main.dashboard_fulfill_orders.view.*

/**
 * Dashboard card that displays the total number of orders awaiting fulfillment and a button to display
 * those orders.
 */
class DashboardFulfillOrdersCard @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null)
    : ConstraintLayout(ctx, attrs) {
    init {
        View.inflate(context, R.layout.dashboard_fulfill_orders, this)
    }

    interface Listener {
        fun onViewOrdersClicked()
    }

    fun initView(listener: Listener) {
        alertAction_action.setOnClickListener {
            listener.onViewOrdersClicked()
        }
    }

    fun updateOrdersCount(count: Int) {
        val titleTxt = StringUtils.getQuantityString(
                context, count, R.string.dashboard_fulfill_orders_title, one = R.string.dashboard_fulfill_order_title)
        alertAction_title.text = titleTxt

        val buttonTxt = StringUtils.getQuantityString(
                context, count, R.string.dashboard_action_view_orders, one = R.string.dashboard_action_view_order)
        alertAction_action.text = buttonTxt
    }
}
