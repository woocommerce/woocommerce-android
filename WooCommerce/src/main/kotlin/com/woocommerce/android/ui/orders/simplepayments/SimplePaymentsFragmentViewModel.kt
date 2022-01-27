package com.woocommerce.android.ui.orders.simplepayments

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.model.Order
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal
import javax.inject.Inject

@OpenClassOnDebug
@HiltViewModel
class SimplePaymentsFragmentViewModel @Inject constructor(
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
    final val viewStateLiveData = LiveDataDelegate(savedState, ViewState())
    internal final var viewState by viewStateLiveData

    private val navArgs: SimplePaymentsFragmentArgs by savedState.navArgs()

    private val order: Order
        get() = navArgs.order

    val orderDraft
        get() = order.copy(
            total = viewState.orderTotal,
            totalTax = viewState.orderTotalTax,
            customerNote = viewState.customerNote
        )

    init {
        viewState = viewState.copy(customerNote = order.customerNote)
        val hasTaxes = order.totalTax > BigDecimal.ZERO
        updateViewState(hasTaxes)
    }

    private fun updateViewState(chargeTaxes: Boolean) {
        // accessing feesLines[0] is safe to do since a fee line is passed by FluxC when creating the order. also note
        // the single fee line is the only way to get the price w/o taxes, and FluxC sets the tax status to "taxable"
        // so when the order is created core automatically sets the total tax if the store has taxes enabled.
        val feeLine = order.feesLines[0]

        val orderTaxRate = if (order.taxLines.isEmpty()) {
            EMPTY_TAX_RATE
        } else {
            order.taxLines[0].taxTotal
        }

        if (chargeTaxes) {
            viewState = viewState.copy(
                chargeTaxes = true,
                orderSubtotal = feeLine.total,
                orderTotalTax = order.totalTax,
                orderTaxRate = orderTaxRate,
                orderTotal = order.total
            )
        } else {
            viewState = viewState.copy(
                chargeTaxes = false,
                orderSubtotal = feeLine.total,
                orderTotalTax = BigDecimal.ZERO,
                orderTaxRate = "0.0",
                orderTotal = feeLine.total
            )
        }
    }

    fun onChargeTaxesChanged(chargeTaxes: Boolean) {
        updateViewState(chargeTaxes = chargeTaxes)
    }

    fun onCustomerNoteClicked() {
        triggerEvent(ShowCustomerNoteEditor)
    }

    fun onCustomerNoteChanged(customerNote: String) {
        viewState = viewState.copy(customerNote = customerNote)
    }

    fun onDoneButtonClicked() {
        // TODO nbradbury - save the order draft, waiting for FluxC changes to do that
        triggerEvent(ShowTakePaymentScreen)
    }

    @Parcelize
    data class ViewState(
        val chargeTaxes: Boolean = false,
        val orderSubtotal: BigDecimal = BigDecimal.ZERO,
        val orderTotalTax: BigDecimal = BigDecimal.ZERO,
        val orderTaxRate: String = "",
        val orderTotal: BigDecimal = BigDecimal.ZERO,
        val customerNote: String = ""
    ) : Parcelable

    object ShowCustomerNoteEditor : MultiLiveEvent.Event()
    object ShowTakePaymentScreen : MultiLiveEvent.Event()

    companion object {
        private const val EMPTY_TAX_RATE = "0.00"
    }
}
