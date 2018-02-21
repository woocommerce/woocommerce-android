package com.woocommerce.android.ui.order

import android.content.Context
import android.support.design.widget.CoordinatorLayout
import android.util.AttributeSet
import android.view.View
import com.woocommerce.android.R

class OrderDetailOrderStatusView @JvmOverloads constructor(ctx: Context,
                                                           attrs: AttributeSet? = null) : CoordinatorLayout(ctx, attrs) {
    init {
        View.inflate(context, R.layout.order_detail_order_status, this)
    }
}
