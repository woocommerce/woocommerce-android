package com.woocommerce.android.ui.orders.detail

import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.woocommerce.android.R
import com.woocommerce.android.di.ActivityScope
import com.woocommerce.android.model.order.Order
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.view.IMvvmCustomViewModel
import com.woocommerce.android.viewmodel.utility.ResourceProvider
import kotlinx.android.parcel.Parcelize
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

@ActivityScope
class OrderDetailPaymentViewModel @Inject constructor(
    private val resourceProvider: ResourceProvider,
    val currencyFormatter: CurrencyFormatter
) : IMvvmCustomViewModel<OrderDetailPaymentViewState> {
    private val _data = MutableLiveData<StaticUiState>()
    val data: LiveData<StaticUiState> = _data

    override var state: OrderDetailPaymentViewState = OrderDetailPaymentViewState()
        set(value) {
            field = value
            notifyObservers(value)
        }

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

        val uiState = StaticUiState(
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

        state = OrderDetailPaymentViewState(uiState)
    }

    private fun notifyObservers(state: OrderDetailPaymentViewState) {
        state.uiState?.let {
            _data.value = it
        }
    }

    @Parcelize
    data class StaticUiState(
        val title: String,
        val subtotal: String,
        val shippingTotal: String,
        val taxesTotal: String,
        val total: String,
        val refundTotal: String,
        val isPaymentMessageVisible: Boolean,
        val paymentMessage: String,
        val isRefundSectionVisible: Boolean,
        val totalAfterRefunds: String,
        val isDiscountSectionVisible: Boolean,
        val discountTotal: String,
        val discountItems: String
    ) : Parcelable
}
