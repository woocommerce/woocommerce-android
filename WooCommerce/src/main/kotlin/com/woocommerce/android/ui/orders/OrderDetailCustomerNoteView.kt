package com.woocommerce.android.ui.orders

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.woocommerce.android.R
import kotlinx.android.synthetic.main.order_detail_customer_note.view.*
import org.wordpress.android.fluxc.model.WCOrderModel

class OrderDetailCustomerNoteView @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null)
    : ConstraintLayout(ctx, attrs) {
    init {
        View.inflate(context, R.layout.order_detail_customer_note, this)
    }

    fun initView(order: WCOrderModel) {
        customerNote_msg.text = order.customerNote
    }
}
