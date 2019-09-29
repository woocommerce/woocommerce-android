package com.woocommerce.android.ui.refunds

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.woocommerce.android.R
import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.di.UI_THREAD
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.SingleLiveEvent
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.fluxc.model.order.OrderIdentifier
import org.wordpress.android.fluxc.model.refunds.RefundModel
import org.wordpress.android.fluxc.store.RefundsStore
import org.wordpress.android.fluxc.store.WCOrderStore
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Named

@OpenClassOnDebug
class RefundDetailViewModel @Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val refundStore: RefundsStore,
    private val orderStore: WCOrderStore,
    private val selectedSite: SelectedSite,
    private val currencyFormatter: CurrencyFormatter,
    private val resourceProvider: ResourceProvider
) : ScopedViewModel(mainDispatcher) {
    private val _screenTitle = MutableLiveData<String>()
    val screenTitle: LiveData<String> = _screenTitle

    private val _formattedRefundAmount = MutableLiveData<String>()
    val formattedRefundAmount: LiveData<String> = _formattedRefundAmount

    private val _refundMethod = MutableLiveData<String>()
    val refundMethod: LiveData<String> = _refundMethod

    private val _refundReason = MutableLiveData<String>()
    val refundReason: LiveData<String> = _refundReason

    private lateinit var formatCurrency: (BigDecimal) -> String

    fun start(orderId: Long, refundId: Long) {
        _screenTitle.value = "${resourceProvider.getString(R.string.order_refunds_refund)} #$refundId"

        orderStore.getOrderByIdentifier(OrderIdentifier(selectedSite.get().id, orderId))
                ?.toAppModel()?.let { order ->
            formatCurrency = currencyFormatter.buildBigDecimalFormatter(order.currency)
            refundStore.getRefund(selectedSite.get(), orderId, refundId)?.let { refund ->
                displayRefundDetails(refund, order)
            }
        }
    }

    private fun displayRefundDetails(refund: RefundModel, order: Order) {
        _formattedRefundAmount.value = formatCurrency(refund.amount)
        _refundReason.value = refund.reason

        val method = resourceProvider.getString(R.string.order_refunds_refunded_via).format(order.paymentMethodTitle)
        _refundMethod.value = method
    }
}
