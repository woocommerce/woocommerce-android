package com.woocommerce.android.ui.orders

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import com.woocommerce.android.R
import com.woocommerce.android.ui.orders.detail.OrderDetailPaymentViewState
import com.woocommerce.android.util.toVisibility
import kotlinx.android.synthetic.main.order_detail_payment_info.view.*

class OrderDetailPaymentView @JvmOverloads
constructor(ctx: Context, attrs: AttributeSet? = null) : LinearLayout(ctx, attrs) {
    init {
        View.inflate(context, R.layout.order_detail_payment_info, this)
        orientation = VERTICAL
    }

    fun updateView(viewState: OrderDetailPaymentViewState) {
        paymentInfo_subTotal.text = viewState.subtotal
        paymentInfo_shippingTotal.text = viewState.shippingTotal
        paymentInfo_taxesTotal.text = viewState.taxesTotal
        paymentInfo_total.text = viewState.total

        paymentInfo_paymentMsg.visibility = viewState.isPaymentMessageVisible.toVisibility()
        paymentInfo_divider2.visibility = viewState.isPaymentMessageVisible.toVisibility()
        paymentInfo_paymentMsg.text = viewState.paymentMessage

        paymentInfo_refundSection.visibility = viewState.isRefundSectionVisible.toVisibility()
        paymentInfo_lblTitle.text = viewState.title
        paymentInfo_refundTotal.text = viewState.refundTotal
        paymentInfo_newTotal.text = viewState.totalAfterRefunds

        paymentInfo_discountSection.visibility = viewState.isDiscountSectionVisible.toVisibility()
        paymentInfo_discountTotal.text = viewState.discountTotal
        paymentInfo_discountItems.text = viewState.discountItems
    }
}
