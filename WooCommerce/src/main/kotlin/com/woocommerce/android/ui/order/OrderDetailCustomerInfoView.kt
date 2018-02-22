package com.woocommerce.android.ui.order

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.RelativeLayout
import com.woocommerce.android.R
import org.wordpress.android.fluxc.model.WCOrderModel

class OrderDetailCustomerInfoView @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null)
    : RelativeLayout(ctx, attrs) {
    init {
        View.inflate(context, R.layout.order_detail_customer_info, this)
    }

    fun initView(order: WCOrderModel) {
        // todo
    }
}
