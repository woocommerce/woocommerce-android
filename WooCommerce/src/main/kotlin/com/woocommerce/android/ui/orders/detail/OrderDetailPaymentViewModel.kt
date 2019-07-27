package com.woocommerce.android.ui.orders.detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.woocommerce.android.R
import com.woocommerce.android.di.ActivityScope
import com.woocommerce.android.model.order.Order
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.utility.ResourceProvider
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import java.math.BigDecimal
import javax.inject.Inject

@ActivityScope
class OrderDetailPaymentViewModel @Inject constructor(
    private val resourceProvider: ResourceProvider,
    val currencyFormatter: CurrencyFormatter
) {
    private val _data = MutableLiveData<OrderDetailPaymentViewState>()
    val data: LiveData<OrderDetailPaymentViewState> = _data

    fun update(order: Order) {
        val formatCurrencyForDisplay = currencyFormatter.buildBigDecimalFormatter(order.currency)

        // Populate or hide payment message
        val isPaymentMessageVisible = order.paymentMethodTitle.isNotEmpty()
        val paymentMessage = if (isPaymentMessageVisible) {
            when (order.status) {
                CoreOrderStatus.PENDING,
                CoreOrderStatus.ON_HOLD -> resourceProvider.getString(
                        R.string.orderdetail_payment_summary_onhold,
                        order.paymentMethodTitle
                )
                else -> resourceProvider.getString(
                        R.string.orderdetail_payment_summary_completed,
                        formatCurrencyForDisplay(order.total),
                        order.paymentMethodTitle
                )
            }
        } else {
            ""
        }

        // Populate or hide payment message
        val title: String
        var refundTotal = ""
        var totalAfterRefunds = ""
        val isRefundSectionVisible = order.refundTotal.abs() > BigDecimal.ZERO
        if (isRefundSectionVisible) {
            title = resourceProvider.getString(R.string.orderdetail_payment_refunded)
            refundTotal = formatCurrencyForDisplay(order.refundTotal)
            totalAfterRefunds = formatCurrencyForDisplay(order.total + order.refundTotal)
        } else {
            title = resourceProvider.getString(R.string.payment)
        }

        // Populate or hide discounts section
        var discountTotal = ""
        var discountItems = ""
        val isDiscountSectionVisible = order.discountTotal > BigDecimal.ZERO
        if (isDiscountSectionVisible) {
            discountTotal = formatCurrencyForDisplay(order.discountTotal)
            discountItems = resourceProvider.getString(R.string.orderdetail_discount_items, order.discountCodes)
        }

        val viewState = OrderDetailPaymentViewState(
                title,
                formatCurrencyForDisplay(order.items.sumBy { it.subtotal.toInt() }.toBigDecimal()),
                formatCurrencyForDisplay(order.shippingTotal),
                formatCurrencyForDisplay(order.totalTax),
                formatCurrencyForDisplay(order.total),
                refundTotal,
                isPaymentMessageVisible,
                paymentMessage,
                isRefundSectionVisible,
                totalAfterRefunds,
                isDiscountSectionVisible,
                discountTotal,
                discountItems
        )

        _data.value = viewState
    }
}
