package com.woocommerce.android.ui.refunds

import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.R
import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.Refund
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.android.parcel.Parcelize
import org.wordpress.android.fluxc.model.order.OrderIdentifier
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCRefundStore
import java.math.BigDecimal

@OpenClassOnDebug
class RefundDetailViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateHandle,
    dispatchers: CoroutineDispatchers,
    private val orderStore: WCOrderStore,
    private val selectedSite: SelectedSite,
    private val currencyFormatter: CurrencyFormatter,
    private val resourceProvider: ResourceProvider,
    private val refundStore: WCRefundStore
) : ScopedViewModel(savedState, dispatchers) {
    companion object {
        private const val REFUND_DETAIL_SAVED_STATE_KEY = "REFUND_DETAIL_SAVED_STATE_KEY"
    }
    private val _viewState = savedState.getLiveData<ViewState>(REFUND_DETAIL_SAVED_STATE_KEY)
    val viewState: LiveData<ViewState> = _viewState

    private lateinit var formatCurrency: (BigDecimal) -> String

    fun start(orderId: Long, refundId: Long) {
        orderStore.getOrderByIdentifier(OrderIdentifier(selectedSite.get().id, orderId))
                ?.toAppModel()?.let { order ->
            formatCurrency = currencyFormatter.buildBigDecimalFormatter(order.currency)
            refundStore.getRefund(selectedSite.get(), orderId, refundId)?.let { refund ->
                displayRefundDetails(refund, order)
            }
        }
    }

    private fun displayRefundDetails(refund: Refund, order: Order) {
        val method = if (refund.automaticGatewayRefund)
            order.paymentMethodTitle
        else
            "${resourceProvider.getString(R.string.order_refunds_manual_refund)} - ${order.paymentMethodTitle}"

        val refundDetails = ViewState(
                "${resourceProvider.getString(R.string.order_refunds_refund)} #${refund.id}",
                formatCurrency(refund.amount),
                resourceProvider.getString(R.string.order_refunds_refunded_via).format(method),
                refund.reason
        )
        _viewState.value = refundDetails
    }

    @Parcelize
    data class ViewState(
        val screenTitle: String,
        val refundAmount: String,
        val refundMethod: String,
        val refundReason: String?
    ) : Parcelable

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<RefundDetailViewModel>
}
