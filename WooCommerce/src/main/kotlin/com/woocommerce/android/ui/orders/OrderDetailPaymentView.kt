package com.woocommerce.android.ui.orders

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import android.view.View
import com.woocommerce.android.R
import org.wordpress.android.fluxc.model.WCOrderModel

class OrderDetailPaymentView @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null)
    : ConstraintLayout(ctx, attrs) {
    init {
        View.inflate(context, R.layout.order_detail_payment_info, this)
    }

    fun initView(order: WCOrderModel) {
        // todo populate the view
    }
}
