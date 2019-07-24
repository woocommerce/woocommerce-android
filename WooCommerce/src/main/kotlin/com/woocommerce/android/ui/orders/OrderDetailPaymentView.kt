package com.woocommerce.android.ui.orders

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import com.woocommerce.android.R
import com.woocommerce.android.di.Injector
import com.woocommerce.android.ui.orders.detail.OrderDetailPaymentViewModel
import com.woocommerce.android.ui.orders.detail.OrderDetailPaymentViewModel.StaticUiState
import com.woocommerce.android.ui.orders.detail.OrderDetailPaymentViewState
import com.woocommerce.android.util.toVisibility
import com.woocommerce.android.viewmodel.layout.MvvmLinearLayout
import kotlinx.android.synthetic.main.order_detail_payment_info.view.*
import javax.inject.Inject

class OrderDetailPaymentView @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null)
    : MvvmLinearLayout<OrderDetailPaymentViewState, OrderDetailPaymentViewModel>(ctx, attrs) {
    @Inject override lateinit var viewModel: OrderDetailPaymentViewModel

    init {
        Injector.get().inject(this)

        View.inflate(context, R.layout.order_detail_payment_info, this)
        orientation = VERTICAL
    }

    override fun onLifecycleOwnerAttached(lifecycleOwner: LifecycleOwner) {
        setupObservers(lifecycleOwner)
    }

    private fun setupObservers(lifecycleOwner: LifecycleOwner) {
        viewModel.data.observe(lifecycleOwner, Observer {
            it?.let { data ->
                updateView(data)
            }
        })
    }

    private fun updateView(paymentInfo: StaticUiState) {
        paymentInfo_subTotal.text = paymentInfo.subtotal
        paymentInfo_shippingTotal.text = paymentInfo.shippingTotal
        paymentInfo_taxesTotal.text = paymentInfo.taxesTotal
        paymentInfo_total.text = paymentInfo.total

        paymentInfo_paymentMsg.visibility = paymentInfo.isPaymentMessageVisible.toVisibility()
        paymentInfo_divider2.visibility = paymentInfo.isPaymentMessageVisible.toVisibility()
        paymentInfo_paymentMsg.text = paymentInfo.paymentMessage

        paymentInfo_refundSection.visibility = paymentInfo.isRefundSectionVisible.toVisibility()
        paymentInfo_lblTitle.text = paymentInfo.title
        paymentInfo_refundTotal.text = paymentInfo.refundTotal
        paymentInfo_newTotal.text = paymentInfo.totalAfterRefunds

        paymentInfo_discountSection.visibility = paymentInfo.isDiscountSectionVisible.toVisibility()
        paymentInfo_discountTotal.text = paymentInfo.discountTotal
        paymentInfo_discountItems.text = paymentInfo.discountItems
    }
}
