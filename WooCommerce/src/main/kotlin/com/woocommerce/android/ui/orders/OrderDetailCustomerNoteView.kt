package com.woocommerce.android.ui.orders

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import android.view.View
import com.woocommerce.android.R
import org.wordpress.android.fluxc.model.WCOrderModel

class OrderDetailCustomerNoteView @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null)
    : ConstraintLayout(ctx, attrs) {
    init {
        View.inflate(context, R.layout.order_detail_customer_note, this)
    }

    fun initView(order: WCOrderModel) {
        // TODO populate the customer note.

        // TODO set customer profile image
    }
}
