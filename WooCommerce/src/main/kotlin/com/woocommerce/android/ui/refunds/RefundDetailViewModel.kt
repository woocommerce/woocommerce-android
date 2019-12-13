package com.woocommerce.android.ui.refunds

import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.R
import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.Refund
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.refunds.RefundProductListAdapter.RefundListItem
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.android.parcel.Parcelize
import org.wordpress.android.fluxc.model.order.OrderIdentifier
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCRefundStore
import java.math.BigDecimal
import com.woocommerce.android.extensions.calculateTotals

@OpenClassOnDebug
class RefundDetailViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    dispatchers: CoroutineDispatchers,
    orderStore: WCOrderStore,
    private val selectedSite: SelectedSite,
    private val currencyFormatter: CurrencyFormatter,
    private val resourceProvider: ResourceProvider,
    private val refundStore: WCRefundStore
) : ScopedViewModel(savedState, dispatchers) {
    final val viewStateData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateData

    private val _refundItems = MutableLiveData<List<RefundListItem>>()
    final val refundItems: LiveData<List<RefundListItem>> = _refundItems

    private lateinit var formatCurrency: (BigDecimal) -> String

    private val navArgs: RefundDetailFragmentArgs by savedState.navArgs()

    init {
        orderStore.getOrderByIdentifier(OrderIdentifier(selectedSite.get().id, navArgs.orderId))
                ?.toAppModel()?.let { order ->
            formatCurrency = currencyFormatter.buildBigDecimalFormatter(order.currency)
            refundStore.getRefund(selectedSite.get(), navArgs.orderId, navArgs.refundId)?.toAppModel()?.let { refund ->
                displayRefundDetails(refund, order)
            }
        }
    }

    private fun displayRefundDetails(refund: Refund, order: Order) {
        val method = if (refund.automaticGatewayRefund)
            order.paymentMethodTitle
        else
            "${resourceProvider.getString(R.string.order_refunds_manual_refund)} - ${order.paymentMethodTitle}"

        if (refund.items.isNotEmpty()) {
            val items = refund.items.map { refundItem ->
                RefundListItem(
                    order.items.first { it.uniqueId == refundItem.uniqueId },
                    quantity = refundItem.quantity
                )
            }

            val (subtotal, taxes) = items.calculateTotals()
            viewState = viewState.copy(
                    currency = order.currency,
                    areItemsVisible = true,
                    subtotal = formatCurrency(subtotal),
                    taxes = formatCurrency(taxes)
            )

            _refundItems.value = items
        } else {
            viewState = viewState.copy(areItemsVisible = false)
        }

        viewState = viewState.copy(
                screenTitle = "${resourceProvider.getString(R.string.order_refunds_refund)} #${refund.id}",
                refundAmount = formatCurrency(refund.amount),
                refundMethod = resourceProvider.getString(R.string.order_refunds_refunded_via).format(method),
                refundReason = refund.reason
        )
    }

    @Parcelize
    data class ViewState(
        val screenTitle: String? = null,
        val refundAmount: String? = null,
        val subtotal: String? = null,
        val taxes: String? = null,
        val refundMethod: String? = null,
        val refundReason: String? = null,
        val currency: String? = null,
        val areItemsVisible: Boolean? = null
    ) : Parcelable

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<RefundDetailViewModel>
}
