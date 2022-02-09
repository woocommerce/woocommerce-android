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

    // it was decided that both Android and iOS would determine the tax rate by looking at the
    // first tax line item because there was nowhere else to get the tax rate, and we didn't
    // want to calculate the tax percentage on the client. although it's possible that there will
    // be multiple tax lines, the team assumed the first rate would be the one to show (this may
    // be revisited)
    val taxRatePercent
        get() = if (order.taxLines.isNotEmpty() && viewState.chargeTaxes) {
            order.taxLines[0].ratePercent.toString()
        } else {
            EMPTY_TAX_RATE
        }

    // accessing feesLines[0] should be safe to do since a fee line is passed by FluxC when creating the order, but we
    // check for an empty list here to simplify our test. note the single fee line is the only way to get the price w/o
    // taxes, and FluxC sets the tax status to "taxable" so when the order is created core automatically sets the total
    // tax if the store has taxes enabled.
    val feeLineTotal
        get() = if (order.feesLines.isNotEmpty()) {
            order.feesLines[0].total
        } else {
            BigDecimal.ZERO
        }

    init {
        viewState = viewState.copy(customerNote = order.customerNote)
        val hasTaxes = order.totalTax > BigDecimal.ZERO
        updateViewState(hasTaxes)
    }

    private fun updateViewState(chargeTaxes: Boolean) {
        if (chargeTaxes) {
            viewState = viewState.copy(
                chargeTaxes = true,
                orderSubtotal = feeLineTotal,
                orderTotalTax = order.totalTax,
                orderTotal = order.total
            )
        } else {
            viewState = viewState.copy(
                chargeTaxes = false,
                orderSubtotal = feeLineTotal,
                orderTotalTax = BigDecimal.ZERO,
                orderTotal = feeLineTotal
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
        val orderTotal: BigDecimal = BigDecimal.ZERO,
        val customerNote: String = ""
    ) : Parcelable

    object ShowCustomerNoteEditor : MultiLiveEvent.Event()
    object ShowTakePaymentScreen : MultiLiveEvent.Event()

    companion object {
        const val EMPTY_TAX_RATE = "0.00"
    }
}
