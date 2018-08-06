package com.woocommerce.android.ui.dashboard

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import android.view.View
import com.woocommerce.android.R
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
        val titleTxt = resources.getQuantityString(R.plurals.dashboard_fulfill_orders_title, count, count)
        alertAction_title.text = titleTxt

        val buttonTxt = resources.getQuantityString(R.plurals.dashboard_action_view_orders, count, count)
        alertAction_action.text = buttonTxt
    }
}
